package com.dacs.bff.service;

import com.dacs.bff.dto.UserNewsResponseDto;
import com.dacs.bff.dto.GameDetailsDto;

/**
 * Servicio de orquestación para noticias de usuario
 */
public interface UserNewsOrchestrationService {
    
    /**
     * Obtener noticias de TODOS los juegos del usuario (owned games)
     * @param steamId ID de Steam del usuario
     * @param page Página (0-based)
     * @param pageSize Cantidad de juegos por página (default 25)
     * @return DTO con noticias organizadas por juego
     */
    UserNewsResponseDto getAllUserGamesNews(String steamId, int page, int pageSize);
    
    /**
     * Obtener noticias de los juegos JUGADOS RECIENTEMENTE (últimas 2 semanas)
     * @param steamId ID de Steam del usuario
     * @param page Página (0-based)
     * @param pageSize Cantidad de juegos por página (default 25)
     * @return DTO con noticias organizadas por juego
     */
    UserNewsResponseDto getRecentlyPlayedGamesNews(String steamId, int page, int pageSize);
    
    /**
     * Buscar detalles de un juego específico desde Steam Store
     * @param appId ID de la aplicación
     * @return Detalles del juego
     */
    GameDetailsDto searchGameDetails(String appId);
}
