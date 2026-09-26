package com.dacs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para estadísticas de logros de un usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementStatsDto {
    
    private String steamId;
    
    // Estadísticas globales
    private Integer totalJuegosConLogros;
    private Integer totalLogrosDesbloqueados;
    private Integer totalLogrosDisponibles;
    private Double porcentajeGlobal;
    
    // Listas de juegos por categoría
    private List<GameAchievementProgress> juegosCompletos100;
    private List<GameAchievementProgress> juegosCercanos100;
    private List<GameAchievementProgress> juegosMasProgreso; // Basado en tiempo jugado
    
    /**
     * Progreso de logros de un juego individual
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameAchievementProgress {
        private Long appId;
        private Integer totalAchievements;
        private Integer unlockedAchievements;
        private Double percentage;
        
        // Información adicional del juego (será enriquecida por el BFF)
        private String gameName;
        private String headerImage;
        private Integer playtimeForever; // minutos
    }
}
