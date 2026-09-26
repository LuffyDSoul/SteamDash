package com.dacs.bff.controller;

import com.dacs.bff.dto.GameDetailsDto;
import com.dacs.bff.dto.UserNewsResponseDto;
import com.dacs.bff.service.SteamIdResolverService;
import com.dacs.bff.service.UserNewsOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para noticias de usuario
 */
@RestController
@RequestMapping("/user-news")
@RequiredArgsConstructor
@Slf4j
public class UserNewsController {
    
    private final UserNewsOrchestrationService userNewsOrchestrationService;
    private final SteamIdResolverService steamIdResolverService;
    
    /**
     * Obtener noticias de TODOS los juegos de un usuario (owned games)
     * GET /user-news/{steamId}?page=0&pageSize=25
     */
    @GetMapping("/{steamId}")
    public ResponseEntity<UserNewsResponseDto> getUserGamesNews(
            @PathVariable String steamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
        
        log.info("Getting news for user: {}, page: {}, pageSize: {}", steamId, page, pageSize);
        String resolvedId;
        try {
            resolvedId = steamIdResolverService.resolve(steamId);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot resolve vanity '{}': {}", steamId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
        
        try {
            UserNewsResponseDto response = userNewsOrchestrationService.getAllUserGamesNews(resolvedId, page, pageSize);
            log.info("Successfully retrieved news for user: {}, games count: {}", steamId, response.getGamesNews().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting user games news for steamId: {}", steamId, e);
            log.error("Exception type: {}, message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {}, message: {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Obtener noticias de los juegos JUGADOS RECIENTEMENTE
     * GET /user-news/{steamId}/recently-played?page=0&pageSize=25
     */
    @GetMapping("/{steamId}/recently-played")
    public ResponseEntity<UserNewsResponseDto> getRecentlyPlayedGamesNews(
            @PathVariable String steamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int pageSize) {
        
        log.info("Getting recently played games news for user: {}, page: {}, pageSize: {}", steamId, page, pageSize);
        String resolvedId;
        try {
            resolvedId = steamIdResolverService.resolve(steamId);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot resolve vanity '{}': {}", steamId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
        
        try {
            UserNewsResponseDto response = userNewsOrchestrationService.getRecentlyPlayedGamesNews(resolvedId, page, pageSize);
            log.info("Successfully retrieved recently played news for user: {}, games count: {}", steamId, response.getGamesNews().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting recently played games news for steamId: {}", steamId, e);
            log.error("Exception type: {}, message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                log.error("Caused by: {}, message: {}", e.getCause().getClass().getName(), e.getCause().getMessage());
            }
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Buscar detalles de un juego específico
     * GET /user-news/game-search/{appId}
     */
    @GetMapping("/game-search/{appId}")
    public ResponseEntity<GameDetailsDto> searchGameDetails(@PathVariable String appId) {
        log.info("Searching game details for appId: {}", appId);
        
        try {
            GameDetailsDto gameDetails = userNewsOrchestrationService.searchGameDetails(appId);
            
            if (gameDetails == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(gameDetails);
        } catch (Exception e) {
            log.error("Error searching game details", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
