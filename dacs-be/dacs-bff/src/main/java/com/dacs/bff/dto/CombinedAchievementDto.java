package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para un logro individual con información combinada del jugador y del esquema
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombinedAchievementDto {
    
    private String apiname;
    private String displayName;
    private String description;
    private String icon;
    private String icongray;
    private Boolean achieved;
    private Long unlocktime;
    private Boolean hidden;
}
