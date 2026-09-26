package com.dacs.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * DTO para datos de juegos de Steam (input para el servicio de comparación)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SteamUserGamesInput {
    private String steamId;
    private PlayerInfo playerInfo;
    private List<GameInfo> games;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlayerInfo {
        private String steamId;
        private String personaName;
        private String avatarFull;
        private String profileUrl;
        private String localCountryCode;
        private Long timeCreated;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GameInfo {
        private Long appId;
        private String name;
        private Integer playtimeForever; // en minutos
        private String headerImage;
        private String imgIconUrl; // img_icon_url from GetOwnedGames include_appinfo
        private Boolean isFree;
        private String price;
        private Boolean appdetailsFailed; // true si appdetails devolvió success: false
        private java.util.List<com.dacs.backend.dto.Tags> tags; // Tags futuros (SteamSpy)
    }
}
