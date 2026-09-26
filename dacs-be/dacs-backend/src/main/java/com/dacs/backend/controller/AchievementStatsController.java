package com.dacs.backend.controller;

import com.dacs.backend.dto.AchievementStatsDto;
import com.dacs.backend.service.AchievementStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para estadísticas de logros
 */
@Slf4j
@RestController
@RequestMapping("/achievement-stats")
@RequiredArgsConstructor
public class AchievementStatsController {
    
    private final AchievementStatsService achievementStatsService;
    
    /**
     * Obtiene estadísticas de logros para un usuario
     * @param steamId Steam ID del usuario
     * @param topGames Lista opcional de appIds ordenados por tiempo jugado (para optimización)
     * @return Estadísticas de logros
     */
    @PostMapping("/{steamId}")
    public ResponseEntity<AchievementStatsDto> getAchievementStats(
            @PathVariable String steamId,
            @RequestBody(required = false) List<Long> topGames) {
        
        log.info("Obteniendo estadísticas de logros para usuario {}", steamId);
        
        AchievementStatsDto stats = achievementStatsService.calculateAchievementStats(steamId, topGames);
        
        return ResponseEntity.ok(stats);
    }
}
