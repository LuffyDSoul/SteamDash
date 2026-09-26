package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO de request para procesar noticias de usuario
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNewsRequestDto {
    private String steamId;
    private List<GameNewsInput> gamesWithNews;
    private Integer page;
    private Integer pageSize;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameNewsInput {
        private Long appId;
        private String gameName;
        private List<NewsItemInput> news;
        
        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NewsItemInput {
            private String gid;
            private String title;
            private String url;
            private Boolean isExternalUrl;
            private String author;
            private String contents;
            private String feedLabel;
            private Long date;
            private String feedName;
            private Integer feedType;
            private Long appId;
        }
    }
}
