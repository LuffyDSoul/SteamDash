package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class SteamPlayerAchievementsResponseDto {
    @JsonProperty("playerstats")
    private PlayerAchievementsDto playerStats;
    
    @Getter
    @Setter
    public static class PlayerAchievementsDto {
        @JsonProperty("steamID")
        private String steamId;
        
        @JsonProperty("gameName")
        private String gameName;
        
        @JsonProperty("achievements")
        private List<PlayerAchievementDto> achievements;
        
        @JsonProperty("success")
        private Boolean success;
    }
    
    @Getter
    @Setter
    public static class PlayerAchievementDto {
        @JsonProperty("apiname")
        private String apiName;
        
        @JsonProperty("achieved")
        private Integer achieved;
        
        @JsonProperty("unlocktime")
        private Long unlockTime;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("description")
        private String description;
    }
}