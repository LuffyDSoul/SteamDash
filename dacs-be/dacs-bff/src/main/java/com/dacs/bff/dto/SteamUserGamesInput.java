package com.dacs.bff.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para entrada de datos de usuario y juegos de Steam
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteamUserGamesInput {
    
    /**
     * Steam ID del usuario
     */
    private String steamId;
    
    /**
     * Información del jugador
     */
    private PlayerInfo playerInfo;
    
    /**
     * Lista de juegos del jugador
     */
    private List<GameInfo> games;
    
    /**
     * Información del jugador
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerInfo {
        private String steamId;
        private String personaName;
        private String avatarFull;
        private String profileUrl;
        private String localCountryCode;
        private Long timeCreated;
    }
    
    /**
     * Información de un juego
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameInfo {
        private Integer appId;
        private String name;
        private Integer playtimeForever;
        private String headerImage;
        private String imgIconUrl; // img_icon_url from GetOwnedGames include_appinfo
        private Boolean isFree;
        private String price;
        private java.util.List<com.dacs.bff.dto.Tags> tags; // Tags futuros (SteamSpy)
    }
}
