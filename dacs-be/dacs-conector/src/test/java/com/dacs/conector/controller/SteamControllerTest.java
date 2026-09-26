package com.dacs.conector.controller;

import com.dacs.conector.dto.steamDTO.*;
import com.dacs.conector.service.SteamApiServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SteamController
 */
@ExtendWith(MockitoExtension.class)
class SteamControllerTest {

    @Mock
    private SteamApiServiceImpl steamApiService;

    @InjectMocks
    private SteamController steamController;

    private static final String TEST_APP_ID = "730";
    private static final String TEST_STEAM_ID = "76561198000000000";

    // ==================== GET GAME DETAILS TESTS ====================

    @Test
    void testGetGameDetails_Success() {
        // Arrange
        SteamGameDto gameDto = new SteamGameDto();
        gameDto.setName("Counter-Strike 2");
        gameDto.setSteamAppId(730L);
        gameDto.setIsFree(true);

        when(steamApiService.getGameDetails(TEST_APP_ID)).thenReturn(gameDto);

        // Act
        ResponseEntity<SteamGameDto> response = steamController.getGameDetails(TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Counter-Strike 2", response.getBody().getName());
        assertEquals(730L, response.getBody().getSteamAppId());
        verify(steamApiService, times(1)).getGameDetails(TEST_APP_ID);
    }

    @Test
    void testGetGameDetails_NotFound() {
        // Arrange
        when(steamApiService.getGameDetails(TEST_APP_ID)).thenReturn(null);

        // Act
        ResponseEntity<SteamGameDto> response = steamController.getGameDetails(TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(steamApiService, times(1)).getGameDetails(TEST_APP_ID);
    }

    @Test
    void testGetGameDetails_InternalServerError() {
        // Arrange
        when(steamApiService.getGameDetails(TEST_APP_ID))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamGameDto> response = steamController.getGameDetails(TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        verify(steamApiService, times(1)).getGameDetails(TEST_APP_ID);
    }

    // ==================== GET GAME NEWS TESTS ====================

    @Test
    void testGetGameNews_Success() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        appNews.setAppId(730L);
        appNews.setCount(5);
        newsResponse.setAppNews(appNews);

        when(steamApiService.getNewsForApp(TEST_APP_ID, 10, 300))
                .thenReturn(newsResponse);

        // Act
        ResponseEntity<SteamNewsResponseDto> response = steamController.getGameNews(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(730L, response.getBody().getAppNews().getAppId());
        verify(steamApiService, times(1)).getNewsForApp(TEST_APP_ID, 10, 300);
    }

    @Test
    void testGetGameNews_WithDefaultParams() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        when(steamApiService.getNewsForApp(TEST_APP_ID, 10, 300))
                .thenReturn(newsResponse);

        // Act
        ResponseEntity<SteamNewsResponseDto> response = steamController.getGameNews(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(steamApiService, times(1)).getNewsForApp(TEST_APP_ID, 10, 300);
    }

    @Test
    void testGetGameNews_InternalServerError() {
        // Arrange
        when(steamApiService.getNewsForApp(TEST_APP_ID, 10, 300))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamNewsResponseDto> response = steamController.getGameNews(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET STEAMSPY APP DETAILS TESTS ====================

    @Test
    void testGetSteamSpyAppDetails_Success() {
        // Arrange
        Map<String, Object> spyData = new HashMap<>();
        spyData.put("appid", "730");
        spyData.put("name", "Counter-Strike 2");
        spyData.put("owners", "100,000,000 .. 200,000,000");

        when(steamApiService.getSteamSpyAppDetails(TEST_APP_ID)).thenReturn(spyData);

        // Act
        ResponseEntity<Map<String, Object>> response = steamController.getSteamSpyAppDetails(TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("730", response.getBody().get("appid"));
        verify(steamApiService, times(1)).getSteamSpyAppDetails(TEST_APP_ID);
    }

    @Test
    void testGetSteamSpyAppDetails_InternalServerError() {
        // Arrange
        when(steamApiService.getSteamSpyAppDetails(TEST_APP_ID))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<Map<String, Object>> response = steamController.getSteamSpyAppDetails(TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET USER GAMES TESTS ====================

    @Test
    void testGetUserGames_Success() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGames = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGameCount(150);
        ownedGames.setResponse(response);

        when(steamApiService.getOwnedGames(TEST_STEAM_ID, true, true))
                .thenReturn(ownedGames);

        // Act
        ResponseEntity<SteamOwnedGamesResponseDto> result = steamController.getUserGames(TEST_STEAM_ID, true, true);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(150, result.getBody().getResponse().getGameCount());
        verify(steamApiService, times(1)).getOwnedGames(TEST_STEAM_ID, true, true);
    }

    @Test
    void testGetUserGames_WithDefaultParams() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGames = new SteamOwnedGamesResponseDto();
        when(steamApiService.getOwnedGames(TEST_STEAM_ID, true, true))
                .thenReturn(ownedGames);

        // Act
        ResponseEntity<SteamOwnedGamesResponseDto> result = steamController.getUserGames(TEST_STEAM_ID, true, true);

        // Assert
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(steamApiService, times(1)).getOwnedGames(TEST_STEAM_ID, true, true);
    }

    @Test
    void testGetUserGames_InternalServerError() {
        // Arrange
        when(steamApiService.getOwnedGames(TEST_STEAM_ID, true, true))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamOwnedGamesResponseDto> response = steamController.getUserGames(TEST_STEAM_ID, true, true);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET RECENTLY PLAYED TESTS ====================

    @Test
    void testGetRecentlyPlayed_Success() {
        // Arrange
        SteamRecentlyPlayedGamesResponseDto recentlyPlayed = new SteamRecentlyPlayedGamesResponseDto();
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse responseData = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse();
        responseData.setTotalCount(5);
        recentlyPlayed.setResponse(responseData);

        when(steamApiService.getRecentlyPlayedGames(TEST_STEAM_ID))
                .thenReturn(recentlyPlayed);

        // Act
        ResponseEntity<SteamRecentlyPlayedGamesResponseDto> response = steamController.getRecentlyPlayed(TEST_STEAM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(5, response.getBody().getResponse().getTotalCount());
        verify(steamApiService, times(1)).getRecentlyPlayedGames(TEST_STEAM_ID);
    }

    @Test
    void testGetRecentlyPlayed_InternalServerError() {
        // Arrange
        when(steamApiService.getRecentlyPlayedGames(TEST_STEAM_ID))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamRecentlyPlayedGamesResponseDto> response = steamController.getRecentlyPlayed(TEST_STEAM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET ALL APPS TESTS ====================

    @Test
    void testGetAllApps_Success() {
        // Arrange
        SteamAppListResponseDto appList = new SteamAppListResponseDto();
        SteamAppListResponseDto.AppListDto wrapper = new SteamAppListResponseDto.AppListDto();
        appList.setAppList(wrapper);

        when(steamApiService.getAllApps()).thenReturn(appList);

        // Act
        ResponseEntity<SteamAppListResponseDto> response = steamController.getAllApps();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(steamApiService, times(1)).getAllApps();
    }

    @Test
    void testGetAllApps_InternalServerError() {
        // Arrange
        when(steamApiService.getAllApps()).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamAppListResponseDto> response = steamController.getAllApps();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET USER GAME STATS TESTS ====================

    @Test
    void testGetUserGameStats_Success() {
        // Arrange
        SteamUserStatsResponseDto stats = new SteamUserStatsResponseDto();
        SteamUserStatsResponseDto.PlayerStatsDto playerStats = new SteamUserStatsResponseDto.PlayerStatsDto();
        playerStats.setSteamId(TEST_STEAM_ID);
        playerStats.setGameName("Counter-Strike 2");
        stats.setPlayerStats(playerStats);

        when(steamApiService.getUserStatsForGame(TEST_STEAM_ID, TEST_APP_ID))
                .thenReturn(stats);

        // Act
        ResponseEntity<SteamUserStatsResponseDto> response = steamController.getUserGameStats(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_STEAM_ID, response.getBody().getPlayerStats().getSteamId());
        verify(steamApiService, times(1)).getUserStatsForGame(TEST_STEAM_ID, TEST_APP_ID);
    }

    @Test
    void testGetUserGameStats_InternalServerError() {
        // Arrange
        when(steamApiService.getUserStatsForGame(TEST_STEAM_ID, TEST_APP_ID))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamUserStatsResponseDto> response = steamController.getUserGameStats(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET MOST PLAYED GAMES TESTS ====================

    @Test
    void testGetMostPlayedGames_Success() {
        // Arrange
        SteamMostPlayedGamesResponseDto mostPlayed = new SteamMostPlayedGamesResponseDto();
        SteamMostPlayedGamesResponseDto.MostPlayedResponseDto responseData = new SteamMostPlayedGamesResponseDto.MostPlayedResponseDto();
        mostPlayed.setResponse(responseData);

        when(steamApiService.getMostPlayedGames()).thenReturn(mostPlayed);

        // Act
        ResponseEntity<SteamMostPlayedGamesResponseDto> response = steamController.getMostPlayedGames();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(steamApiService, times(1)).getMostPlayedGames();
    }

    @Test
    void testGetMostPlayedGames_InternalServerError() {
        // Arrange
        when(steamApiService.getMostPlayedGames()).thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamMostPlayedGamesResponseDto> response = steamController.getMostPlayedGames();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET PLAYER ACHIEVEMENTS TESTS ====================

    @Test
    void testGetPlayerAchievements_Success() {
        // Arrange
        SteamPlayerAchievementsResponseDto achievements = new SteamPlayerAchievementsResponseDto();
        SteamPlayerAchievementsResponseDto.PlayerAchievementsDto playerStats = new SteamPlayerAchievementsResponseDto.PlayerAchievementsDto();
        playerStats.setSteamId(TEST_STEAM_ID);
        playerStats.setGameName("Counter-Strike 2");
        achievements.setPlayerStats(playerStats);

        when(steamApiService.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "english"))
                .thenReturn(achievements);

        // Act
        ResponseEntity<SteamPlayerAchievementsResponseDto> response = 
                steamController.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_STEAM_ID, response.getBody().getPlayerStats().getSteamId());
        verify(steamApiService, times(1)).getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "english");
    }

    @Test
    void testGetPlayerAchievements_WithDefaultLanguage() {
        // Arrange
        SteamPlayerAchievementsResponseDto achievements = new SteamPlayerAchievementsResponseDto();
        when(steamApiService.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "english"))
                .thenReturn(achievements);

        // Act
        ResponseEntity<SteamPlayerAchievementsResponseDto> response = 
                steamController.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(steamApiService, times(1)).getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "english");
    }

    @Test
    void testGetPlayerAchievements_InternalServerError() {
        // Arrange
        when(steamApiService.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "spanish"))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamPlayerAchievementsResponseDto> response = 
                steamController.getPlayerAchievements(TEST_STEAM_ID, TEST_APP_ID, "spanish");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET PLAYER SUMMARIES TESTS ====================

    @Test
    void testGetPlayerSummaries_Success() {
        // Arrange
        SteamPlayerSummariesResponseDto summaries = new SteamPlayerSummariesResponseDto();
        SteamPlayerSummariesResponseDto.ResponseDto responseData = new SteamPlayerSummariesResponseDto.ResponseDto();
        List<SteamPlayerSummariesResponseDto.PlayerDto> players = new ArrayList<>();
        SteamPlayerSummariesResponseDto.PlayerDto player = new SteamPlayerSummariesResponseDto.PlayerDto();
        player.setSteamId(TEST_STEAM_ID);
        player.setPersonaName("TestPlayer");
        players.add(player);
        responseData.setPlayers(players);
        summaries.setResponse(responseData);

        when(steamApiService.getPlayerSummaries(TEST_STEAM_ID)).thenReturn(summaries);

        // Act
        ResponseEntity<SteamPlayerSummariesResponseDto> response = steamController.getPlayerSummaries(TEST_STEAM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getResponse().getPlayers().size());
        verify(steamApiService, times(1)).getPlayerSummaries(TEST_STEAM_ID);
    }

    @Test
    void testGetPlayerSummaries_MultipleIds() {
        // Arrange
        String multipleIds = TEST_STEAM_ID + ",76561198000000001";
        SteamPlayerSummariesResponseDto summaries = new SteamPlayerSummariesResponseDto();
        SteamPlayerSummariesResponseDto.ResponseDto responseData = new SteamPlayerSummariesResponseDto.ResponseDto();
        summaries.setResponse(responseData);

        when(steamApiService.getPlayerSummaries(multipleIds)).thenReturn(summaries);

        // Act
        ResponseEntity<SteamPlayerSummariesResponseDto> response = steamController.getPlayerSummaries(multipleIds);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(steamApiService, times(1)).getPlayerSummaries(multipleIds);
    }

    @Test
    void testGetPlayerSummaries_InternalServerError() {
        // Arrange
        when(steamApiService.getPlayerSummaries(TEST_STEAM_ID))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamPlayerSummariesResponseDto> response = steamController.getPlayerSummaries(TEST_STEAM_ID);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // ==================== GET GAME SCHEMA TESTS ====================

    @Test
    void testGetGameSchema_Success() {
        // Arrange
        SteamGameSchemaResponseDto schema = new SteamGameSchemaResponseDto();
        SteamGameSchemaResponseDto.GameSchemaDto game = new SteamGameSchemaResponseDto.GameSchemaDto();
        game.setGameName("Counter-Strike 2");
        game.setGameVersion("1");
        
        SteamGameSchemaResponseDto.AvailableGameStatsDto availableStats = new SteamGameSchemaResponseDto.AvailableGameStatsDto();
        List<SteamGameSchemaResponseDto.SchemaAchievementDto> achievements = new ArrayList<>();
        SteamGameSchemaResponseDto.SchemaAchievementDto achievement = new SteamGameSchemaResponseDto.SchemaAchievementDto();
        achievement.setName("ACH_1");
        achievements.add(achievement);
        availableStats.setAchievements(achievements);
        game.setAvailableGameStats(availableStats);
        schema.setGame(game);

        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english")).thenReturn(schema);

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Counter-Strike 2", response.getBody().getGame().getGameName());
        verify(steamApiService, times(1)).getSchemaForGame(TEST_APP_ID, "english");
    }

    @Test
    void testGetGameSchema_NullSchema() {
        // Arrange
        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english")).thenReturn(null);

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(steamApiService, times(1)).getSchemaForGame(TEST_APP_ID, "english");
    }

    @Test
    void testGetGameSchema_NullGame() {
        // Arrange
        SteamGameSchemaResponseDto schema = new SteamGameSchemaResponseDto();
        schema.setGame(null);

        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english")).thenReturn(schema);

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void testGetGameSchema_NoAchievements() {
        // Arrange
        SteamGameSchemaResponseDto schema = new SteamGameSchemaResponseDto();
        SteamGameSchemaResponseDto.GameSchemaDto game = new SteamGameSchemaResponseDto.GameSchemaDto();
        game.setGameName("Test Game");
        game.setAvailableGameStats(null);
        schema.setGame(game);

        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english")).thenReturn(schema);

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetGameSchema_EmptyAchievementsList() {
        // Arrange
        SteamGameSchemaResponseDto schema = new SteamGameSchemaResponseDto();
        SteamGameSchemaResponseDto.GameSchemaDto game = new SteamGameSchemaResponseDto.GameSchemaDto();
        game.setGameName("Test Game");
        SteamGameSchemaResponseDto.AvailableGameStatsDto availableStats = new SteamGameSchemaResponseDto.AvailableGameStatsDto();
        availableStats.setAchievements(new ArrayList<>());
        game.setAvailableGameStats(availableStats);
        schema.setGame(game);

        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english")).thenReturn(schema);

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetGameSchema_FeignNotFound() {
        // Arrange
        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english"))
                .thenThrow(new feign.FeignException.NotFound("Not found", 
                        feign.Request.create(feign.Request.HttpMethod.GET, "url", new HashMap<>(), null, null, null), null, null));

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetGameSchema_FeignBadRequest() {
        // Arrange
        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english"))
                .thenThrow(new feign.FeignException.BadRequest("Bad request", 
                        feign.Request.create(feign.Request.HttpMethod.GET, "url", new HashMap<>(), null, null, null), null, null));

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetGameSchema_FeignException() {
        // Arrange
        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english"))
                .thenThrow(new feign.FeignException.InternalServerError("Server error", 
                        feign.Request.create(feign.Request.HttpMethod.GET, "url", new HashMap<>(), null, null, null), null, null));

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetGameSchema_GenericException() {
        // Arrange
        when(steamApiService.getSchemaForGame(TEST_APP_ID, "english"))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<SteamGameSchemaResponseDto> response = steamController.getGameSchema(TEST_APP_ID, "english");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ==================== RESOLVE VANITY URL TESTS ====================

    @Test
    void testResolveVanity_Success() {
        // Arrange
        String vanityUrl = "testuser";
        SteamResolveVanityResponseDto resolveResponse = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse responseData = new SteamResolveVanityResponseDto.InnerResponse();
        responseData.setSteamid(TEST_STEAM_ID);
        responseData.setSuccess(1);
        resolveResponse.setResponse(responseData);

        when(steamApiService.resolveVanityUrl(vanityUrl)).thenReturn(resolveResponse);

        // Act
        ResponseEntity<SteamResolveVanityResponseDto> response = steamController.resolveVanity(vanityUrl);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TEST_STEAM_ID, response.getBody().getResponse().getSteamid());
        assertEquals(1, response.getBody().getResponse().getSuccess());
        verify(steamApiService, times(1)).resolveVanityUrl(vanityUrl);
    }

    @Test
    void testResolveVanity_InternalServerError() {
        // Arrange
        String vanityUrl = "testuser";
        when(steamApiService.resolveVanityUrl(vanityUrl))
                .thenThrow(new RuntimeException("Service error"));

        // Act
        ResponseEntity<SteamResolveVanityResponseDto> response = steamController.resolveVanity(vanityUrl);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
