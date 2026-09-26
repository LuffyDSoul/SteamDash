package com.dacs.bff.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SteamPlayerSummariesResponseDto {
    @JsonProperty("response")
    private ResponseDto response;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseDto {
        @JsonProperty("players")
        private List<PlayerDto> players;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerDto {
        @JsonProperty("steamid")
        private String steamId;

        @JsonProperty("personaname")
        private String personaName;

        @JsonProperty("avatarfull")
        private String avatarFull;
        
        @JsonProperty("profileurl")
        private String profileUrl;
        
        @JsonProperty("loccountrycode")
        private String localCountryCode;
        
        @JsonProperty("timecreated")
        private Long timeCreated;
        
        @JsonProperty("communityvisibilitystate")
        private Integer communityVisibilityState;
    }
}
