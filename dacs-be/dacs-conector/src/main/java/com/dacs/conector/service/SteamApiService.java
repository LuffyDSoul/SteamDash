package com.dacs.conector.service;

import com.dacs.conector.dto.steamDTO.SteamAppListResponseDto;
import com.dacs.conector.dto.steamDTO.SteamGameDto;
import com.dacs.conector.dto.steamDTO.SteamGameSchemaResponseDto;
import com.dacs.conector.dto.steamDTO.SteamMostPlayedGamesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamPlayerSummariesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamNewsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamOwnedGamesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamPlayerAchievementsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamUserStatsResponseDto;

public interface SteamApiService {
    
    // Detalles de un juego específico
    SteamGameDto getGameDetails(String appId);
    
    // Noticias de un juego
    SteamNewsResponseDto getNewsForApp(String appId, Integer count, Integer maxLength);
    
    // Juegos de un usuario
    SteamOwnedGamesResponseDto getOwnedGames(String steamId, Boolean includeAppInfo, Boolean includePlayedFreeGames);
    
    // Lista de todas las aplicaciones de Steam
    SteamAppListResponseDto getAllApps();
    
    // Estadísticas de usuario para un juego
    SteamUserStatsResponseDto getUserStatsForGame(String steamId, String appId);
    
    // Juegos más jugados
    SteamMostPlayedGamesResponseDto getMostPlayedGames();

    // Obtener información pública de jugadores (personaname, avatar)
    SteamPlayerSummariesResponseDto getPlayerSummaries(String steamIds);
    
    // Logros de un jugador para un juego específico
    SteamPlayerAchievementsResponseDto getPlayerAchievements(String steamId, String appId, String language);
    
    // Esquema de logros y estadísticas de un juego
    SteamGameSchemaResponseDto getSchemaForGame(String appId, String language);

    // Obtener detalles de app desde SteamSpy (appdetails) - devuelve raw JSON como Map
    java.util.Map<String, Object> getSteamSpyAppDetails(String appId);

    // Juegos jugados recientemente (últimas 2 semanas)
    com.dacs.conector.dto.steamDTO.SteamRecentlyPlayedGamesResponseDto getRecentlyPlayedGames(String steamId);

    // Resolver vanity URL a steamid64
    com.dacs.conector.dto.steamDTO.SteamResolveVanityResponseDto resolveVanityUrl(String vanityUrl);
}