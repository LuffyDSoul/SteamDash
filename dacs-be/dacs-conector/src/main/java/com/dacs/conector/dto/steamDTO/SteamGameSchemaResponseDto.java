package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class SteamGameSchemaResponseDto {
    @JsonProperty("game")
    private GameSchemaDto game;
    
    @Getter
    @Setter
    public static class GameSchemaDto {
        @JsonProperty("gameName")
        private String gameName;
        
        @JsonProperty("gameVersion")
        private String gameVersion;
        
        @JsonProperty("availableGameStats")
        private AvailableGameStatsDto availableGameStats;
    }
    
    @Getter
    @Setter
    public static class AvailableGameStatsDto {
        @JsonProperty("stats")
        private List<SchemaStatDto> stats;
        
        @JsonProperty("achievements")
        private List<SchemaAchievementDto> achievements;
    }
    
    @Getter
    @Setter
    public static class SchemaStatDto {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("defaultvalue")
        private Long defaultValue; // Cambiado a Long para soportar valores grandes como 4294967295
        
        @JsonProperty("displayName")
        private String displayName;
    }
    
    @Getter
    @Setter
    public static class SchemaAchievementDto {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("defaultvalue")
        private Long defaultValue; // Cambiado a Long para soportar valores grandes
        
        @JsonProperty("displayName")
        private String displayName;
        
        @JsonProperty("hidden")
        private Integer hidden;
        
        @JsonProperty("description")
        private String description;
        
        @JsonProperty("icon")
        private String icon;
        
        @JsonProperty("icongray")
        private String iconGray;
    }
}