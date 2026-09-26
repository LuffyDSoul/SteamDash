package com.dacs.bff.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.dacs.bff.dto.BibliotecaComparacionDto;
import com.dacs.bff.dto.SteamUserGamesInput;

import lombok.extern.slf4j.Slf4j;

/**
 * Cliente para comunicarse con el Backend
 */
@Slf4j
@Component
public class BackendClient {

    private final RestTemplate restTemplate;
    private final String backendUrl;

    public BackendClient(RestTemplate restTemplate,
                        @Value("${backend.url:http://localhost:9000}") String backendUrl) {
        this.restTemplate = restTemplate;
        this.backendUrl = backendUrl;
    }

    /**
     * Llama al backend para comparar bibliotecas de múltiples usuarios (2-6)
     */
    public BibliotecaComparacionDto compararBibliotecasMultiples(List<SteamUserGamesInput> usuarios) {
        String url = backendUrl + "/backend/biblioteca-comparacion/comparar-multiples";
        
        log.info("BFF - Llamando al backend para comparar {} bibliotecas: {}", usuarios.size(), url);
        
        try {
            ResponseEntity<BibliotecaComparacionDto> response = restTemplate.postForEntity(
                url,
                usuarios,
                BibliotecaComparacionDto.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Backend retornó código de estado: {}", response.getStatusCode());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Error al comparar bibliotecas en el backend");
            }
        } catch (Exception e) {
            log.error("Error al llamar al backend: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error al comunicarse con el backend: " + e.getMessage());
        }
    }
    
    /**
     * Cuenta cuántos juegos con logros tiene el usuario en BD
     */
    public long countUserGamesWithAchievements(String steamId) {
        String url = backendUrl + "/backend/achievements/count/" + steamId;
        
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object count = response.getBody().get("gamesWithAchievements");
                if (count instanceof Number) {
                    return ((Number) count).longValue();
                }
            }
        } catch (Exception e) {
            log.error("Error al contar juegos con logros: {}", e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Verifica qué juegos NO tienen logros en BD
     */
    public List<Long> checkMissingAchievements(String steamId, List<Long> appIds) {
        String url = backendUrl + "/backend/achievements/check";
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("steamId", steamId);
            request.put("appIds", appIds);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Integer> missingInts = (List<Integer>) response.getBody().get("missingAppIds");
                if (missingInts != null) {
                    return missingInts.stream().map(Integer::longValue).toList();
                }
            }
        } catch (Exception e) {
            log.error("Error al verificar logros faltantes: {}", e.getMessage());
        }
        
        // Si hay error, retornar todos los appIds para intentar ingestarlos
        return appIds;
    }
    
    /**
     * Inicia la ingesta de logros para múltiples juegos de un usuario
     * Heurística: si tiene ≥10 juegos en BD, asume que ya tiene todos y hace verificación async
     */
    public void ingestarLogros(String steamId, List<Long> appIds) {
        // Heurística: contar juegos existentes
        long existingGames = countUserGamesWithAchievements(steamId);
        
        if (existingGames >= 10) {
            // Usuario con datos, hacer verificación en segundo plano
            log.info("BFF - Usuario {} tiene {} juegos en BD, verificando diferencias en segundo plano", 
                     steamId, existingGames);
            
            new Thread(() -> {
                try {
                    List<Long> missingAppIds = checkMissingAchievements(steamId, appIds);
                    
                    if (missingAppIds.isEmpty()) {
                        log.info("BFF - [ASYNC] Todos los logros ya existen para usuario {}", steamId);
                        return;
                    }
                    
                    log.info("BFF - [ASYNC] Encontrados {}/{} juegos nuevos para usuario {}, ingestionando...", 
                             missingAppIds.size(), appIds.size(), steamId);
                    
                    ingestarLogrosInternal(steamId, missingAppIds);
                } catch (Exception e) {
                    log.error("Error en verificación async de logros: {}", e.getMessage());
                }
            }).start();
            
        } else {
            // Usuario nuevo o con pocos datos, hacer verificación bloqueante
            log.info("BFF - Usuario {} tiene solo {} juegos, verificando síncronamente", steamId, existingGames);
            
            List<Long> missingAppIds = checkMissingAchievements(steamId, appIds);
            
            if (missingAppIds.isEmpty()) {
                log.info("BFF - Todos los logros ya existen en BD para usuario {}", steamId);
                return;
            }
            
            log.info("BFF - Ingestionando {}/{} juegos faltantes para usuario {}", 
                     missingAppIds.size(), appIds.size(), steamId);
            
            ingestarLogrosInternal(steamId, missingAppIds);
        }
    }
    
    /**
     * Método interno para realizar la ingesta
     */
    private void ingestarLogrosInternal(String steamId, List<Long> appIds) {
        String url = backendUrl + "/backend/achievements/ingest";
        
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("steamId", steamId);
            request.put("appIds", appIds);
            
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Ingesta de logros iniciada exitosamente para {} juegos", appIds.size());
            } else {
                log.warn("Backend retornó código {} al iniciar ingesta de logros", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error al iniciar ingesta de logros: {}", e.getMessage());
        }
    }
    
    /**
     * Fuerza la actualización de logros para TODOS los juegos sin verificar si ya existen
     * Usado por refreshAllGames para asegurar que los logros se actualicen completamente
     * Llama al endpoint individual para cada juego para forzar refresh real
     */
    public void forceRefreshLogros(String steamId, List<Long> appIds) {
        log.info("BFF - 🔄 Forzando actualización INDIVIDUAL de logros para {} juegos del usuario {}", appIds.size(), steamId);
        
        // Ejecutar en un thread separado para no bloquear la respuesta
        new Thread(() -> {
            int success = 0;
            int errors = 0;
            
            for (Long appId : appIds) {
                try {
                    boolean result = ingestarLogrosJuegoIndividual(steamId, appId);
                    if (result) {
                        success++;
                    } else {
                        errors++;
                    }
                    
                    // Pequeña pausa para no saturar el backend
                    Thread.sleep(500);
                    
                } catch (Exception e) {
                    log.error("Error al refrescar logros para juego {}: {}", appId, e.getMessage());
                    errors++;
                }
            }
            
            log.info("BFF - ✅ Actualización de logros completada: {}/{} juegos actualizados ({} errores)", 
                     success, appIds.size(), errors);
        }).start();
    }
    
    /**
     * Ingesta de logros para un solo juego de forma síncrona
     * Llamada al endpoint /backend/achievements/ingest/user/{steamId}/game/{appId}
     */
    public boolean ingestarLogrosJuegoIndividual(String steamId, Long appId) {
        String url = backendUrl + "/backend/achievements/ingest/user/" + steamId + "/game/" + appId;
        
        log.info("BFF - Ingestionando logros síncronamente para usuario {} appId {}", steamId, appId);
        
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("BFF - Ingesta de logros completada para appId {}", appId);
                return true;
            } else {
                log.warn("Backend retornó código {} al ingestar logros", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Error al ingestar logros para juego individual: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtiene logros desde la base de datos del Backend
     */
    public Map<String, Object> getAchievementsFromDatabase(String steamId, Long appId) {
        String url = backendUrl + "/backend/achievements/user/" + steamId + "/game/" + appId;
        
        log.debug("BFF - Obteniendo logros desde BD para usuario {} appId {}", steamId, appId);
        
        try {
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Backend retornó código {} al obtener logros desde BD", response.getStatusCode());
                return Map.of("success", false, "error", "Error al obtener logros desde BD");
            }
        } catch (Exception e) {
            log.error("Error al obtener logros desde BD: {}", e.getMessage());
            return Map.of("success", false, "error", "Error al comunicarse con el backend");
        }
    }
    
    /**
     * Obtiene estadísticas de logros para un usuario desde el Backend
     * @param steamId Steam ID del usuario
     * @param topGames Lista opcional de appIds ordenados por tiempo jugado
     * @return Estadísticas de logros
     */
    public com.dacs.bff.dto.AchievementStatsDto getAchievementStats(String steamId, List<Long> topGames) {
        String url = backendUrl + "/backend/achievement-stats/" + steamId;
        
        log.info("BFF - Obteniendo estadísticas de logros desde backend para usuario {}", steamId);
        
        try {
            ResponseEntity<com.dacs.bff.dto.AchievementStatsDto> response = restTemplate.postForEntity(
                url,
                topGames,
                com.dacs.bff.dto.AchievementStatsDto.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                log.warn("Backend retornó código {} al obtener estadísticas de logros", response.getStatusCode());
                return com.dacs.bff.dto.AchievementStatsDto.builder()
                        .steamId(steamId)
                        .totalJuegosConLogros(0)
                        .totalLogrosDesbloqueados(0)
                        .totalLogrosDisponibles(0)
                        .porcentajeGlobal(0.0)
                        .build();
            }
        } catch (Exception e) {
            log.error("Error al obtener estadísticas de logros desde backend: {}", e.getMessage());
            return com.dacs.bff.dto.AchievementStatsDto.builder()
                    .steamId(steamId)
                    .totalJuegosConLogros(0)
                    .totalLogrosDesbloqueados(0)
                    .totalLogrosDisponibles(0)
                    .porcentajeGlobal(0.0)
                    .build();
        }
    }
}
