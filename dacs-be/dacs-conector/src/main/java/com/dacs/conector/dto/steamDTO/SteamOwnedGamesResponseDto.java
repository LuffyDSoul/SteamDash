package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class SteamOwnedGamesResponseDto {
    @JsonProperty("response")
    private OwnedGamesResponse response;
    
    @Getter
    @Setter
    public static class OwnedGamesResponse {
        @JsonProperty("game_count")
        private Integer gameCount;
        
        @JsonProperty("games")
        private List<OwnedGameDto> games;
    }
    
    @Getter
    @Setter
    public static class OwnedGameDto {
        @JsonProperty("appid")
        private Long appId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("playtime_forever")
        private Integer playtimeForever;
        
        @JsonProperty("img_icon_url")
        private String imgIconUrl;
        
        @JsonProperty("img_logo_url")
        private String imgLogoUrl;
        
        @JsonProperty("has_community_visible_stats")
        private Boolean hasCommunityVisibleStats;
        
        @JsonProperty("playtime_windows_forever")
        private Integer playtimeWindowsForever;
        
        @JsonProperty("playtime_mac_forever")
        private Integer playtimeMacForever;
        
        @JsonProperty("playtime_linux_forever")
        private Integer playtimeLinuxForever;
        
        @JsonProperty("rtime_last_played")
        private Long rtimeLastPlayed;
        
        @JsonProperty("playtime_disconnected")
        private Integer playtimeDisconnected;
    }
}