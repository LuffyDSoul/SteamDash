package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para un juego en la biblioteca del usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JuegoUsuarioDto {
    private Integer appId;
    private String name;
    private Integer playtimeForever; // en minutos
    private Integer playtime2Weeks; // en minutos
    private String headerImage;
    private String price;
    private Boolean isFree;
    private String storeUrl;
    private String libraryImage;
    private java.util.List<String> tags;
    
    // Indica si el juego es prestado de biblioteca familiar
    private Boolean isBorrowed;
    
    // Logros del usuario para este juego (solo los que ya están en BD)
    private Integer totalAchievements;
    private Integer unlockedAchievements;
}
