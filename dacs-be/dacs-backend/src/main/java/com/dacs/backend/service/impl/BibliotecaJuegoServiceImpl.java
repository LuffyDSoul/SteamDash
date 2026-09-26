package com.dacs.backend.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.dacs.backend.dto.BibliotecaJuegoDto;
import com.dacs.backend.entity.GameRecord;
import com.dacs.backend.repository.GameRecordRepository;
import com.dacs.backend.service.BibliotecaJuegoService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BibliotecaJuegoServiceImpl implements BibliotecaJuegoService {

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Autowired
    private GameRecordServiceImpl gameRecordService;

    @Value("${comparison.price.free-format:Gratuito}")
    private String freePriceFormat;

    @Value("${comparison.price.missing-format:N/A}")
    private String missingPriceFormat;

    /**
     * Verifica si un juego debe ser excluido basándose en su nombre
     * @param gameName Nombre del juego
     * @return true si el juego debe ser excluido, false si debe mostrarse
     */
    private boolean shouldExcludeGame(String gameName) {
        if (gameName == null || gameName.trim().isEmpty()) {
            return false;
        }
        
        String lowerName = gameName.toLowerCase();
        
        // Whitelist: Juegos que NUNCA deben ser excluidos
        if (lowerName.equals("rising storm/red orchestra 2 multiplayer")) {
            return false;
        }
        
        // Filtrar juegos que contienen estas palabras (case-insensitive)
        if (lowerName.contains("public") ||
            lowerName.contains("publictest") ||
            lowerName.contains("private") ||
            lowerName.contains("server") ||
            lowerName.contains("beta test") ||
            lowerName.contains("public beta") ||
            lowerName.contains("playtest") ||
            lowerName.contains("unstable") ||
            lowerName.contains("staging branch")) {
            return true;
        }
        
        // Filtrar "Multiplayer", "Multi-Player" o "Multi Player" como palabra completa
        if (gameName.matches("(?i).*\\bmultiplayer\\b.*") ||
            gameName.matches("(?i).*\\bmulti-player\\b.*") ||
            gameName.matches("(?i).*\\bmulti\\s+player\\b.*")) {
            return true;
        }
        
        // "Test" debe ser palabra completa (usando regex con word boundaries)
        if (gameName.matches("(?i).*\\btest\\b.*")) {
            return true;
        }
        
        return false;
    }

    @Override
    public BibliotecaJuegoDto obtenerJuego(Long appId) {
        if (appId == null) throw new IllegalArgumentException("appId es requerido");

        // Siempre refrescar y hacer upsert; si ya estaba, se actualiza; si no, se crea
        GameRecord refreshed = null;
        try {
            refreshed = gameRecordService.refreshAndUpsert(appId, null, null);
        } catch (Exception e) {
            log.warn("Fallo al refrescar juego {}: {}", appId, e.getMessage());
        }

        if (refreshed != null) {
            // FILTRO: Excluir juegos de test/beta/privados/servers
            if (shouldExcludeGame(refreshed.getName())) {
                log.debug("Juego {} excluido por filtro de nombre: {}", appId, refreshed.getName());
                return null;
            }
            return mapToDto(refreshed);
        }

        // Fallback: leer lo que haya en DB
        Optional<GameRecord> existing = gameRecordRepository.findByAppId(appId);
        if (existing.isPresent()) {
            // FILTRO: Excluir juegos de test/beta/privados/servers
            if (shouldExcludeGame(existing.get().getName())) {
                log.debug("Juego {} excluido por filtro de nombre: {}", appId, existing.get().getName());
                return null;
            }
            return mapToDto(existing.get());
        }

        // Si no hay nada, devolver DTO mínimo 
    return BibliotecaJuegoDto.builder()
                .appId(appId)
                .name(null)
                .headerImage(null)
                .imgVertical("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg")
                .imgIconUrl(null)
                .isFree(null)
        .price(missingPriceFormat)
                .storeUrl("https://store.steampowered.com/app/" + appId)
                .tags(new java.util.ArrayList<>())
                .build();
    }
    
    @Override
    public List<Long> buscarAppIdsPorTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // Normalizar tags a lowercase para búsqueda case-insensitive
        List<String> normalizedTags = tags.stream()
            .map(String::toLowerCase)
            .map(String::trim)
            .collect(Collectors.toList());
        
        log.debug("Buscando appIds con tags: {}", normalizedTags);
        List<Long> appIds = gameRecordRepository.findAppIdsByTagsIn(normalizedTags);
        log.debug("Encontrados {} juegos con las tags especificadas", appIds.size());
        
        return appIds;
    }

    private BibliotecaJuegoDto mapToDto(GameRecord r) {
        List<com.dacs.backend.dto.Tags> tagDtos = new java.util.ArrayList<>();
        if (r.getTags() != null) {
            for (String t : r.getTags()) {
                tagDtos.add(com.dacs.backend.dto.Tags.builder().tag(t).build());
            }
        }
        return BibliotecaJuegoDto.builder()
            .appId(r.getAppId())
            .name(r.getName())
            .headerImage(r.getHeaderImage())
            .imgVertical(r.getImgVertical())
            .imgIconUrl(r.getImgIconUrl())
            .isFree(r.getIsFree())
            .price(r.getPrice())
            .storeUrl("https://store.steampowered.com/app/" + r.getAppId())
            .tags(tagDtos)
            .build();
    }
}
