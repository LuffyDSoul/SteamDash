package com.dacs.bff.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SteamNewsResponseDto {
    @JsonProperty("appnews")
    private AppNewsDto appNews;

    @Getter
    @Setter
    public static class AppNewsDto {
        @JsonProperty("appid")
        private Long appId;

        @JsonProperty("newsitems")
        private List<NewsItemDto> newsItems;

        @JsonProperty("count")
        private Integer count;
    }

    @Getter
    @Setter
    public static class NewsItemDto {
        @JsonProperty("gid")
        private String gid;

        @JsonProperty("title")
        private String title;

        @JsonProperty("url")
        private String url;

        @JsonProperty("is_external_url")
        private Boolean isExternalUrl;

        @JsonProperty("author")
        private String author;

        @JsonProperty("contents")
        private String contents;

        @JsonProperty("feedlabel")
        private String feedLabel;

        @JsonProperty("date")
        private Long date;

        @JsonProperty("feedname")
        private String feedName;

        @JsonProperty("feed_type")
        private Integer feedType;

        @JsonProperty("appid")
        private Long appId;
    }
}
