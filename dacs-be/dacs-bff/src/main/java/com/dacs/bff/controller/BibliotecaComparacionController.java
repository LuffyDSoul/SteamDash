package com.dacs.bff.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.dto.BibliotecaComparacionDto;
import com.dacs.bff.service.BibliotecaComparacionOrchestrationService;
import com.dacs.bff.service.SteamIdResolverService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para comparación de bibliotecas de Steam
 */
@Slf4j
@RestController
@RequestMapping("/usuarios")
public class BibliotecaComparacionController {

    @Autowired
    private BibliotecaComparacionOrchestrationService bibliotecaComparacionService;

    @Autowired
    private SteamIdResolverService steamIdResolverService;

    /**
     * Endpoint para comparar bibliotecas de dos usuarios
     * GET /usuarios/comparar/{steamId1}/con/{steamId2}
     */
    @GetMapping("/comparar/{steamId1}/con/{steamId2}")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecas(
            @PathVariable String steamId1,
            @PathVariable String steamId2) {
        
        log.info("BFF - Endpoint comparación de bibliotecas: {} con {}", steamId1, steamId2);
        try {
            String r1 = steamIdResolverService.resolve(steamId1);
            String r2 = steamIdResolverService.resolve(steamId2);
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(r1, r2);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint para comparar bibliotecas de 3 usuarios
     * GET /usuarios/comparar/{steamId1}/con/{steamId2}/con/{steamId3}
     */
    @GetMapping("/comparar/{steamId1}/con/{steamId2}/con/{steamId3}")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecas3(
            @PathVariable String steamId1,
            @PathVariable String steamId2,
            @PathVariable String steamId3) {
        
        log.info("BFF - Comparación de 3 usuarios: {}, {}, {}", steamId1, steamId2, steamId3);
        
        try {
            List<String> steamIds = Arrays.asList(steamId1, steamId2, steamId3);
            List<String> resolved = steamIds.stream().map(steamIdResolverService::resolve).toList();
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(resolved);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint para comparar bibliotecas de 4 usuarios
     * GET /usuarios/comparar/{steamId1}/con/{steamId2}/con/{steamId3}/con/{steamId4}
     */
    @GetMapping("/comparar/{steamId1}/con/{steamId2}/con/{steamId3}/con/{steamId4}")
        public ResponseEntity<BibliotecaComparacionDto> compararBibliotecas4(
            @PathVariable String steamId1,
            @PathVariable String steamId2,
            @PathVariable String steamId3,
            @PathVariable String steamId4) {
        
        log.info("BFF - Comparación de 4 usuarios: {}, {}, {}, {}", steamId1, steamId2, steamId3, steamId4);
        
        try {
            List<String> steamIds = Arrays.asList(steamId1, steamId2, steamId3, steamId4);
            List<String> resolved = steamIds.stream().map(steamIdResolverService::resolve).toList();
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(resolved);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint para comparar bibliotecas de 5 usuarios
     * GET /usuarios/comparar/{steamId1}/con/{steamId2}/con/{steamId3}/con/{steamId4}/con/{steamId5}
     */
    @GetMapping("/comparar/{steamId1}/con/{steamId2}/con/{steamId3}/con/{steamId4}/con/{steamId5}")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecas5(
            @PathVariable String steamId1,
            @PathVariable String steamId2,
            @PathVariable String steamId3,
            @PathVariable String steamId4,
            @PathVariable String steamId5) {
        
        log.info("BFF - Comparación de 5 usuarios");
        
        try {
            List<String> steamIds = Arrays.asList(steamId1, steamId2, steamId3, steamId4, steamId5);
            List<String> resolved = steamIds.stream().map(steamIdResolverService::resolve).toList();
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(resolved);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint para comparar bibliotecas de 6 usuarios
     * GET /usuarios/comparar/{steamId1}/con/{steamId2}/con/{steamId3}/con/{steamId4}/con/{steamId5}/con/{steamId6}
     */
    @GetMapping("/comparar/{steamId1}/con/{steamId2}/con/{steamId3}/con/{steamId4}/con/{steamId5}/con/{steamId6}")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecas6(
            @PathVariable String steamId1,
            @PathVariable String steamId2,
            @PathVariable String steamId3,
            @PathVariable String steamId4,
            @PathVariable String steamId5,
            @PathVariable String steamId6) {
        
        log.info("BFF - Comparación de 6 usuarios");
        
        try {
            List<String> steamIds = Arrays.asList(steamId1, steamId2, steamId3, steamId4, steamId5, steamId6);
            List<String> resolved = steamIds.stream().map(steamIdResolverService::resolve).toList();
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(resolved);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint para comparar bibliotecas de múltiples usuarios (2-6)
     * POST /usuarios/comparar
     * Body: { "steamIds": ["id1", "id2", "id3"] }
     */
    @PostMapping("/comparar")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecasMultiples(
            @RequestBody CompararBibliotecasRequest request) {
        
        log.info("BFF - Endpoint comparación de bibliotecas múltiples: {}", request.getSteamIds());
        
        try {
            List<String> steamIds = request.getSteamIds();
            
            if (steamIds == null || steamIds.size() < 2) {
                log.warn("Se requieren al menos 2 usuarios para comparar");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            if (steamIds.size() > 6) {
                log.warn("El máximo de usuarios a comparar es 6");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            List<String> resolved = steamIds.stream().map(steamIdResolverService::resolve).toList();
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(resolved);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Endpoint GET para comparar bibliotecas de múltiples usuarios (2-6)
     * GET /usuarios/comparar-multi?ids=id1,id2,id3,id4,id5,id6
     * Ejemplo: /usuarios/comparar-multi?ids=76561198142482352,76561198861705684,76561199096696000
     */
    @GetMapping("/comparar-multi")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecasMultiplesGet(
            @RequestParam("ids") List<String> steamIds) {
        
        log.info("BFF - Endpoint GET comparación de bibliotecas múltiples: {}", steamIds);
        
        try {
            if (steamIds == null || steamIds.size() < 2) {
                log.warn("Se requieren al menos 2 usuarios para comparar");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            if (steamIds.size() > 6) {
                log.warn("El máximo de usuarios a comparar es 6");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            List<String> resolved = steamIds.stream().map(steamIdResolverService::resolve).toList();
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(resolved);
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.warn("Error resolviendo vanity: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * DTO para el request de comparación múltiple
     */
    public static class CompararBibliotecasRequest {
        private List<String> steamIds;
        
        public List<String> getSteamIds() {
            return steamIds;
        }
        
        public void setSteamIds(List<String> steamIds) {
            this.steamIds = steamIds;
        }
    }
}

