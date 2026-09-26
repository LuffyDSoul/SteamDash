package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class SteamUserStatsResponseDto {
    @JsonProperty("playerstats")
    private PlayerStatsDto playerStats;
    
    @Getter
    @Setter
    public static class PlayerStatsDto {
        @JsonProperty("steamID")
        private String steamId;
        
        @JsonProperty("gameName")
        private String gameName;
        
        @JsonProperty("stats")
        private List<StatDto> stats;
        
        @JsonProperty("achievements")
        private List<AchievementDto> achievements;
    }
    
    @Getter
    @Setter
    public static class StatDto {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("value")
        private Integer value;
    }
    
    @Getter
    @Setter
    public static class AchievementDto {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("achieved")
        private Integer achieved;
        
        @JsonProperty("unlocktime")
        private Long unlockTime;
    }
}