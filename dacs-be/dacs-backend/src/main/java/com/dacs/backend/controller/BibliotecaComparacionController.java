package com.dacs.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.backend.dto.BibliotecaComparacionDto;
import com.dacs.backend.dto.SteamUserGamesInput;
import com.dacs.backend.service.BibliotecaComparacionService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador para comparación de bibliotecas de Steam
 */
@Slf4j
@RestController
@RequestMapping("/biblioteca-comparacion")
public class BibliotecaComparacionController {

    @Autowired
    private BibliotecaComparacionService bibliotecaComparacionService;
    
    /**
     * Endpoint para comparar bibliotecas de múltiples usuarios (2-6)
     * POST /biblioteca-comparacion/comparar-multiples
     * Body: Lista de SteamUserGamesInput
     */
    @PostMapping("/comparar-multiples")
    public ResponseEntity<BibliotecaComparacionDto> compararBibliotecasMultiples(
            @RequestBody List<SteamUserGamesInput> usuarios) {
        
        log.info("Backend - Comparando bibliotecas entre {} usuarios", usuarios.size());
        
        try {
            if (usuarios == null || usuarios.size() < 2) {
                log.warn("Se requieren al menos 2 usuarios para comparar");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            if (usuarios.size() > 6) {
                log.warn("El máximo de usuarios a comparar es 6");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            
            BibliotecaComparacionDto resultado = bibliotecaComparacionService.compararBibliotecas(usuarios);
            
            return new ResponseEntity<>(resultado, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            log.error("Argumentos inválidos: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error al comparar bibliotecas: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
