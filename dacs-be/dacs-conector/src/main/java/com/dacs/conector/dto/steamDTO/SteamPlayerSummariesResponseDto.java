package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class SteamPlayerSummariesResponseDto {
    @JsonProperty("response")
    private ResponseDto response;

    @Getter
    @Setter
    public static class ResponseDto {
        @JsonProperty("players")
        private List<PlayerDto> players;
    }

    @Getter
    @Setter
    public static class PlayerDto {
        @JsonProperty("steamid")
        private String steamId;

        @JsonProperty("personaname")
        private String personaName;

        @JsonProperty("avatar")
        private String avatar;

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