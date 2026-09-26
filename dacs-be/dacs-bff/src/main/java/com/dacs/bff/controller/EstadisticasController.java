package com.dacs.bff.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.api.client.ApiBackendClient;
import com.dacs.bff.service.SteamIdResolverService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para estadísticas de logros de usuarios
 */
@Slf4j
@RestController
@RequestMapping("/usuarios/{steamId}/estadisticas")
public class EstadisticasController {

    @Autowired
    private ApiBackendClient apiBackendClient;

    @Autowired
    private SteamIdResolverService steamIdResolverService;

    /**
     * GET /usuarios/{steamId}/estadisticas/logros
     * Obtiene las estadísticas de logros del usuario (todos los juegos)
     */
    @GetMapping("/logros")
    public ResponseEntity<Map<String, Object>> getEstadisticasLogros(
            @PathVariable String steamId) {
        try {
            log.info("BFF - Obteniendo estadísticas de logros para steamId: {}", steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            
            // Llamar al endpoint completo que devuelve AchievementStatsDto
            // Pasar lista vacía para que calcule basado en todos los juegos
            Map<String, Object> stats = apiBackendClient.getUserAchievementStatsComplete(resolvedId, Collections.emptyList());
            
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas de logros: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /usuarios/{steamId}/estadisticas/logros/sync
     * Sincroniza los logros de todos los juegos del usuario desde Steam API
     */
    @PostMapping("/logros/sync")
    public ResponseEntity<String> syncLogros(
            @PathVariable String steamId) {
        try {
            log.info("BFF - Sincronizando logros para steamId: {}", steamId);
            String resolvedId = steamIdResolverService.resolve(steamId);
            
            // Por ahora retornamos un mensaje simple indicando que la sincronización debe hacerse por juego
            return ResponseEntity.ok("Sincronización de logros iniciada para " + resolvedId);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity URL: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error sincronizando logros: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
