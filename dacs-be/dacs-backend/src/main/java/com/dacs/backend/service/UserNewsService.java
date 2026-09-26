package com.dacs.backend.service;

import com.dacs.backend.dto.UserNewsResponseDto;

import java.util.List;

/**
 * Servicio para manejar noticias de juegos de usuarios
 */
public interface UserNewsService {
    
    /**
     * Procesar y filtrar noticias de juegos de un usuario
     * @param steamId ID de Steam del usuario
     * @param gamesWithNews Lista de juegos con sus noticias
     * @param page Página actual (0-based)
     * @param pageSize Tamaño de página
     * @return DTO con noticias filtradas y marcadas como nuevas
     */
    UserNewsResponseDto processUserNews(String steamId, List<GameNewsInput> gamesWithNews, int page, int pageSize);
    
    /**
     * Marcar noticias como vistas
     * @param steamId ID de Steam del usuario
     * @param newsToMark Lista de noticias a marcar como vistas
     */
    void markNewsAsSeen(String steamId, List<NewsToMark> newsToMark);
    
    /**
     * Limpiar caché antigua (más de 30 días)
     */
    void cleanOldCache();
    
    /**
     * Input para procesar noticias de un juego
     */
    public static class GameNewsInput {
        public Long appId;
        public String gameName;
        public List<NewsItemInput> news;
        
        public static class NewsItemInput {
            public String gid;
            public String title;
            public String url;
            public Boolean isExternalUrl;
            public String author;
            public String contents;
            public String feedLabel;
            public Long date;
            public String feedName;
            public Integer feedType;
            public Long appId;
        }
    }
    
    /**
     * Input para marcar noticias como vistas
     */
    public static class NewsToMark {
        public String gid;
        public Long appId;
        public Long date;
    }
}
