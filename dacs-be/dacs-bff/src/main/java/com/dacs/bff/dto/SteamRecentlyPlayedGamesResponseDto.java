package com.dacs.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SteamRecentlyPlayedGamesResponseDto {
    @JsonProperty("response")
    private RecentlyPlayedResponse response;

    @Getter
    @Setter
    public static class RecentlyPlayedResponse {
        @JsonProperty("total_count")
        private Integer totalCount;

        @JsonProperty("games")
        private List<RecentlyPlayedGameDto> games;
    }

    @Getter
    @Setter
    public static class RecentlyPlayedGameDto {
        @JsonProperty("appid")
        private Long appId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("playtime_2weeks")
        private Integer playtime2Weeks;

        @JsonProperty("playtime_forever")
        private Integer playtimeForever;

        @JsonProperty("img_icon_url")
        private String imgIconUrl;

        @JsonProperty("img_logo_url")
        private String imgLogoUrl;
    }
}
