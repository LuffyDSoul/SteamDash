package com.dacs.conector.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dacs.conector.dto.steamDTO.*;

@FeignClient(
		name = "steamWebApiClient", 
		url = "${steam.api.web-url}"
		)
public interface SteamWebApiClient {

    // ISteamNews/GetNewsForApp - Noticias de un juego
    @GetMapping("/ISteamNews/GetNewsForApp/v2/")
    SteamNewsResponseDto getNewsForApp(
        @RequestParam("appid") String appId,
        @RequestParam(value = "count", defaultValue = "10") Integer count,
        @RequestParam(value = "maxlength", defaultValue = "300") Integer maxLength
    );

    // IPlayerService/GetOwnedGames - Juegos de un usuario
    @GetMapping("/IPlayerService/GetOwnedGames/v1/")
    SteamOwnedGamesResponseDto getOwnedGames(
        @RequestParam("key") String apiKey,
        @RequestParam("steamid") String steamId,
        @RequestParam(value = "format", defaultValue = "json") String format,
        @RequestParam(value = "include_appinfo", defaultValue = "true") Boolean includeAppInfo,
        @RequestParam(value = "include_played_free_games", defaultValue = "true") Boolean includePlayedFreeGames
    );

    // ISteamApps/GetAppList - Lista de todas las aplicaciones
    @GetMapping("/ISteamApps/GetAppList/v2/")
    SteamAppListResponseDto getAppList();

    // ISteamUserStats/GetUserStatsForGame - Estadísticas de usuario para un juego
    @GetMapping("/ISteamUserStats/GetUserStatsForGame/v2/")
    SteamUserStatsResponseDto getUserStatsForGame(
        @RequestParam("key") String apiKey,
        @RequestParam("steamid") String steamId,
        @RequestParam("appid") String appId
    );

    // ISteamUser/GetPlayerSummaries - Información pública de usuarios
    @GetMapping("/ISteamUser/GetPlayerSummaries/v2/")
    SteamPlayerSummariesResponseDto getPlayerSummaries(
        @RequestParam("key") String apiKey,
        @RequestParam("steamids") String steamIds
    );

    // ISteamChartsService/GetMostPlayedGames - Juegos más jugados
    @GetMapping("/ISteamChartsService/GetMostPlayedGames/v1/")
    SteamMostPlayedGamesResponseDto getMostPlayedGames(
        @RequestParam("key") String apiKey
    );

    // ISteamUserStats/GetPlayerAchievements - Logros de un jugador
    @GetMapping("/ISteamUserStats/GetPlayerAchievements/v1/")
    SteamPlayerAchievementsResponseDto getPlayerAchievements(
        @RequestParam("key") String apiKey,
        @RequestParam("steamid") String steamId,
        @RequestParam("appid") String appId,
        @RequestParam(value = "l", defaultValue = "english") String language
    );

    // ISteamUserStats/GetSchemaForGame - Esquema de logros y stats de un juego
    @GetMapping("/ISteamUserStats/GetSchemaForGame/v2/")
    SteamGameSchemaResponseDto getSchemaForGame(
        @RequestParam("key") String apiKey,
        @RequestParam("appid") String appId,
        @RequestParam(value = "l", defaultValue = "english") String language
    );

    // IPlayerService/GetRecentlyPlayedGames - Juegos jugados recientemente (últimas 2 semanas)
    @GetMapping("/IPlayerService/GetRecentlyPlayedGames/v1/")
    SteamRecentlyPlayedGamesResponseDto getRecentlyPlayedGames(
        @RequestParam("key") String apiKey,
        @RequestParam("steamid") String steamId
    );

    // ISteamUser/ResolveVanityURL - Resolver vanity URL a steamid64
    @GetMapping("/ISteamUser/ResolveVanityURL/v1/")
    com.dacs.conector.dto.steamDTO.SteamResolveVanityResponseDto resolveVanityUrl(
        @RequestParam("key") String apiKey,
        @RequestParam("vanityurl") String vanityUrl
    );
}