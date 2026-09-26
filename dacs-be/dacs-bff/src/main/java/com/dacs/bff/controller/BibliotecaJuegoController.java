package com.dacs.bff.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.api.client.ApiBackendClient;
import com.dacs.bff.client.BackendClient;
import com.dacs.bff.dto.BibliotecaJuegoDto;

import lombok.extern.slf4j.Slf4j;

/**
 * BFF endpoint para devolver la info de un único juego de biblioteca.
 * Simplemente orquesta y pasa el payload del backend sin lógica de negocio.
 */
@Slf4j
@RestController
@RequestMapping
public class BibliotecaJuegoController {

    @Autowired
    private ApiBackendClient backendClient;
    
    @Autowired
    private BackendClient backendClientLogros;

    @GetMapping("/biblioteca-juego/{appId}")
    public ResponseEntity<BibliotecaJuegoDto> getBibliotecaJuego(
            @PathVariable("appId") Long appId,
            @RequestParam(value = "steamId", required = false) String steamId) {
        try {
            log.info("BFF - Obtener biblioteca-juego appId={}, steamId={}", appId, steamId);
            
            // Si se proporciona steamId, hacer ingesta síncrona de logros para este juego
            // Esta operación es OPCIONAL y silenciosa - no debe afectar la respuesta principal
            if (steamId != null && !steamId.trim().isEmpty()) {
                try {
                    log.info("BFF - Ingestionando logros síncronamente para appId={} y usuario={}", appId, steamId);
                    boolean success = backendClientLogros.ingestarLogrosJuegoIndividual(steamId, appId);
                    if (success) {
                        log.info("BFF - Logros actualizados exitosamente para appId={}", appId);
                    } else {
                        log.info("BFF - No se pudieron actualizar logros para appId={} (puede ser que el juego no tenga logros o perfil privado)", appId);
                    }
                } catch (Exception e) {
                    log.info("BFF - No se pudieron ingestar logros para appId={}: {} (esto es normal si el juego no tiene logros)", appId, e.getMessage());
                    // No fallar la respuesta principal si falla la ingesta - esto es completamente normal
                }
            }
            
            BibliotecaJuegoDto dto = backendClient.getBibliotecaJuego(appId);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (Exception e) {
            log.error("BFF - Error obteniendo biblioteca-juego {}: {}", appId, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Alias endpoint para compatibilidad con el frontend.
     * Expone `/biblioteca/juego/{appId}` y delega al handler existente.
     */
    @GetMapping("/biblioteca/juego/{appId}")
    public ResponseEntity<BibliotecaJuegoDto> getBibliotecaJuegoPath(
            @PathVariable("appId") Long appId,
            @RequestParam(value = "steamId", required = false) String steamId) {
        return getBibliotecaJuego(appId, steamId);
    }
}
