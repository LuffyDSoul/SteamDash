package com.dacs.conector.service;

import com.dacs.conector.api.client.SteamApiClient;
import com.dacs.conector.api.client.SteamWebApiClient;
import com.dacs.conector.config.SteamApiConfig;
import com.dacs.conector.dto.steamDTO.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SteamApiServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class SteamApiServiceImplTest {

    @Mock
    private SteamApiClient steamApiClient;

    @Mock
    private SteamWebApiClient steamWebApiClient;

    @Mock
    private SteamApiConfig steamApiConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SteamApiServiceImpl steamApiService;

    private static final String TEST_APP_ID = "730";
    private static final String TEST_STEAM_ID = "76561198000000000";
    private static final String TEST_API_KEY = "TEST_API_KEY";

    @BeforeEach
    void setUp() {
        // Use lenient() to avoid UnnecessaryStubbingException for tests that don't use API key
        lenient().when(steamApiConfig.getKey()).thenReturn(TEST_API_KEY);
    }

    @Test
    void testGetGameDetails_Success() {
        // Arrange
        SteamGameDto gameDto = new SteamGameDto();
        gameDto.setName("Counter-Strike 2");
        gameDto.setSteamAppId(730L);
        gameDto.setIsFree(true);

        SteamAppDetailsResponseDto responseDto = new SteamAppDetailsResponseDto();
        responseDto.setSuccess(true);
        responseDto.setData(gameDto);

        Map<String, SteamAppDetailsResponseDto> responseMap = new HashMap<>();
        responseMap.put(TEST_APP_ID, responseDto);

        when(steamApiClient.getAppDetails(eq(TEST_APP_ID), eq("ar"))).thenReturn(responseMap);

        // Act
        SteamGameDto result = steamApiService.getGameDetails(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals("Counter-Strike 2", result.getName());
        assertEquals(730L, result.getSteamAppId());
        assertTrue(result.getIsFree());
        verify(steamApiClient, times(1)).getAppDetails(eq(TEST_APP_ID), eq("ar"));
    }

    @Test
    void testGetGameDetails_NullResponse() {
        // Arrange
        when(steamApiClient.getAppDetails(eq(TEST_APP_ID), eq("ar"))).thenReturn(null);

        // Act
        SteamGameDto result = steamApiService.getGameDetails(TEST_APP_ID);

        // Assert
        assertNull(result);
        verify(steamApiClient, times(1)).getAppDetails(eq(TEST_APP_ID), eq("ar"));
    }

    @Test
    void testGetGameDetails_SuccessFalse() {
        // Arrange
        SteamAppDetailsResponseDto responseDto = new SteamAppDetailsResponseDto();
        responseDto.setSuccess(false);

        Map<String, SteamAppDetailsResponseDto> responseMap = new HashMap<>();
        responseMap.put(TEST_APP_ID, responseDto);

        when(steamApiClient.getAppDetails(eq(TEST_APP_ID), eq("ar"))).thenReturn(responseMap);

        // Act
        SteamGameDto result = steamApiService.getGameDetails(TEST_APP_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetGameDetails_ExceptionThrown() {
        // Arrange
        when(steamApiClient.getAppDetails(eq(TEST_APP_ID), eq("ar")))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getGameDetails(TEST_APP_ID);
        });

        assertTrue(exception.getMessage().contains("Error al obtener detalles del juego de Steam"));
    }

    @Test
    void testGetNewsForApp_Success() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        appNews.setAppId(730L);
        appNews.setCount(5);
        newsResponse.setAppNews(appNews);

        when(steamWebApiClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        SteamNewsResponseDto result = steamApiService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAppNews());
        assertEquals(730L, result.getAppNews().getAppId());
        assertEquals(5, result.getAppNews().getCount());
        verify(steamWebApiClient, times(1)).getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300));
    }

    @Test
    void testGetNewsForApp_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getNewsForApp(TEST_APP_ID, 10, 300);
        });

        assertTrue(exception.getMessage().contains("Error al obtener noticias del juego"));
    }

    @Test
    void testGetOwnedGames_Success() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGameCount(150);
        ownedGamesResponse.setResponse(response);

        when(steamWebApiClient.getOwnedGames(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq("json"),
                eq(true),
                eq(true)
        )).thenReturn(ownedGamesResponse);

        // Act
        SteamOwnedGamesResponseDto result = steamApiService.getOwnedGames(
                TEST_STEAM_ID, true, true
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(150, result.getResponse().getGameCount());
        verify(steamWebApiClient, times(1)).getOwnedGames(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq("json"),
                eq(true),
                eq(true)
        );
    }

    @Test
    void testGetOwnedGames_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getOwnedGames(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq("json"),
                eq(true),
                eq(true)
        )).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getOwnedGames(TEST_STEAM_ID, true, true);
        });

        assertTrue(exception.getMessage().contains("Error al obtener juegos del usuario"));
    }

    @Test
    void testGetAllApps_Success() {
        // Arrange
        SteamAppListResponseDto appListResponse = new SteamAppListResponseDto();
        SteamAppListResponseDto.AppListDto wrapper = new SteamAppListResponseDto.AppListDto();
        appListResponse.setAppList(wrapper);

        when(steamWebApiClient.getAppList()).thenReturn(appListResponse);

        // Act
        SteamAppListResponseDto result = steamApiService.getAllApps();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAppList());
        verify(steamWebApiClient, times(1)).getAppList();
    }

    @Test
    void testGetAllApps_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getAppList()).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getAllApps();
        });

        assertTrue(exception.getMessage().contains("Error al obtener lista de aplicaciones de Steam"));
    }

    @Test
    void testGetUserStatsForGame_Success() {
        // Arrange
        SteamUserStatsResponseDto statsResponse = new SteamUserStatsResponseDto();
        SteamUserStatsResponseDto.PlayerStatsDto playerStats = new SteamUserStatsResponseDto.PlayerStatsDto();
        playerStats.setSteamId(TEST_STEAM_ID);
        playerStats.setGameName("Counter-Strike 2");
        statsResponse.setPlayerStats(playerStats);

        when(steamWebApiClient.getUserStatsForGame(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID)
        )).thenReturn(statsResponse);

        // Act
        SteamUserStatsResponseDto result = steamApiService.getUserStatsForGame(
                TEST_STEAM_ID, TEST_APP_ID
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPlayerStats());
        assertEquals(TEST_STEAM_ID, result.getPlayerStats().getSteamId());
        assertEquals("Counter-Strike 2", result.getPlayerStats().getGameName());
        verify(steamWebApiClient, times(1)).getUserStatsForGame(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID)
        );
    }

    @Test
    void testGetUserStatsForGame_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getUserStatsForGame(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID)
        )).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getUserStatsForGame(TEST_STEAM_ID, TEST_APP_ID);
        });

        assertTrue(exception.getMessage().contains("Error al obtener estadísticas del usuario"));
    }

    @Test
    void testGetMostPlayedGames_Success() {
        // Arrange
        SteamMostPlayedGamesResponseDto mostPlayedResponse = new SteamMostPlayedGamesResponseDto();
        SteamMostPlayedGamesResponseDto.MostPlayedResponseDto response = new SteamMostPlayedGamesResponseDto.MostPlayedResponseDto();
        mostPlayedResponse.setResponse(response);

        when(steamWebApiClient.getMostPlayedGames(eq(TEST_API_KEY))).thenReturn(mostPlayedResponse);

        // Act
        SteamMostPlayedGamesResponseDto result = steamApiService.getMostPlayedGames();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        verify(steamWebApiClient, times(1)).getMostPlayedGames(eq(TEST_API_KEY));
    }

    @Test
    void testGetMostPlayedGames_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getMostPlayedGames(eq(TEST_API_KEY)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getMostPlayedGames();
        });

        assertTrue(exception.getMessage().contains("Error al obtener juegos más jugados"));
    }

    @Test
    void testGetPlayerSummaries_Success() {
        // Arrange
        SteamPlayerSummariesResponseDto summariesResponse = new SteamPlayerSummariesResponseDto();
        SteamPlayerSummariesResponseDto.ResponseDto response = new SteamPlayerSummariesResponseDto.ResponseDto();
        summariesResponse.setResponse(response);

        when(steamWebApiClient.getPlayerSummaries(eq(TEST_API_KEY), eq(TEST_STEAM_ID)))
                .thenReturn(summariesResponse);

        // Act
        SteamPlayerSummariesResponseDto result = steamApiService.getPlayerSummaries(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        verify(steamWebApiClient, times(1)).getPlayerSummaries(eq(TEST_API_KEY), eq(TEST_STEAM_ID));
    }

    @Test
    void testGetPlayerSummaries_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getPlayerSummaries(eq(TEST_API_KEY), eq(TEST_STEAM_ID)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getPlayerSummaries(TEST_STEAM_ID);
        });

        assertTrue(exception.getMessage().contains("Error al obtener player summaries"));
    }

    @Test
    void testGetPlayerAchievements_Success() {
        // Arrange
        String language = "english";
        SteamPlayerAchievementsResponseDto achievementsResponse = new SteamPlayerAchievementsResponseDto();
        SteamPlayerAchievementsResponseDto.PlayerAchievementsDto playerStats = new SteamPlayerAchievementsResponseDto.PlayerAchievementsDto();
        playerStats.setSteamId(TEST_STEAM_ID);
        playerStats.setGameName("Counter-Strike 2");
        achievementsResponse.setPlayerStats(playerStats);

        when(steamWebApiClient.getPlayerAchievements(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID),
                eq(language)
        )).thenReturn(achievementsResponse);

        // Act
        SteamPlayerAchievementsResponseDto result = steamApiService.getPlayerAchievements(
                TEST_STEAM_ID, TEST_APP_ID, language
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getPlayerStats());
        assertEquals(TEST_STEAM_ID, result.getPlayerStats().getSteamId());
        assertEquals("Counter-Strike 2", result.getPlayerStats().getGameName());
        verify(steamWebApiClient, times(1)).getPlayerAchievements(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID),
                eq(language)
        );
    }

    @Test
    void testGetPlayerAchievements_ExceptionThrown() {
        // Arrange
        String language = "english";
        when(steamWebApiClient.getPlayerAchievements(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID),
                eq(language)
        )).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, language);
        });

        assertTrue(exception.getMessage().contains("Error al obtener logros del jugador"));
    }

    @Test
    void testGetSchemaForGame_Success() {
        // Arrange
        String language = "english";
        SteamGameSchemaResponseDto schemaResponse = new SteamGameSchemaResponseDto();
        SteamGameSchemaResponseDto.GameSchemaDto game = new SteamGameSchemaResponseDto.GameSchemaDto();
        game.setGameName("Counter-Strike 2");
        game.setGameVersion("1");
        schemaResponse.setGame(game);

        when(steamWebApiClient.getSchemaForGame(
                eq(TEST_API_KEY),
                eq(TEST_APP_ID),
                eq(language)
        )).thenReturn(schemaResponse);

        // Act
        SteamGameSchemaResponseDto result = steamApiService.getSchemaForGame(
                TEST_APP_ID, language
        );

        // Assert
        assertNotNull(result);
        assertNotNull(result.getGame());
        assertEquals("Counter-Strike 2", result.getGame().getGameName());
        assertEquals("1", result.getGame().getGameVersion());
        verify(steamWebApiClient, times(1)).getSchemaForGame(
                eq(TEST_API_KEY),
                eq(TEST_APP_ID),
                eq(language)
        );
    }

    @Test
    void testGetSchemaForGame_ExceptionThrown() {
        // Arrange
        String language = "english";
        when(steamWebApiClient.getSchemaForGame(
                eq(TEST_API_KEY),
                eq(TEST_APP_ID),
                eq(language)
        )).thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getSchemaForGame(TEST_APP_ID, language);
        });

        assertTrue(exception.getMessage().contains("Error al obtener esquema del juego"));
    }

    @Test
    void testGetSteamSpyAppDetails_Success() {
        // Arrange
        Map<String, Object> steamSpyData = new HashMap<>();
        steamSpyData.put("appid", "730");
        steamSpyData.put("name", "Counter-Strike 2");
        steamSpyData.put("owners", "100,000,000 .. 200,000,000");

        String expectedUrl = "https://steamspy.com/api.php?request=appdetails&appid=" + TEST_APP_ID;

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class)))
                .thenReturn(steamSpyData);

        // Act
        Map<String, Object> result = steamApiService.getSteamSpyAppDetails(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals("730", result.get("appid"));
        assertEquals("Counter-Strike 2", result.get("name"));
        assertEquals("100,000,000 .. 200,000,000", result.get("owners"));
        verify(restTemplate, times(1)).getForObject(eq(expectedUrl), eq(Map.class));
    }

    @Test
    void testGetSteamSpyAppDetails_ExceptionThrown() {
        // Arrange
        String expectedUrl = "https://steamspy.com/api.php?request=appdetails&appid=" + TEST_APP_ID;

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class)))
                .thenThrow(new RuntimeException("SteamSpy API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getSteamSpyAppDetails(TEST_APP_ID);
        });

        assertTrue(exception.getMessage().contains("Error al obtener appdetails de SteamSpy"));
    }

    @Test
    void testGetRecentlyPlayedGames_Success() {
        // Arrange
        SteamRecentlyPlayedGamesResponseDto recentlyPlayed = new SteamRecentlyPlayedGamesResponseDto();
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse response = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse();
        response.setTotalCount(5);
        recentlyPlayed.setResponse(response);

        when(steamWebApiClient.getRecentlyPlayedGames(eq(TEST_API_KEY), eq(TEST_STEAM_ID)))
                .thenReturn(recentlyPlayed);

        // Act
        SteamRecentlyPlayedGamesResponseDto result = steamApiService.getRecentlyPlayedGames(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(5, result.getResponse().getTotalCount());
        verify(steamWebApiClient, times(1)).getRecentlyPlayedGames(eq(TEST_API_KEY), eq(TEST_STEAM_ID));
    }

    @Test
    void testGetRecentlyPlayedGames_ExceptionThrown() {
        // Arrange
        when(steamWebApiClient.getRecentlyPlayedGames(eq(TEST_API_KEY), eq(TEST_STEAM_ID)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.getRecentlyPlayedGames(TEST_STEAM_ID);
        });

        assertTrue(exception.getMessage().contains("Error al obtener juegos jugados recientemente"));
    }

    @Test
    void testResolveVanityUrl_Success() {
        // Arrange
        String vanityUrl = "testuser";
        SteamResolveVanityResponseDto resolveResponse = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse response = new SteamResolveVanityResponseDto.InnerResponse();
        response.setSteamid(TEST_STEAM_ID);
        response.setSuccess(1);
        resolveResponse.setResponse(response);

        when(steamWebApiClient.resolveVanityUrl(eq(TEST_API_KEY), eq(vanityUrl)))
                .thenReturn(resolveResponse);

        // Act
        SteamResolveVanityResponseDto result = steamApiService.resolveVanityUrl(vanityUrl);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(TEST_STEAM_ID, result.getResponse().getSteamid());
        assertEquals(1, result.getResponse().getSuccess());
        verify(steamWebApiClient, times(1)).resolveVanityUrl(eq(TEST_API_KEY), eq(vanityUrl));
    }

    @Test
    void testResolveVanityUrl_NotFound() {
        // Arrange
        String vanityUrl = "nonexistentuser";
        SteamResolveVanityResponseDto resolveResponse = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse response = new SteamResolveVanityResponseDto.InnerResponse();
        response.setSuccess(42); // 42 means not found
        resolveResponse.setResponse(response);

        when(steamWebApiClient.resolveVanityUrl(eq(TEST_API_KEY), eq(vanityUrl)))
                .thenReturn(resolveResponse);

        // Act
        SteamResolveVanityResponseDto result = steamApiService.resolveVanityUrl(vanityUrl);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getResponse());
        assertEquals(42, result.getResponse().getSuccess());
        verify(steamWebApiClient, times(1)).resolveVanityUrl(eq(TEST_API_KEY), eq(vanityUrl));
    }

    @Test
    void testResolveVanityUrl_ExceptionThrown() {
        // Arrange
        String vanityUrl = "testuser";
        when(steamWebApiClient.resolveVanityUrl(eq(TEST_API_KEY), eq(vanityUrl)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            steamApiService.resolveVanityUrl(vanityUrl);
        });

        assertTrue(exception.getMessage().contains("Error al resolver vanity URL"));
    }

    @Test
    void testGetGameDetails_EmptyResponse() {
        // Arrange
        Map<String, SteamAppDetailsResponseDto> responseMap = new HashMap<>();

        when(steamApiClient.getAppDetails(eq(TEST_APP_ID), eq("ar"))).thenReturn(responseMap);

        // Act
        SteamGameDto result = steamApiService.getGameDetails(TEST_APP_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetGameDetails_NullData() {
        // Arrange
        SteamAppDetailsResponseDto responseDto = new SteamAppDetailsResponseDto();
        responseDto.setSuccess(true);
        responseDto.setData(null);

        Map<String, SteamAppDetailsResponseDto> responseMap = new HashMap<>();
        responseMap.put(TEST_APP_ID, responseDto);

        when(steamApiClient.getAppDetails(eq(TEST_APP_ID), eq("ar"))).thenReturn(responseMap);

        // Act
        SteamGameDto result = steamApiService.getGameDetails(TEST_APP_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetOwnedGames_WithIncludeFlags() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGameCount(50);
        ownedGamesResponse.setResponse(response);

        when(steamWebApiClient.getOwnedGames(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq("json"),
                eq(false),
                eq(false)
        )).thenReturn(ownedGamesResponse);

        // Act
        SteamOwnedGamesResponseDto result = steamApiService.getOwnedGames(
                TEST_STEAM_ID, false, false
        );

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getResponse().getGameCount());
        verify(steamWebApiClient, times(1)).getOwnedGames(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq("json"),
                eq(false),
                eq(false)
        );
    }

    @Test
    void testGetPlayerAchievements_DifferentLanguage() {
        // Arrange
        String language = "spanish";
        SteamPlayerAchievementsResponseDto achievementsResponse = new SteamPlayerAchievementsResponseDto();
        SteamPlayerAchievementsResponseDto.PlayerAchievementsDto playerStats = new SteamPlayerAchievementsResponseDto.PlayerAchievementsDto();
        playerStats.setSteamId(TEST_STEAM_ID);
        achievementsResponse.setPlayerStats(playerStats);

        when(steamWebApiClient.getPlayerAchievements(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID),
                eq(language)
        )).thenReturn(achievementsResponse);

        // Act
        SteamPlayerAchievementsResponseDto result = steamApiService.getPlayerAchievements(
                TEST_STEAM_ID, TEST_APP_ID, language
        );

        // Assert
        assertNotNull(result);
        verify(steamWebApiClient, times(1)).getPlayerAchievements(
                eq(TEST_API_KEY),
                eq(TEST_STEAM_ID),
                eq(TEST_APP_ID),
                eq(language)
        );
    }

    @Test
    void testGetSteamSpyAppDetails_EmptyData() {
        // Arrange
        Map<String, Object> steamSpyData = new HashMap<>();
        String expectedUrl = "https://steamspy.com/api.php?request=appdetails&appid=" + TEST_APP_ID;

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class)))
                .thenReturn(steamSpyData);

        // Act
        Map<String, Object> result = steamApiService.getSteamSpyAppDetails(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSteamSpyAppDetails_NullResponse() {
        // Arrange
        String expectedUrl = "https://steamspy.com/api.php?request=appdetails&appid=" + TEST_APP_ID;

        when(restTemplate.getForObject(eq(expectedUrl), eq(Map.class)))
                .thenReturn(null);

        // Act
        Map<String, Object> result = steamApiService.getSteamSpyAppDetails(TEST_APP_ID);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetPlayerSummaries_MultipleIds() {
        // Arrange
        String steamIds = TEST_STEAM_ID + ",76561198000000001";
        SteamPlayerSummariesResponseDto summariesResponse = new SteamPlayerSummariesResponseDto();
        SteamPlayerSummariesResponseDto.ResponseDto response = new SteamPlayerSummariesResponseDto.ResponseDto();
        summariesResponse.setResponse(response);

        when(steamWebApiClient.getPlayerSummaries(eq(TEST_API_KEY), eq(steamIds)))
                .thenReturn(summariesResponse);

        // Act
        SteamPlayerSummariesResponseDto result = steamApiService.getPlayerSummaries(steamIds);

        // Assert
        assertNotNull(result);
        verify(steamWebApiClient, times(1)).getPlayerSummaries(eq(TEST_API_KEY), eq(steamIds));
    }

    @Test
    void testGetSchemaForGame_DifferentLanguage() {
        // Arrange
        String language = "french";
        SteamGameSchemaResponseDto schemaResponse = new SteamGameSchemaResponseDto();
        SteamGameSchemaResponseDto.GameSchemaDto game = new SteamGameSchemaResponseDto.GameSchemaDto();
        game.setGameName("Counter-Strike 2");
        schemaResponse.setGame(game);

        when(steamWebApiClient.getSchemaForGame(
                eq(TEST_API_KEY),
                eq(TEST_APP_ID),
                eq(language)
        )).thenReturn(schemaResponse);

        // Act
        SteamGameSchemaResponseDto result = steamApiService.getSchemaForGame(TEST_APP_ID, language);

        // Assert
        assertNotNull(result);
        assertEquals("Counter-Strike 2", result.getGame().getGameName());
        verify(steamWebApiClient, times(1)).getSchemaForGame(
                eq(TEST_API_KEY),
                eq(TEST_APP_ID),
                eq(language)
        );
    }
}
