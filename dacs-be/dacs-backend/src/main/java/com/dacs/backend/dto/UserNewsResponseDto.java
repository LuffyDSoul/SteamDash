package com.dacs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO para respuesta de noticias filtradas por usuario
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNewsResponseDto {
    private String steamId;
    private List<GameNewsDto> gamesNews;
    private Integer totalGames;
    private Integer page;
    private Integer pageSize;
    private Boolean hasMore;
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameNewsDto {
        private Long appId;
        private String gameName;
        private String gameImageUrl; // library_600x900.jpg
        private List<NewsItemDto> latestNews; // Últimas 2 noticias
        private List<NewsItemDto> olderNews; // Resto de noticias (para el desplegable)
        private Integer totalNewsCount;
        private Long lastNewsDate; // timestamp de la noticia más reciente
        
        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class NewsItemDto {
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
            private Boolean isNew; // Si es nueva desde última vez
        }
    }
}
