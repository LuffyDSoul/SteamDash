package com.dacs.backend.service;

import com.dacs.backend.entity.GameAchievement;
import com.dacs.backend.entity.UserGameAchievement;
import com.dacs.backend.repository.GameAchievementRepository;
import com.dacs.backend.repository.UserGameAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * Servicio para consultar logros desde la base de datos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementQueryService {
    
    private final GameAchievementRepository gameAchievementRepository;
    private final UserGameAchievementRepository userGameAchievementRepository;
    
    /**
     * Cuenta cuántos juegos con logros tiene un usuario en BD
     */
    public long countUserGamesWithAchievements(String steamId) {
        return userGameAchievementRepository.countDistinctAppIdBySteamId(steamId);
    }
    
    /**
     * Encuentra qué juegos NO tienen logros en BD para un usuario
     * Retorna lista de appIds que necesitan ser ingestionados
     */
    public List<Long> findMissingAchievements(String steamId, List<Long> appIds) {
        return appIds.stream()
            .filter(appId -> {
                boolean hasGameSchema = gameAchievementRepository.existsByAppId(appId);
                boolean hasUserProgress = userGameAchievementRepository.existsBySteamIdAndAppId(steamId, appId);
                return !hasGameSchema || !hasUserProgress;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Obtiene los logros de un juego con el progreso de un usuario
     * Retorna un mapa con:
     * - success: boolean
     * - gameName: String (puede ser null si no se tiene)
     * - totalAchievements: int
     * - unlockedAchievements: int
     * - achievements: List<Map> con datos combinados de schema + progreso
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGameAchievementsWithUserProgress(String steamId, Long appId) {
        log.debug("Consultando logros desde BD para usuario {} appId {}", steamId, appId);
        
        // Verificar si existen logros del juego
        List<GameAchievement> gameAchievements = gameAchievementRepository.findByAppId(appId);
        
        if (gameAchievements.isEmpty()) {
            log.debug("No hay logros en BD para appId {}", appId);
            return Map.of(
                "success", false,
                "error", "No existen logros para este juego en la base de datos"
            );
        }
        
        // Obtener progreso del usuario
        List<UserGameAchievement> userProgress = userGameAchievementRepository.findBySteamIdAndAppId(steamId, appId);
        
        log.info("📊 BD Query - GameAchievements en schema: {}", gameAchievements.size());
        log.info("📊 BD Query - UserGameAchievements guardados: {}", userProgress.size());
        
        Map<String, UserGameAchievement> progressByName = userProgress.stream()
            .collect(Collectors.toMap(UserGameAchievement::getAchievementName, ua -> ua, (a, b) -> a));
        
        // Combinar datos
        List<Map<String, Object>> combinedAchievements = new ArrayList<>();
        int unlockedCount = 0;
        
        for (GameAchievement ga : gameAchievements) {
            UserGameAchievement userAch = progressByName.get(ga.getAchievementName());
            
            boolean achieved = userAch != null && userAch.getAchieved();
            if (achieved) {
                unlockedCount++;
            }
            
            Map<String, Object> achData = new HashMap<>();
            achData.put("apiname", ga.getAchievementName());
            achData.put("name", ga.getAchievementName());
            achData.put("displayName", ga.getDisplayName());
            achData.put("description", ga.getDescription());
            achData.put("icon", ga.getIconUrl());
            achData.put("icongray", ga.getIconGrayUrl());
            achData.put("hidden", ga.getHidden() != null ? ga.getHidden() : 0);
            achData.put("achieved", achieved);
            achData.put("unlocktime", userAch != null ? userAch.getUnlockTime() : null);
            
            combinedAchievements.add(achData);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("steamId", steamId);
        result.put("appId", appId);
        result.put("gameName", null); // No tenemos gameName en GameAchievement
        result.put("totalAchievements", gameAchievements.size());
        result.put("unlockedAchievements", unlockedCount);
        result.put("achievements", combinedAchievements);
        
        log.info("✅ BD Response - Total: {}, Desbloqueados: {}, Achievements list size: {}", 
                  gameAchievements.size(), unlockedCount, combinedAchievements.size());
        log.debug("Retornando {} logros desde BD para usuario {} appId {} ({} desbloqueados)", 
                  gameAchievements.size(), steamId, appId, unlockedCount);
        
        return result;
    }
    
    /**
     * Verifica si existen logros almacenados para un juego y usuario
     */
    public boolean hasAchievementsInDatabase(String steamId, Long appId) {
        boolean hasGame = gameAchievementRepository.existsByAppId(appId);
        boolean hasUser = userGameAchievementRepository.existsBySteamIdAndAppId(steamId, appId);
        return hasGame && hasUser;
    }
    
    /**
     * Obtiene estadísticas de logros para todos los juegos de un usuario en una sola consulta
     * Retorna un mapa con appId como clave y un mapa con las estadísticas como valor
     */
    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> getUserAchievementStatsBulk(String steamId) {
        log.info("📊 [INICIO] Consultando estadísticas de logros para usuario {} (bulk)", steamId);
        long startTime = System.currentTimeMillis();
        
        List<Object[]> results = userGameAchievementRepository.getAchievementStatsByUser(steamId);
        long queryTime = System.currentTimeMillis() - startTime;
        log.info("⏱️ Query SQL completada en {}ms, obtenidos {} registros", queryTime, results.size());
        
        Map<Long, Map<String, Object>> statsMap = new HashMap<>();
        
        for (Object[] row : results) {
            Long appId = (Long) row[0];
            Long totalAchievements = (Long) row[1];
            Long unlockedAchievements = row[2] != null ? (Long) row[2] : 0L; // Protección contra NULL
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAchievements", totalAchievements.intValue());
            stats.put("unlockedAchievements", unlockedAchievements.intValue());
            
            statsMap.put(appId, stats);
        }
        
        long totalTime = System.currentTimeMillis() - startTime;
        log.info("✅ Estadísticas procesadas para {} juegos del usuario {} en {}ms", statsMap.size(), steamId, totalTime);
        return statsMap;
    }
}

