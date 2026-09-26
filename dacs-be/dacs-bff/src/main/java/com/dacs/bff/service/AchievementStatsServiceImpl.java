package com.dacs.bff.service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.client.BackendClient;
import com.dacs.bff.dto.AchievementStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de estadísticas de logros
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementStatsServiceImpl implements AchievementStatsService {
    
    private final BackendClient backendClient;
    private final ApiConectorClient apiConectorClient;
    
    @Override
    @SuppressWarnings("unchecked")
    public AchievementStatsDto getAchievementStatsEnriched(String steamId) {
        log.info("BFF - Obteniendo estadísticas de logros enriquecidas para usuario {}", steamId);
        
        // 1. Obtener biblioteca del usuario para sacar los juegos más jugados
        com.dacs.bff.dto.SteamOwnedGamesResponseDto ownedGames = apiConectorClient.getUserOwnedGames(steamId, true, true);
        
        if (ownedGames == null || ownedGames.getResponse() == null || ownedGames.getResponse().getGames() == null) {
            log.warn("No se pudo obtener biblioteca del usuario {}", steamId);
            return AchievementStatsDto.builder()
                    .steamId(steamId)
                    .totalJuegosConLogros(0)
                    .totalLogrosDesbloqueados(0)
                    .totalLogrosDisponibles(0)
                    .porcentajeGlobal(0.0)
                    .juegosCompletos100(Collections.emptyList())
                    .juegosCercanos100(Collections.emptyList())
                    .juegosMasProgreso(Collections.emptyList())
                    .build();
        }
        
        List<com.dacs.bff.dto.SteamOwnedGamesResponseDto.OwnedGameDto> games = ownedGames.getResponse().getGames();
        
        if (games.isEmpty()) {
            log.warn("Respuesta de biblioteca vacía para usuario {}", steamId);
            return AchievementStatsDto.builder()
                    .steamId(steamId)
                    .totalJuegosConLogros(0)
                    .totalLogrosDesbloqueados(0)
                    .totalLogrosDisponibles(0)
                    .porcentajeGlobal(0.0)
                    .juegosCompletos100(Collections.emptyList())
                    .juegosCercanos100(Collections.emptyList())
                    .juegosMasProgreso(Collections.emptyList())
                    .build();
        }
        
        // 2. Crear un mapa de juegos por appId para enriquecimiento
        Map<Long, com.dacs.bff.dto.SteamOwnedGamesResponseDto.OwnedGameDto> gamesById = games.stream()
                .collect(Collectors.toMap(
                        g -> g.getAppId(),
                        g -> g,
                        (a, b) -> a
                ));
        
        // 3. Obtener top juegos por tiempo jugado (para priorizar en estadísticas)
        List<Long> topGamesByPlaytime = games.stream()
                .sorted((a, b) -> {
                    int playtimeA = a.getPlaytimeForever() != null ? a.getPlaytimeForever() : 0;
                    int playtimeB = b.getPlaytimeForever() != null ? b.getPlaytimeForever() : 0;
                    return Integer.compare(playtimeB, playtimeA);
                })
                .limit(50) // Top 50 juegos más jugados
                .map(g -> g.getAppId())
                .collect(Collectors.toList());
        
        // 4. Llamar al backend para obtener estadísticas
        AchievementStatsDto stats = backendClient.getAchievementStats(steamId, topGamesByPlaytime);
        
        // 5. Enriquecer los juegos con información de la biblioteca
        enrichGameInfo(stats.getJuegosCompletos100(), gamesById);
        enrichGameInfo(stats.getJuegosCercanos100(), gamesById);
        enrichGameInfo(stats.getJuegosMasProgreso(), gamesById);
        
        log.info("BFF - Estadísticas de logros obtenidas: {} juegos con logros, {}/{} logros desbloqueados ({}%)",
                stats.getTotalJuegosConLogros(),
                stats.getTotalLogrosDesbloqueados(),
                stats.getTotalLogrosDisponibles(),
                String.format("%.1f", stats.getPorcentajeGlobal()));
        
        return stats;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public String syncAllAchievements(String steamId) {
        log.info("BFF - Iniciando sincronización de logros para usuario {}", steamId);
        
        // Obtener biblioteca del usuario
        com.dacs.bff.dto.SteamOwnedGamesResponseDto ownedGames = apiConectorClient.getUserOwnedGames(steamId, true, true);
        
        if (ownedGames == null || ownedGames.getResponse() == null || ownedGames.getResponse().getGames() == null) {
            return "Error: No se pudo obtener la biblioteca del usuario";
        }
        
        List<com.dacs.bff.dto.SteamOwnedGamesResponseDto.OwnedGameDto> games = ownedGames.getResponse().getGames();
        
        if (games.isEmpty()) {
            return "Error: La biblioteca está vacía";
        }
        
        // Extraer appIds
        List<Long> appIds = games.stream()
                .map(g -> g.getAppId())
                .collect(Collectors.toList());
        
        log.info("BFF - Sincronizando logros para {} juegos del usuario {}", appIds.size(), steamId);
        
        // Llamar al backend para forzar actualización de TODOS los logros
        backendClient.forceRefreshLogros(steamId, appIds);
        
        return String.format("Sincronización iniciada para %d juegos. Los logros se actualizarán completamente en segundo plano.", appIds.size());
    }
    
    /**
    /**
     * Enriquece la lista de juegos con información de la biblioteca
     */
    private void enrichGameInfo(List<AchievementStatsDto.GameAchievementProgress> gamesList, 
                                Map<Long, com.dacs.bff.dto.SteamOwnedGamesResponseDto.OwnedGameDto> gamesById) {
        if (gamesList == null) return;
        
        for (AchievementStatsDto.GameAchievementProgress game : gamesList) {
            com.dacs.bff.dto.SteamOwnedGamesResponseDto.OwnedGameDto gameInfo = gamesById.get(game.getAppId());
            if (gameInfo != null) {
                game.setGameName(gameInfo.getName());
                
                // Header image desde CDN de Steam
                String headerImage = String.format(
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/%d/header.jpg",
                    game.getAppId()
                );
                game.setHeaderImage(headerImage);
                
                // Playtime en minutos
                Integer playtime = gameInfo.getPlaytimeForever() != null ? gameInfo.getPlaytimeForever() : 0;
                game.setPlaytimeForever(playtime);
            }
        }
    }
}