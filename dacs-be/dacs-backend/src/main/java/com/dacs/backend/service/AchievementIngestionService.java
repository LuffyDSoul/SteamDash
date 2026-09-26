package com.dacs.backend.service;

import com.dacs.backend.entity.GameAchievement;
import com.dacs.backend.entity.UserGameAchievement;
import com.dacs.backend.repository.GameAchievementRepository;
import com.dacs.backend.repository.UserGameAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import jakarta.persistence.EntityManager;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Servicio para ingesta de logros desde Steam API con rate limiting
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementIngestionService {
    
    private final GameAchievementRepository gameAchievementRepository;
    private final UserGameAchievementRepository userGameAchievementRepository;
    private final RestTemplate restTemplate;
    private final EntityManager entityManager;
    
    @Value("${conector.url:http://localhost:9002}")
    private String conectorUrl;
    
    private static final long RATE_LIMIT_MS = 500; // 500ms = 2 llamadas por segundo
    private long lastApiCallTime = 0;
    
    /**
     * Ingesta de logros para múltiples juegos de un usuario
     * Limita a 2 llamadas por segundo y maneja errores sin detener el proceso
     */
    public void ingestAchievementsForUserGames(String steamId, List<Long> appIds) {
        log.info("Iniciando ingesta de logros para usuario {} con {} juegos", steamId, appIds.size());
        
        int processed = 0;
        int skipped = 0;
        int errors = 0;
        
        for (Long appId : appIds) {
            try {
                // Verificar rate limiting
                waitForRateLimit();
                
                // Verificar si ya tenemos los datos en BD
                boolean hasGameSchema = gameAchievementRepository.existsByAppId(appId);
                boolean hasUserProgress = userGameAchievementRepository.existsBySteamIdAndAppId(steamId, appId);
                
                if (hasGameSchema && hasUserProgress) {
                    log.debug("Logros ya existen en BD para appId {} y usuario {}, skipping", appId, steamId);
                    skipped++;
                    continue;
                }
                
                // Ingestar schema del juego si no existe
                if (!hasGameSchema) {
                    boolean schemaIngested = ingestGameAchievementSchema(appId);
                    if (!schemaIngested) {
                        log.warn("No se pudo ingestar schema para appId {}, skipping user progress", appId);
                        errors++;
                        continue;
                    }
                }
                
                // Ingestar progreso del usuario si no existe
                if (!hasUserProgress) {
                    waitForRateLimit(); // Segunda llamada, esperar de nuevo
                    boolean progressIngested = ingestUserAchievementProgress(steamId, appId);
                    if (!progressIngested) {
                        log.warn("No se pudo ingestar progreso de usuario para appId {}", appId);
                        errors++;
                        continue;
                    }
                }
                
                processed++;
                log.debug("Ingesta completada para appId {}: {}/{}", appId, processed, appIds.size());
                
            } catch (Exception e) {
                log.error("Error inesperado al ingestar logros para appId {}: {}", appId, e.getMessage());
                errors++;
                // Continuar con el siguiente juego
            }
        }
        
        log.info("Ingesta completada: {} procesados, {} omitidos (ya existentes), {} errores", 
                 processed, skipped, errors);
    }
    
    /**
     * Ingesta el schema de logros de un juego (lista de todos los logros disponibles)
     */
    @Transactional
    public boolean ingestGameAchievementSchema(Long appId) {
        try {
            String url = conectorUrl + "/conector/steam/game/" + appId + "/schema";
            log.debug("Obteniendo schema de logros para appId {} desde {}", appId, url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || response.isEmpty()) {
                log.debug("Schema vacío para appId {}, probablemente no tiene logros", appId);
                return false;
            }
            
            Map<String, Object> game = (Map<String, Object>) response.get("game");
            if (game == null) {
                log.debug("No hay datos de juego en schema para appId {}", appId);
                return false;
            }
            
            Object availableGameStatsObj = game.get("availableGameStats");
            if (availableGameStatsObj == null) {
                log.debug("No hay availableGameStats para appId {}", appId);
                return false;
            }
            
            List<Map<String, Object>> achievements = extractAchievementsFromStats(availableGameStatsObj);
            
            if (achievements.isEmpty()) {
                log.debug("No se encontraron logros en schema para appId {}", appId);
                return false;
            }
            
            // Guardar logros en BD
            List<GameAchievement> entities = new ArrayList<>();
            for (Map<String, Object> ach : achievements) {
                GameAchievement entity = GameAchievement.builder()
                    .appId(appId)
                    .achievementName((String) ach.get("name"))
                    .displayName((String) ach.get("displayName"))
                    .description((String) ach.get("description"))
                    .iconUrl((String) ach.get("icon"))
                    .iconGrayUrl((String) ach.get("icongray"))
                    .hidden((Integer) ach.get("hidden"))
                    .build();
                entities.add(entity);
            }
            
            gameAchievementRepository.saveAll(entities);
            log.info("Guardados {} logros para appId {}", entities.size(), appId);
            return true;
            
        } catch (Exception e) {
            log.error("Error al ingestar schema para appId {}: {}", appId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Ingesta el progreso de logros de un usuario en un juego específico
     */
    @Transactional
    public boolean ingestUserAchievementProgress(String steamId, Long appId) {
        try {
            log.info("=== INICIANDO REFRESH DE LOGROS ===");
            log.info("Usuario: {}, AppId: {}", steamId, appId);
            log.info("🗑️ IMPORTANTE: Se borrarán todos los logros existentes y se recargarán desde Steam API");
            
            // Verificar y cargar el schema del juego si no existe
            boolean hasGameSchema = gameAchievementRepository.existsByAppId(appId);
            if (!hasGameSchema) {
                log.info("Schema del juego {} no existe, ingiriendo primero...", appId);
                boolean schemaIngested = ingestGameAchievementSchema(appId);
                if (!schemaIngested) {
                    log.warn("No se pudo ingestar schema para appId {}, continuando de todos modos...", appId);
                    // No retornamos false aquí porque algunos juegos pueden tener logros de usuario sin schema
                }
            }
            
            // PASO 1: PRIMERO verificar y guardar estado anterior (antes de borrar)
            log.info("🗑️ PASO 1: Verificando estado anterior en BD");
            List<UserGameAchievement> previousAchievements = userGameAchievementRepository.findBySteamIdAndAppId(steamId, appId);
            long previousUnlockedCount = previousAchievements.stream()
                .filter(ach -> Boolean.TRUE.equals(ach.getAchieved()))
                .count();
            long existingCount = previousAchievements.size();
            
            if (!previousAchievements.isEmpty()) {
                log.info("📊 Estado anterior en BD: {}/{} logros desbloqueados", previousUnlockedCount, existingCount);
            } else {
                log.info("📊 No hay estado anterior en BD para este usuario/juego");
            }
            
            // PASO 2: BORRAR INMEDIATAMENTE todos los registros existentes
            if (existingCount > 0) {
                log.info("🗑️ PASO 2: Eliminando {} registros existentes de la BD", existingCount);
                userGameAchievementRepository.deleteBySteamIdAndAppId(steamId, appId);
                userGameAchievementRepository.flush(); // Forzar ejecución del DELETE
                entityManager.clear(); // Limpiar cache de Hibernate
                log.info("✅ {} registros eliminados de la BD", existingCount);
            } else {
                log.info("ℹ️ No hay registros existentes, se crearán nuevos");
            }
            
            // PASO 3: Obtener datos actualizados desde Steam API
            String url = conectorUrl + "/conector/steam/user/" + steamId + "/game/" + appId + "/achievements";
            log.info("🌐 PASO 3: Obteniendo datos desde Steam API: {}", url);
            
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response == null || response.isEmpty()) {
                log.warn("Respuesta vacía para progreso de usuario {} appId {}", steamId, appId);
                return false;
            }
            
            Map<String, Object> playerstats = (Map<String, Object>) response.get("playerstats");
            if (playerstats == null) {
                log.warn("No hay playerstats para usuario {} appId {}", steamId, appId);
                return false;
            }
            
            List<Map<String, Object>> playerAchievements = (List<Map<String, Object>>) playerstats.get("achievements");
            
            // Si no hay achievements, puede ser que el jugador no haya desbloqueado ninguno aún
            // En ese caso, creamos registros para todos los logros del juego marcados como no desbloqueados
            if (playerAchievements == null || playerAchievements.isEmpty()) {
                log.info("No hay logros en la respuesta de Steam API, creando registros bloqueados");
                return createLockedAchievementsForUser(steamId, appId);
            }
            
            log.info("Steam API devolvió {} logros", playerAchievements.size());
            
            // Contar cuántos están desbloqueados en los nuevos datos
            long unlockedCount = playerAchievements.stream()
                .filter(ach -> {
                    Integer achieved = (Integer) ach.get("achieved");
                    return achieved != null && achieved == 1;
                })
                .count();
            
            log.info("Logros desbloqueados según Steam API: {}/{}", unlockedCount, playerAchievements.size());
            
            // Comparar con estado anterior
            if (existingCount > 0) {
                if (unlockedCount > previousUnlockedCount) {
                    log.info("🎉 El usuario desbloqueó {} logro(s) nuevo(s)!", unlockedCount - previousUnlockedCount);
                } else if (unlockedCount < previousUnlockedCount) {
                    log.warn("⚠️ El usuario perdió {} logro(s) (reset/revocación)", previousUnlockedCount - unlockedCount);
                } else {
                    log.info("ℹ️ Sin cambios en el número de logros desbloqueados");
                }
            }
            
            // PASO 4: Guardar nuevos registros en BD
            List<UserGameAchievement> entities = new ArrayList<>();
            for (Map<String, Object> ach : playerAchievements) {
                String apiname = (String) ach.get("apiname");
                Integer achieved = (Integer) ach.get("achieved");
                Object unlockTimeObj = ach.get("unlocktime");
                Long unlockTime = null;
                if (unlockTimeObj != null) {
                    unlockTime = unlockTimeObj instanceof Integer ? 
                        ((Integer) unlockTimeObj).longValue() : (Long) unlockTimeObj;
                }
                
                boolean isAchieved = achieved != null && achieved == 1;
                
                UserGameAchievement entity = UserGameAchievement.builder()
                    .steamId(steamId)
                    .appId(appId)
                    .achievementName(apiname)
                    .achieved(isAchieved)
                    .unlockTime(unlockTime)
                    .build();
                entities.add(entity);
                
                // Log para los primeros 5 logros o los que están desbloqueados
                if (entities.size() <= 5 || isAchieved) {
                    log.debug("Logro: {} - Achieved: {} - UnlockTime: {}", apiname, isAchieved, unlockTime);
                }
            }
            
            log.info("💾 PASO 4: Guardando {} nuevos registros en BD", entities.size());
            List<UserGameAchievement> savedEntities = userGameAchievementRepository.saveAll(entities);
            userGameAchievementRepository.flush(); // Forzar el COMMIT
            log.info("✅ Guardados {} registros en BD", savedEntities.size());
            
            // VERIFICACIÓN FINAL: Leer desde BD para confirmar
            List<UserGameAchievement> verifyAchievements = userGameAchievementRepository.findBySteamIdAndAppId(steamId, appId);
            long verifyUnlockedCount = verifyAchievements.stream()
                .filter(ach -> Boolean.TRUE.equals(ach.getAchieved()))
                .count();
            log.info("🔍 VERIFICACIÓN FINAL: BD contiene {}/{} logros desbloqueados", verifyUnlockedCount, verifyAchievements.size());
            
            if (verifyUnlockedCount != unlockedCount) {
                log.error("❌ INCONSISTENCIA: Steam API reporta {} desbloqueados pero BD tiene {}", unlockedCount, verifyUnlockedCount);
            } else {
                log.info("✅ CONSISTENCIA CONFIRMADA: BD coincide con Steam API");
            }
            
            log.info("=== REFRESH DE LOGROS COMPLETADO ===");
            return true;
            
        } catch (Exception e) {
            log.error("Error al ingestar progreso para usuario {} appId {}: {}", steamId, appId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Crea registros de logros bloqueados para un usuario basándose en el schema del juego
     */
    @Transactional
    private boolean createLockedAchievementsForUser(String steamId, Long appId) {
        try {
            // PRIMERO: Obtener schema del juego
            List<GameAchievement> gameAchievements = gameAchievementRepository.findByAppId(appId);
            
            if (gameAchievements.isEmpty()) {
                log.debug("No hay logros en el schema para crear registros bloqueados para appId {}", appId);
                return false;
            }
            
            // SEGUNDO: Verificar si hay registros existentes
            long existingCount = userGameAchievementRepository.countBySteamIdAndAppId(steamId, appId);
            
            // TERCERO: Eliminar registros antiguos INMEDIATAMENTE si existen
            if (existingCount > 0) {
                log.info("🗑️ Eliminando {} registros antiguos de logros para usuario {} appId {}", existingCount, steamId, appId);
                userGameAchievementRepository.deleteBySteamIdAndAppId(steamId, appId);
                userGameAchievementRepository.flush(); // Forzar ejecución del DELETE
                entityManager.clear(); // Limpiar cache de Hibernate
                log.info("✅ {} registros antiguos eliminados de la BD", existingCount);
            } else {
                log.info("ℹ️ No hay registros existentes para eliminar");
            }
            
            // CUARTO: Crear y guardar nuevos registros bloqueados
            List<UserGameAchievement> lockedAchievements = new ArrayList<>();
            for (GameAchievement ga : gameAchievements) {
                UserGameAchievement entity = UserGameAchievement.builder()
                    .steamId(steamId)
                    .appId(appId)
                    .achievementName(ga.getAchievementName())
                    .achieved(false)
                    .unlockTime(null)
                    .build();
                lockedAchievements.add(entity);
            }
            
            List<UserGameAchievement> savedEntities = userGameAchievementRepository.saveAll(lockedAchievements);
            userGameAchievementRepository.flush(); // Forzar el COMMIT
            log.info("Creados {} logros bloqueados para usuario {} appId {} (registros antiguos eliminados)", 
                     savedEntities.size(), steamId, appId);
            return true;
            
        } catch (Exception e) {
            log.error("Error al crear logros bloqueados para usuario {} appId {}: {}", steamId, appId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Extrae la lista de achievements del objeto availableGameStats
     */
    private List<Map<String, Object>> extractAchievementsFromStats(Object availableGameStatsObj) {
        try {
            if (availableGameStatsObj instanceof Map) {
                Map<String, Object> statsMap = (Map<String, Object>) availableGameStatsObj;
                Object achievementsObj = statsMap.get("achievements");
                
                if (achievementsObj instanceof List) {
                    return (List<Map<String, Object>>) achievementsObj;
                }
            } else if (availableGameStatsObj instanceof List) {
                List<Object> statsList = (List<Object>) availableGameStatsObj;
                for (Object item : statsList) {
                    if (item instanceof Map) {
                        Map<String, Object> itemMap = (Map<String, Object>) item;
                        if (itemMap.containsKey("achievements")) {
                            Object achievementsObj = itemMap.get("achievements");
                            if (achievementsObj instanceof List) {
                                return (List<Map<String, Object>>) achievementsObj;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error al extraer achievements: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
    
    /**
     * Espera el tiempo necesario para respetar el rate limit de 2 llamadas/segundo
     */
    private void waitForRateLimit() {
        synchronized (this) {
            long now = System.currentTimeMillis();
            long timeSinceLastCall = now - lastApiCallTime;
            
            if (timeSinceLastCall < RATE_LIMIT_MS) {
                long waitTime = RATE_LIMIT_MS - timeSinceLastCall;
                try {
                    log.trace("Rate limiting: esperando {}ms", waitTime);
                    TimeUnit.MILLISECONDS.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupción durante rate limiting");
                }
            }
            
            lastApiCallTime = System.currentTimeMillis();
        }
    }
}
