package com.dacs.conector.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.conector.dto.steamDTO.SteamAppListResponseDto;
import com.dacs.conector.dto.steamDTO.SteamGameDto;
import com.dacs.conector.dto.steamDTO.SteamGameSchemaResponseDto;
import com.dacs.conector.dto.steamDTO.SteamMostPlayedGamesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamNewsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamOwnedGamesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamPlayerAchievementsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamUserStatsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamPlayerSummariesResponseDto;
import com.dacs.conector.service.SteamApiServiceImpl;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/steam")
public class SteamController {

    @Autowired
    private SteamApiServiceImpl steamApiService;

    /**
     * Obtener detalles de un juego mediante su ID
     * GET /steam/game/{appId}
     */
    @GetMapping("/game/{appId}")
    public ResponseEntity<SteamGameDto> getGameDetails(@PathVariable String appId) {
        try {
            SteamGameDto gameDetails = steamApiService.getGameDetails(appId);
            if (gameDetails != null) {
                return ResponseEntity.ok(gameDetails);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener noticias de un juego mediante su ID
     * GET /steam/game/{appId}/news?count=10&maxLength=300
     */
    @GetMapping("/game/{appId}/news")
    public ResponseEntity<SteamNewsResponseDto> getGameNews(
            @PathVariable("appId") String appId,
            @RequestParam(name = "count", defaultValue = "10") Integer count,
            @RequestParam(name = "maxLength", defaultValue = "300") Integer maxLength) {
        try {
            SteamNewsResponseDto news = steamApiService.getNewsForApp(appId, count, maxLength);
            return ResponseEntity.ok(news);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener appdetails desde SteamSpy (solo tags nos interesan)
     * GET /steam/spy/appdetails/{appId}
     */
    @GetMapping("/spy/appdetails/{appId}")
    public ResponseEntity<java.util.Map<String, Object>> getSteamSpyAppDetails(@PathVariable String appId) {
        try {
            java.util.Map<String, Object> details = steamApiService.getSteamSpyAppDetails(appId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener juegos de un usuario mediante su ID
     * GET /steam/user/{steamId}/games?includeAppInfo=true&includePlayedFreeGames=true
     */
    @GetMapping("/user/{steamId}/games")
    public ResponseEntity<SteamOwnedGamesResponseDto> getUserGames(
            @PathVariable String steamId,
            @RequestParam(defaultValue = "true") Boolean includeAppInfo,
            @RequestParam(defaultValue = "true") Boolean includePlayedFreeGames) {
        try {
            SteamOwnedGamesResponseDto games = steamApiService.getOwnedGames(steamId, includeAppInfo, includePlayedFreeGames);
            return ResponseEntity.ok(games);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener juegos jugados recientemente por el usuario (últimas 2 semanas)
     * GET /steam/user/{steamId}/recently
     */
    @GetMapping("/user/{steamId}/recently")
    public ResponseEntity<com.dacs.conector.dto.steamDTO.SteamRecentlyPlayedGamesResponseDto> getRecentlyPlayed(
            @PathVariable String steamId) {
        try {
            com.dacs.conector.dto.steamDTO.SteamRecentlyPlayedGamesResponseDto result = steamApiService.getRecentlyPlayedGames(steamId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener lista completa de todas las aplicaciones de Steam 
     * GET /steam/allAppsList
     */
    @GetMapping("/allAppsList")
    public ResponseEntity<SteamAppListResponseDto> getAllApps() {
        try {
            SteamAppListResponseDto apps = steamApiService.getAllApps();
            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener estadísticas de un usuario para un juego específico
     * GET /steam/user/{steamId}/game/{appId}/stats
     */
    @GetMapping("/user/{steamId}/game/{appId}/stats")
    public ResponseEntity<SteamUserStatsResponseDto> getUserGameStats(
            @PathVariable String steamId,
            @PathVariable String appId) {
        try {
            SteamUserStatsResponseDto stats = steamApiService.getUserStatsForGame(steamId, appId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener juegos más jugados en Steam (global)
     * GET /steam/most-played
     */
    @GetMapping("/mostPlayedGames")
    public ResponseEntity<SteamMostPlayedGamesResponseDto> getMostPlayedGames() {
        try {
            SteamMostPlayedGamesResponseDto mostPlayed = steamApiService.getMostPlayedGames();
            return ResponseEntity.ok(mostPlayed);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener logros de un jugador para un juego específico mediante ID Usuario, e ID juego
     * GET /steam/user/{steamId}/game/{appId}/achievements?language=english
     */
    @GetMapping("/user/{steamId}/game/{appId}/achievements")
    public ResponseEntity<SteamPlayerAchievementsResponseDto> getPlayerAchievements(
            @PathVariable String steamId,
            @PathVariable String appId,
            @RequestParam(defaultValue = "english") String language) {
        try {
            SteamPlayerAchievementsResponseDto achievements = steamApiService.getPlayerAchievements(steamId, appId, language);
            return ResponseEntity.ok(achievements);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener información pública de players (persona name, avatar)
     * GET /steam/player-summaries?steamids=123,456
     */
    @GetMapping("/player-summaries")
    public ResponseEntity<SteamPlayerSummariesResponseDto> getPlayerSummaries(
            @RequestParam String steamids) {
        try {
            SteamPlayerSummariesResponseDto summaries = steamApiService.getPlayerSummaries(steamids);
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtener esquema de logros y estadísticas de un juego
     * GET /steam/game/{appId}/schema?language=english
     */
    @GetMapping("/game/{appId}/schema")
    public ResponseEntity<SteamGameSchemaResponseDto> getGameSchema(
            @PathVariable String appId,
            @RequestParam(defaultValue = "english") String language) {
        try {
            log.info("Obteniendo schema de logros para appId: {}, language: {}", appId, language);
            SteamGameSchemaResponseDto schema = steamApiService.getSchemaForGame(appId, language);
            
            // Validar que el schema tenga datos
            if (schema == null || schema.getGame() == null) {
                log.warn("Schema vacío para appId: {} - El juego puede no tener logros", appId);
                return ResponseEntity.noContent().build();
            }
            
            // Validar que tenga achievements
            if (schema.getGame().getAvailableGameStats() == null || 
                schema.getGame().getAvailableGameStats().getAchievements() == null ||
                schema.getGame().getAvailableGameStats().getAchievements().isEmpty()) {
                log.warn("El juego {} no tiene logros definidos en el schema", appId);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Schema obtenido exitosamente para appId: {} con {} logros", 
                    appId, schema.getGame().getAvailableGameStats().getAchievements().size());
            return ResponseEntity.ok(schema);
        } catch (feign.FeignException.NotFound e) {
            log.warn("Schema no encontrado para appId: {} - El juego no tiene logros", appId);
            return ResponseEntity.notFound().build();
        } catch (feign.FeignException.BadRequest e) {
            log.warn("Bad request al obtener schema para appId: {} - Parámetros inválidos o juego sin soporte de logros", appId);
            return ResponseEntity.notFound().build();
        } catch (feign.FeignException e) {
            log.error("Error de Feign al obtener schema para appId {}: {} - Status: {}", 
                     appId, e.getMessage(), e.status());
            // Si es un error de la Steam API (como 403, 500), devolver 404 porque probablemente el juego no tiene logros
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error inesperado al obtener schema para appId {}: {}", appId, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Resolver vanity URL a steamid64
     * GET /steam/resolve-vanity?vanityurl=luzier
     */
    @GetMapping("/resolve-vanity")
    public ResponseEntity<com.dacs.conector.dto.steamDTO.SteamResolveVanityResponseDto> resolveVanity(
            @RequestParam("vanityurl") String vanityUrl) {
        try {
            var resp = steamApiService.resolveVanityUrl(vanityUrl);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}