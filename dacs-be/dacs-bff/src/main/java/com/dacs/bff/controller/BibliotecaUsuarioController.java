package com.dacs.bff.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.dto.BibliotecaUsuarioDto;
import com.dacs.bff.dto.GameAchievementsDto;
import com.dacs.bff.dto.JuegoAleatorioDto;
import com.dacs.bff.dto.RefreshAllGamesResponseDto;
import com.dacs.bff.dto.RefreshJuegoResponseDto;
import com.dacs.bff.dto.SteamRecentlyPlayedGamesResponseDto;
import com.dacs.bff.dto.SyncBibliotecaResponseDto;
import com.dacs.bff.service.BibliotecaUsuarioService;
import com.dacs.bff.service.SteamIdResolverService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para gestionar la biblioteca de juegos de un usuario
 */
@Slf4j
@RestController
@RequestMapping("/usuarios/{steamId}")
public class BibliotecaUsuarioController {

    @Autowired
    private BibliotecaUsuarioService bibliotecaUsuarioService;

    @Autowired
    private ApiConectorClient apiConectorClient;

    @Autowired
    private SteamIdResolverService steamIdResolverService;

    /**
     * GET /usuarios/{steamId}/biblioteca
     * Obtiene la biblioteca completa del usuario
     */
    @GetMapping("/biblioteca")
    public ResponseEntity<BibliotecaUsuarioDto> getBiblioteca(
            @PathVariable String steamId,
            @RequestParam(required = false, defaultValue = "false") boolean refresh) {
        try {
            log.info("BFF - Obteniendo biblioteca para steamId: {}, refresh: {}", steamId, refresh);
            String resolvedId = steamIdResolverService.resolve(steamId);
            BibliotecaUsuarioDto biblioteca = bibliotecaUsuarioService.getBibliotecaUsuario(resolvedId, refresh);
            return ResponseEntity.ok(biblioteca);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error obteniendo biblioteca: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /usuarios/{steamId}/biblioteca/recently-played
     * Obtiene los juegos jugados recientemente
     */
    @GetMapping("/biblioteca/recently-played")
    public ResponseEntity<SteamRecentlyPlayedGamesResponseDto> getRecentlyPlayed(
            @PathVariable String steamId) {
        try {
            log.info("BFF - Obteniendo recently-played para steamId: {}", steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            SteamRecentlyPlayedGamesResponseDto response = apiConectorClient.getRecentlyPlayedGames(resolvedId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error obteniendo recently-played: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /usuarios/{steamId}/biblioteca/juego-aleatorio
     * Obtiene un juego aleatorio de la biblioteca
     */
    @GetMapping("/biblioteca/juego-aleatorio")
    public ResponseEntity<JuegoAleatorioDto> getJuegoAleatorio(
            @PathVariable String steamId,
            @RequestParam(required = false) Integer maxHoras,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Long excludeAppId) {
        try {
            log.info("BFF - Obteniendo juego aleatorio para steamId: {}, maxHoras: {}, tags: {}, excludeAppId: {}", 
                     steamId, maxHoras, tags, excludeAppId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            JuegoAleatorioDto juego = bibliotecaUsuarioService.getJuegoAleatorio(resolvedId, maxHoras, tags, excludeAppId);
            return ResponseEntity.ok(juego);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL o parámetros inválidos: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error obteniendo juego aleatorio: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /usuarios/{steamId}/biblioteca/juegos/{appId}/refresh
     * Actualiza la información de un juego específico
     */
    @PostMapping("/biblioteca/juegos/{appId}/refresh")
    public ResponseEntity<RefreshJuegoResponseDto> refreshJuego(
            @PathVariable String steamId,
            @PathVariable Integer appId) {
        try {
            log.info("BFF - Refrescando juego {} para steamId: {}", appId, steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            RefreshJuegoResponseDto response = bibliotecaUsuarioService.refreshJuego(resolvedId, appId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error refrescando juego: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /usuarios/{steamId}/biblioteca/sync
     * Sincroniza toda la biblioteca con la base de datos
     */
    @PostMapping("/biblioteca/sync")
    public ResponseEntity<SyncBibliotecaResponseDto> syncBiblioteca(
            @PathVariable String steamId) {
        try {
            log.info("BFF - Sincronizando biblioteca para steamId: {}", steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            SyncBibliotecaResponseDto response = bibliotecaUsuarioService.syncBiblioteca(resolvedId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error sincronizando biblioteca: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /usuarios/{steamId}/biblioteca/refresh-all
     * Actualiza todos los juegos de la biblioteca (operación larga)
     */
    @PostMapping("/biblioteca/refresh-all")
    public ResponseEntity<RefreshAllGamesResponseDto> refreshAllGames(
            @PathVariable String steamId) {
        try {
            log.info("BFF - Refrescando todos los juegos para steamId: {}", steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            RefreshAllGamesResponseDto response = bibliotecaUsuarioService.refreshAllGames(resolvedId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error refrescando todos los juegos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /usuarios/{steamId}/juegos/{appId}/logros
     * Obtiene los logros de un juego específico para el usuario
     */
    @GetMapping("/juegos/{appId}/logros")
    public ResponseEntity<GameAchievementsDto> getGameAchievements(
            @PathVariable String steamId,
            @PathVariable Long appId) {
        try {
            log.info("BFF - Obteniendo logros del juego {} para steamId: {}", appId, steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            GameAchievementsDto achievements = bibliotecaUsuarioService.getGameAchievements(resolvedId, appId);
            return ResponseEntity.ok(achievements);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error obteniendo logros del juego: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /usuarios/{steamId}/juegos/{appId}/logros/refresh
     * Refresca forzadamente los logros de un juego desde Steam API
     */
    @PostMapping("/juegos/{appId}/logros/refresh")
    public ResponseEntity<GameAchievementsDto> refreshGameAchievements(
            @PathVariable String steamId,
            @PathVariable Long appId) {
        try {
            log.info("BFF - Refrescando logros del juego {} para steamId: {}", appId, steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            GameAchievementsDto achievements = bibliotecaUsuarioService.refreshGameAchievements(resolvedId, appId);
            return ResponseEntity.ok(achievements);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error refrescando logros del juego: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
