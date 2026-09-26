package com.dacs.bff.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para logros de un juego combinando datos del jugador y esquema del juego
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAchievementsDto {
    
    private String steamId;
    private Long appId;
    private String gameName;
    private Integer totalAchievements;
    private Integer unlockedAchievements;
    private List<CombinedAchievementDto> achievements;
    private Boolean success;
    private String error;
}
