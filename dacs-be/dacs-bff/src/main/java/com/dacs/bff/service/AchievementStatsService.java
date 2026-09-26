package com.dacs.bff.service;

import com.dacs.bff.dto.AchievementStatsDto;

import java.util.List;

/**
 * Interface del servicio de estadísticas de logros
 */
public interface AchievementStatsService {
    
    /**
     * Obtiene estadísticas de logros enriquecidas para un usuario
     * @param steamId Steam ID del usuario
     * @return Estadísticas de logros con información enriquecida desde la biblioteca
     */
    AchievementStatsDto getAchievementStatsEnriched(String steamId);
    
    /**
     * Inicia la sincronización de logros para todos los juegos del usuario
     * @param steamId Steam ID del usuario
     * @return Mensaje de estado
     */
    String syncAllAchievements(String steamId);
}
