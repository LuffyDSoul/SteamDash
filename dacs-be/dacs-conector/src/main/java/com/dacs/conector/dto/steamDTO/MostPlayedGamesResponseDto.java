package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class MostPlayedGamesResponseDto {
    @JsonProperty("response")
    private MostPlayedResponseDto response;
    
    @Getter
    @Setter
    public static class MostPlayedResponseDto {
        @JsonProperty("ranks")
        private List<GameRankDto> ranks;
    }
    
    @Getter
    @Setter
    public static class GameRankDto {
        @JsonProperty("rank")
        private Integer rank;
        
        @JsonProperty("appid")
        private Long appId;
        
        @JsonProperty("last_week_rank")
        private Integer lastWeekRank;
        
        @JsonProperty("peak_in_game")
        private Integer peakInGame;
    }
}