package com.dacs.backend.service;

import com.dacs.backend.dto.AchievementStatsDto;
import com.dacs.backend.entity.UserGameAchievement;
import com.dacs.backend.repository.UserGameAchievementRepository;
import com.dacs.backend.repository.GameAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para calcular estadísticas de logros de un usuario
 * Optimizado para minimizar llamadas a la API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementStatsService {
    
    private final UserGameAchievementRepository userGameAchievementRepository;
    private final GameAchievementRepository gameAchievementRepository;
    
    /**
     * Calcula estadísticas completas de logros para un usuario
     * @param steamId Steam ID del usuario
     * @param topGamesWithPlaytime Lista de appIds ordenados por tiempo jugado (para optimización)
     * @return Estadísticas de logros
     */
    @Transactional(readOnly = true)
    public AchievementStatsDto calculateAchievementStats(String steamId, List<Long> topGamesWithPlaytime) {
        log.info("Calculando estadísticas de logros para usuario {}", steamId);
        
        // Obtener todos los logros del usuario desde BD
        List<UserGameAchievement> allUserAchievements = userGameAchievementRepository.findBySteamId(steamId);
        
        if (allUserAchievements.isEmpty()) {
            log.warn("No se encontraron logros en BD para usuario {}", steamId);
            return AchievementStatsDto.builder()
                    .steamId(steamId)
                    .totalJuegosConLogros(0)
                    .totalLogrosDesbloqueados(0)
                    .totalLogrosDisponibles(0)
                    .porcentajeGlobal(0.0)
                    .juegosCompletos100(Collections.emptyList())
                    .juegosCercanos100(Collections.emptyList())
                    .juegosMasProgreso(Collections.emptyList())
                    .build();
        }
        
        // Agrupar logros por juego
        Map<Long, List<UserGameAchievement>> achievementsByGame = allUserAchievements.stream()
                .collect(Collectors.groupingBy(UserGameAchievement::getAppId));
        
        // Calcular estadísticas por juego
        List<AchievementStatsDto.GameAchievementProgress> gameProgressList = new ArrayList<>();
        int totalUnlocked = 0;
        int totalAvailable = 0;
        
        for (Map.Entry<Long, List<UserGameAchievement>> entry : achievementsByGame.entrySet()) {
            Long appId = entry.getKey();
            List<UserGameAchievement> gameAchievements = entry.getValue();
            
            // Obtener total de logros del juego desde schema
            long totalAchievements = gameAchievementRepository.countByAppId(appId);
            if (totalAchievements == 0) {
                continue; // Skip si no hay schema
            }
            
            long unlockedCount = gameAchievements.stream()
                    .filter(UserGameAchievement::getAchieved)
                    .count();
            
            double percentage = (unlockedCount * 100.0) / totalAchievements;
            
            totalUnlocked += unlockedCount;
            totalAvailable += totalAchievements;
            
            AchievementStatsDto.GameAchievementProgress progress = AchievementStatsDto.GameAchievementProgress.builder()
                    .appId(appId)
                    .totalAchievements((int) totalAchievements)
                    .unlockedAchievements((int) unlockedCount)
                    .percentage(percentage)
                    .build();
            
            gameProgressList.add(progress);
        }
        
        // Ordenar por porcentaje descendente
        gameProgressList.sort((a, b) -> Double.compare(b.getPercentage(), a.getPercentage()));
        
        // Filtrar juegos 100%
        List<AchievementStatsDto.GameAchievementProgress> juegosCompletos = gameProgressList.stream()
                .filter(g -> g.getPercentage() >= 100.0)
                .collect(Collectors.toList());
        
        // Filtrar juegos cercanos a 100% (80-99%)
        List<AchievementStatsDto.GameAchievementProgress> juegosCercanos = gameProgressList.stream()
                .filter(g -> g.getPercentage() >= 80.0 && g.getPercentage() < 100.0)
                .limit(10)
                .collect(Collectors.toList());
        
        // Juegos con más progreso (basado en tiempo jugado si se proporciona)
        List<AchievementStatsDto.GameAchievementProgress> juegosMasProgreso;
        if (topGamesWithPlaytime != null && !topGamesWithPlaytime.isEmpty()) {
            // Ordenar según el orden de topGamesWithPlaytime
            Map<Long, Integer> playtimeOrder = new HashMap<>();
            for (int i = 0; i < topGamesWithPlaytime.size(); i++) {
                playtimeOrder.put(topGamesWithPlaytime.get(i), i);
            }
            
            juegosMasProgreso = gameProgressList.stream()
                    .filter(g -> playtimeOrder.containsKey(g.getAppId()))
                    .sorted((a, b) -> {
                        Integer orderA = playtimeOrder.get(a.getAppId());
                        Integer orderB = playtimeOrder.get(b.getAppId());
                        return orderA.compareTo(orderB);
                    })
                    .limit(10)
                    .collect(Collectors.toList());
        } else {
            // Si no hay info de playtime, tomar top 10 por porcentaje
            juegosMasProgreso = gameProgressList.stream()
                    .limit(10)
                    .collect(Collectors.toList());
        }
        
        double porcentajeGlobal = totalAvailable > 0 ? (totalUnlocked * 100.0) / totalAvailable : 0.0;
        
        return AchievementStatsDto.builder()
                .steamId(steamId)
                .totalJuegosConLogros(achievementsByGame.size())
                .totalLogrosDesbloqueados(totalUnlocked)
                .totalLogrosDisponibles(totalAvailable)
                .porcentajeGlobal(porcentajeGlobal)
                .juegosCompletos100(juegosCompletos)
                .juegosCercanos100(juegosCercanos)
                .juegosMasProgreso(juegosMasProgreso)
                .build();
    }
}
