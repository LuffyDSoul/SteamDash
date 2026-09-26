package com.dacs.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.backend.dto.BibliotecaJuegoDto;
import com.dacs.backend.service.BibliotecaJuegoService;
import com.dacs.backend.repository.GameRecordRepository;
import com.dacs.backend.entity.GameRecord;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/biblioteca-juego")
public class BibliotecaJuegoController {

    @Autowired
    private BibliotecaJuegoService bibliotecaJuegoService;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    /**
     * Devuelve un juego enriquecido y persistido por appId
     * GET /biblioteca-juego/{appId}
     */
    @GetMapping("/{appId}")
    public ResponseEntity<BibliotecaJuegoDto> obtenerJuego(@PathVariable("appId") Long appId) {
        try {
            log.info("Backend - Obtener biblioteca-juego appId={}", appId);
            BibliotecaJuegoDto dto = bibliotecaJuegoService.obtenerJuego(appId);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Error al obtener biblioteca-juego {}: {}", appId, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Devuelve, desde la base de datos y sin refrescar fuentes externas, los juegos para los appIds dados.
     * POST /biblioteca-juego/db/bulk
     */
    @PostMapping("/db/bulk")
    public ResponseEntity<java.util.List<BibliotecaJuegoDto>> obtenerJuegosDesdeDb(@RequestBody java.util.List<Long> appIds) {
        try {
            if (appIds == null || appIds.isEmpty()) {
                return new ResponseEntity<>(java.util.Collections.emptyList(), HttpStatus.OK);
            }
            java.util.List<GameRecord> records = gameRecordRepository.findByAppIdIn(appIds);
            java.util.List<BibliotecaJuegoDto> result = new java.util.ArrayList<>();
            for (GameRecord r : records) {
                java.util.List<com.dacs.backend.dto.Tags> tagDtos = new java.util.ArrayList<>();
                if (r.getTags() != null) {
                    for (String t : r.getTags()) {
                        tagDtos.add(com.dacs.backend.dto.Tags.builder().tag(t).build());
                    }
                }
                result.add(BibliotecaJuegoDto.builder()
                        .appId(r.getAppId())
                        .name(r.getName())
                        .headerImage(r.getHeaderImage())
                        .imgVertical(r.getImgVertical())
                        .imgIconUrl(r.getImgIconUrl())
                        .isFree(r.getIsFree())
                        .price(r.getPrice())
                        .storeUrl("https://store.steampowered.com/app/" + r.getAppId())
                        .tags(tagDtos)
                        .build());
            }
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error al obtener juegos desde DB (bulk): {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Busca appIds de juegos que contengan al menos una de las tags especificadas.
     * Optimizado para filtrado a nivel de base de datos.
     * GET /biblioteca-juego/search/by-tags?tags=Action,RPG,Indie
     */
    @GetMapping("/search/by-tags")
    public ResponseEntity<List<Long>> buscarAppIdsPorTags(@RequestParam("tags") List<String> tags) {
        try {
            log.info("Backend - Buscando appIds por tags: {}", tags);
            List<Long> appIds = bibliotecaJuegoService.buscarAppIdsPorTags(tags);
            log.info("Encontrados {} juegos con tags: {}", appIds.size(), tags);
            return new ResponseEntity<>(appIds, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error al buscar por tags {}: {}", tags, e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Guarda o actualiza un juego en la base de datos
     * POST /biblioteca-juego
     */
    @PostMapping
    public ResponseEntity<BibliotecaJuegoDto> upsertJuego(@RequestBody BibliotecaJuegoDto juegoDto) {
        try {
            log.info("Backend - Guardar/actualizar biblioteca-juego appId={}", juegoDto.getAppId());
            
            if (juegoDto.getAppId() == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            // Convertir tags de DTO a List<String>
            java.util.List<String> tags = new java.util.ArrayList<>();
            if (juegoDto.getTags() != null) {
                for (com.dacs.backend.dto.Tags tagDto : juegoDto.getTags()) {
                    if (tagDto.getTag() != null && !tagDto.getTag().isEmpty()) {
                        tags.add(tagDto.getTag());
                    }
                }
            }

            // Buscar si ya existe
            java.util.Optional<GameRecord> existingOpt = gameRecordRepository.findByAppId(juegoDto.getAppId());
            
            GameRecord gameRecord;
            if (existingOpt.isPresent()) {
                // Actualizar existente
                gameRecord = existingOpt.get();
                gameRecord.setName(juegoDto.getName());
                gameRecord.setHeaderImage(juegoDto.getHeaderImage());
                gameRecord.setImgVertical(juegoDto.getImgVertical());
                gameRecord.setImgIconUrl(juegoDto.getImgIconUrl());
                gameRecord.setIsFree(juegoDto.getIsFree());
                gameRecord.setPrice(juegoDto.getPrice());
                gameRecord.setTags(tags);
                gameRecord.setUpdatedAt(java.time.Instant.now());
                log.info("Actualizando juego existente appId={}", juegoDto.getAppId());
            } else {
                // Crear nuevo
                gameRecord = GameRecord.builder()
                        .appId(juegoDto.getAppId())
                        .name(juegoDto.getName())
                        .headerImage(juegoDto.getHeaderImage())
                        .imgVertical(juegoDto.getImgVertical())
                        .imgIconUrl(juegoDto.getImgIconUrl())
                        .isFree(juegoDto.getIsFree())
                        .price(juegoDto.getPrice())
                        .tags(tags)
                        .createdAt(java.time.Instant.now())
                        .updatedAt(java.time.Instant.now())
                        .build();
                log.info("Creando nuevo juego appId={}", juegoDto.getAppId());
            }

            gameRecord = gameRecordRepository.save(gameRecord);
            log.info("Juego guardado exitosamente appId={}", gameRecord.getAppId());

            // Convertir tags de vuelta para la respuesta
            java.util.List<com.dacs.backend.dto.Tags> tagDtos = new java.util.ArrayList<>();
            if (gameRecord.getTags() != null) {
                for (String t : gameRecord.getTags()) {
                    tagDtos.add(com.dacs.backend.dto.Tags.builder().tag(t).build());
                }
            }

            BibliotecaJuegoDto responseDto = BibliotecaJuegoDto.builder()
                    .appId(gameRecord.getAppId())
                    .name(gameRecord.getName())
                    .headerImage(gameRecord.getHeaderImage())
                    .imgVertical(gameRecord.getImgVertical())
                    .imgIconUrl(gameRecord.getImgIconUrl())
                    .isFree(gameRecord.getIsFree())
                    .price(gameRecord.getPrice())
                    .storeUrl("https://store.steampowered.com/app/" + gameRecord.getAppId())
                    .tags(tagDtos)
                    .build();

            return new ResponseEntity<>(responseDto, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error al guardar juego: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
