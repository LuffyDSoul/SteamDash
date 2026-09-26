package com.dacs.conector.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dacs.conector.api.client.SteamApiClient;
import com.dacs.conector.api.client.SteamWebApiClient;
import com.dacs.conector.config.SteamApiConfig;
import com.dacs.conector.dto.steamDTO.SteamAppDetailsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamAppListResponseDto;
import com.dacs.conector.dto.steamDTO.SteamGameDto;
import com.dacs.conector.dto.steamDTO.SteamGameSchemaResponseDto;
import com.dacs.conector.dto.steamDTO.SteamMostPlayedGamesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamNewsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamOwnedGamesResponseDto;
import com.dacs.conector.dto.steamDTO.SteamPlayerAchievementsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamUserStatsResponseDto;
import com.dacs.conector.dto.steamDTO.SteamPlayerSummariesResponseDto;

@Service
public class SteamApiServiceImpl implements SteamApiService {

    @Autowired
    private SteamApiClient steamApiClient;
    
    @Autowired
    private SteamWebApiClient steamWebApiClient;
    
    @Autowired
    private SteamApiConfig steamApiConfig;

    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    @Override
    public SteamGameDto getGameDetails(String appId) {
        try {
            // Forzar moneda ARS usando cc=ar para obtener precios en Argentina pero en USD
            Map<String, SteamAppDetailsResponseDto> response = steamApiClient.getAppDetails(appId, "ar");
            
            if (response != null && response.containsKey(appId)) {
                SteamAppDetailsResponseDto appDetailsResponse = response.get(appId);
                if (appDetailsResponse != null && appDetailsResponse.getSuccess() != null 
                    && appDetailsResponse.getSuccess()) {
                    return appDetailsResponse.getData();
                }
            }
            
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener detalles del juego de Steam: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamNewsResponseDto getNewsForApp(String appId, Integer count, Integer maxLength) {
        try {
            return steamWebApiClient.getNewsForApp(appId, count, maxLength);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener noticias del juego: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamOwnedGamesResponseDto getOwnedGames(String steamId, Boolean includeAppInfo, Boolean includePlayedFreeGames) {
        try {
            return steamWebApiClient.getOwnedGames(
                steamApiConfig.getKey(), 
                steamId, 
                "json", 
                includeAppInfo, 
                includePlayedFreeGames
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener juegos del usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamAppListResponseDto getAllApps() {
        try {
            return steamWebApiClient.getAppList();
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener lista de aplicaciones de Steam: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamUserStatsResponseDto getUserStatsForGame(String steamId, String appId) {
        try {
            return steamWebApiClient.getUserStatsForGame(steamApiConfig.getKey(), steamId, appId);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener estadísticas del usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamMostPlayedGamesResponseDto getMostPlayedGames() {
        try {
            return steamWebApiClient.getMostPlayedGames(steamApiConfig.getKey());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener juegos más jugados: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamPlayerSummariesResponseDto getPlayerSummaries(String steamIds) {
        try {
            return steamWebApiClient.getPlayerSummaries(steamApiConfig.getKey(), steamIds);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener player summaries: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamPlayerAchievementsResponseDto getPlayerAchievements(String steamId, String appId, String language) {
        try {
            return steamWebApiClient.getPlayerAchievements(steamApiConfig.getKey(), steamId, appId, language);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener logros del jugador: " + e.getMessage(), e);
        }
    }

    @Override
    public SteamGameSchemaResponseDto getSchemaForGame(String appId, String language) {
        try {
            return steamWebApiClient.getSchemaForGame(steamApiConfig.getKey(), appId, language);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener esquema del juego: " + e.getMessage(), e);
        }
    }

    @Override
    public java.util.Map<String, Object> getSteamSpyAppDetails(String appId) {
        try {
            String url = "https://steamspy.com/api.php?request=appdetails&appid=" + appId;
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resp = restTemplate.getForObject(url, java.util.Map.class);
            return resp;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener appdetails de SteamSpy: " + e.getMessage(), e);
        }
    }

    @Override
    public com.dacs.conector.dto.steamDTO.SteamRecentlyPlayedGamesResponseDto getRecentlyPlayedGames(String steamId) {
        try {
            return steamWebApiClient.getRecentlyPlayedGames(steamApiConfig.getKey(), steamId);
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener juegos jugados recientemente: " + e.getMessage(), e);
        }
    }

    @Override
    public com.dacs.conector.dto.steamDTO.SteamResolveVanityResponseDto resolveVanityUrl(String vanityUrl) {
        try {
            return steamWebApiClient.resolveVanityUrl(steamApiConfig.getKey(), vanityUrl);
        } catch (Exception e) {
            throw new RuntimeException("Error al resolver vanity URL: " + e.getMessage(), e);
        }
    }
}