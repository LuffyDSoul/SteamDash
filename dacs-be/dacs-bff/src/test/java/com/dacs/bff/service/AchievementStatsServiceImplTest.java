package com.dacs.bff.service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.client.BackendClient;
import com.dacs.bff.dto.AchievementStatsDto;
import com.dacs.bff.dto.SteamOwnedGamesResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AchievementStatsServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class AchievementStatsServiceImplTest {

    @Mock
    private BackendClient backendClient;

    @Mock
    private ApiConectorClient apiConectorClient;

    @InjectMocks
    private AchievementStatsServiceImpl achievementStatsService;

    private static final String TEST_STEAM_ID = "76561198000000000";

    // ==================== GET ACHIEVEMENT STATS ENRICHED TESTS ====================

    @Test
    void testGetAchievementStatsEnriched_Success_WithMultipleGames() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        
        AchievementStatsDto backendStats = createBackendStats();
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(backendClient.getAchievementStats(eq(TEST_STEAM_ID), anyList()))
                .thenReturn(backendStats);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_STEAM_ID, result.getSteamId());
        assertEquals(3, result.getTotalJuegosConLogros());
        assertEquals(50, result.getTotalLogrosDesbloqueados());
        assertEquals(100, result.getTotalLogrosDisponibles());
        assertEquals(50.0, result.getPorcentajeGlobal());
        
        // Verificar enriquecimiento
        assertEquals(1, result.getJuegosCompletos100().size());
        AchievementStatsDto.GameAchievementProgress completedGame = result.getJuegosCompletos100().get(0);
        assertEquals("Counter-Strike 2", completedGame.getGameName());
        assertEquals(5000, completedGame.getPlaytimeForever());
        assertTrue(completedGame.getHeaderImage().contains("730"));
        
        verify(apiConectorClient, times(1)).getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true));
        verify(backendClient, times(1)).getAchievementStats(eq(TEST_STEAM_ID), anyList());
    }

    @Test
    void testGetAchievementStatsEnriched_PassesTop50GamesByPlaytime() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createLargeOwnedGamesResponse(100);
        AchievementStatsDto backendStats = createBackendStats();
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(backendClient.getAchievementStats(eq(TEST_STEAM_ID), anyList()))
                .thenReturn(backendStats);

        ArgumentCaptor<List<Long>> appIdsCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        verify(backendClient, times(1)).getAchievementStats(eq(TEST_STEAM_ID), appIdsCaptor.capture());
        
        List<Long> passedAppIds = appIdsCaptor.getValue();
        assertEquals(50, passedAppIds.size()); // Top 50 juegos
    }

    @Test
    void testGetAchievementStatsEnriched_EnrichesAllGameLists() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        AchievementStatsDto backendStats = createBackendStatsWithMultipleLists();
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(backendClient.getAchievementStats(eq(TEST_STEAM_ID), anyList()))
                .thenReturn(backendStats);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        
        // Verificar enriquecimiento de juegos completos
        assertEquals(1, result.getJuegosCompletos100().size());
        assertEquals("Counter-Strike 2", result.getJuegosCompletos100().get(0).getGameName());
        assertEquals(5000, result.getJuegosCompletos100().get(0).getPlaytimeForever());
        
        // Verificar enriquecimiento de juegos cercanos a 100%
        assertEquals(1, result.getJuegosCercanos100().size());
        assertEquals("Dota 2", result.getJuegosCercanos100().get(0).getGameName());
        assertEquals(3000, result.getJuegosCercanos100().get(0).getPlaytimeForever());
        
        // Verificar enriquecimiento de juegos con más progreso
        assertEquals(1, result.getJuegosMasProgreso().size());
        assertEquals("Team Fortress 2", result.getJuegosMasProgreso().get(0).getGameName());
        assertEquals(1000, result.getJuegosMasProgreso().get(0).getPlaytimeForever());
    }

    @Test
    void testGetAchievementStatsEnriched_NullOwnedGamesResponse_ReturnsEmptyStats() {
        // Arrange
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(null);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_STEAM_ID, result.getSteamId());
        assertEquals(0, result.getTotalJuegosConLogros());
        assertEquals(0, result.getTotalLogrosDesbloqueados());
        assertEquals(0, result.getTotalLogrosDisponibles());
        assertEquals(0.0, result.getPorcentajeGlobal());
        assertTrue(result.getJuegosCompletos100().isEmpty());
        assertTrue(result.getJuegosCercanos100().isEmpty());
        assertTrue(result.getJuegosMasProgreso().isEmpty());
        
        verifyNoInteractions(backendClient);
    }

    @Test
    void testGetAchievementStatsEnriched_NullResponseInner_ReturnsEmptyStats() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        ownedGamesResponse.setResponse(null);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalJuegosConLogros());
        verifyNoInteractions(backendClient);
    }

    @Test
    void testGetAchievementStatsEnriched_NullGames_ReturnsEmptyStats() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGames(null);
        ownedGamesResponse.setResponse(response);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalJuegosConLogros());
        verifyNoInteractions(backendClient);
    }

    @Test
    void testGetAchievementStatsEnriched_EmptyGames_ReturnsEmptyStats() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGames(Collections.emptyList());
        ownedGamesResponse.setResponse(response);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalJuegosConLogros());
        verifyNoInteractions(backendClient);
    }

    @Test
    void testGetAchievementStatsEnriched_GameNotFoundInLibrary_DoesNotEnrich() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        
        AchievementStatsDto backendStats = AchievementStatsDto.builder()
                .steamId(TEST_STEAM_ID)
                .totalJuegosConLogros(1)
                .totalLogrosDesbloqueados(10)
                .totalLogrosDisponibles(20)
                .porcentajeGlobal(50.0)
                .juegosCompletos100(Arrays.asList(createGameProgress(999999L, 10, 10))) // AppId no existe en biblioteca
                .juegosCercanos100(Collections.emptyList())
                .juegosMasProgreso(Collections.emptyList())
                .build();
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(backendClient.getAchievementStats(eq(TEST_STEAM_ID), anyList()))
                .thenReturn(backendStats);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getJuegosCompletos100().size());
        assertNull(result.getJuegosCompletos100().get(0).getGameName()); // No enriquecido
        assertNull(result.getJuegosCompletos100().get(0).getPlaytimeForever());
    }

    @Test
    void testGetAchievementStatsEnriched_NullPlaytime_SetsZero() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        
        List<SteamOwnedGamesResponseDto.OwnedGameDto> games = new ArrayList<>();
        SteamOwnedGamesResponseDto.OwnedGameDto game = new SteamOwnedGamesResponseDto.OwnedGameDto();
        game.setAppId(730L);
        game.setName("Counter-Strike 2");
        game.setPlaytimeForever(null); // Null playtime
        games.add(game);
        
        response.setGames(games);
        ownedGamesResponse.setResponse(response);
        
        AchievementStatsDto backendStats = AchievementStatsDto.builder()
                .steamId(TEST_STEAM_ID)
                .totalJuegosConLogros(1)
                .totalLogrosDesbloqueados(10)
                .totalLogrosDisponibles(10)
                .porcentajeGlobal(100.0)
                .juegosCompletos100(Arrays.asList(createGameProgress(730L, 10, 10)))
                .juegosCercanos100(Collections.emptyList())
                .juegosMasProgreso(Collections.emptyList())
                .build();
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(backendClient.getAchievementStats(eq(TEST_STEAM_ID), anyList()))
                .thenReturn(backendStats);

        // Act
        AchievementStatsDto result = achievementStatsService.getAchievementStatsEnriched(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getJuegosCompletos100().size());
        assertEquals(0, result.getJuegosCompletos100().get(0).getPlaytimeForever()); // Convertido a 0
    }

    // ==================== SYNC ALL ACHIEVEMENTS TESTS ====================

    @Test
    void testSyncAllAchievements_Success() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        ArgumentCaptor<List<Long>> appIdsCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        String result = achievementStatsService.syncAllAchievements(TEST_STEAM_ID);

        // Assert
        assertTrue(result.contains("Sincronización iniciada para 3 juegos"));
        
        verify(backendClient, times(1)).forceRefreshLogros(eq(TEST_STEAM_ID), appIdsCaptor.capture());
        
        List<Long> passedAppIds = appIdsCaptor.getValue();
        assertEquals(3, passedAppIds.size());
        assertTrue(passedAppIds.contains(730L));
        assertTrue(passedAppIds.contains(570L));
        assertTrue(passedAppIds.contains(440L));
    }

    @Test
    void testSyncAllAchievements_NullOwnedGamesResponse_ReturnsError() {
        // Arrange
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(null);

        // Act
        String result = achievementStatsService.syncAllAchievements(TEST_STEAM_ID);

        // Assert
        assertTrue(result.contains("Error: No se pudo obtener la biblioteca del usuario"));
        verifyNoInteractions(backendClient);
    }

    @Test
    void testSyncAllAchievements_NullResponseInner_ReturnsError() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        ownedGamesResponse.setResponse(null);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        // Act
        String result = achievementStatsService.syncAllAchievements(TEST_STEAM_ID);

        // Assert
        assertTrue(result.contains("Error: No se pudo obtener la biblioteca del usuario"));
        verifyNoInteractions(backendClient);
    }

    @Test
    void testSyncAllAchievements_NullGames_ReturnsError() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGames(null);
        ownedGamesResponse.setResponse(response);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        // Act
        String result = achievementStatsService.syncAllAchievements(TEST_STEAM_ID);

        // Assert
        assertTrue(result.contains("Error: No se pudo obtener la biblioteca del usuario"));
        verifyNoInteractions(backendClient);
    }

    @Test
    void testSyncAllAchievements_EmptyGames_ReturnsError() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setGames(Collections.emptyList());
        ownedGamesResponse.setResponse(response);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        // Act
        String result = achievementStatsService.syncAllAchievements(TEST_STEAM_ID);

        // Assert
        assertTrue(result.contains("Error: La biblioteca está vacía"));
        verifyNoInteractions(backendClient);
    }

    @Test
    void testSyncAllAchievements_LargeLibrary_SyncsAll() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createLargeOwnedGamesResponse(200);
        
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);

        ArgumentCaptor<List<Long>> appIdsCaptor = ArgumentCaptor.forClass(List.class);

        // Act
        String result = achievementStatsService.syncAllAchievements(TEST_STEAM_ID);

        // Assert
        assertTrue(result.contains("Sincronización iniciada para 200 juegos"));
        
        verify(backendClient, times(1)).forceRefreshLogros(eq(TEST_STEAM_ID), appIdsCaptor.capture());
        
        List<Long> passedAppIds = appIdsCaptor.getValue();
        assertEquals(200, passedAppIds.size());
    }

    // ==================== HELPER METHODS ====================

    private SteamOwnedGamesResponseDto createOwnedGamesResponse() {
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        
        List<SteamOwnedGamesResponseDto.OwnedGameDto> games = new ArrayList<>();
        
        SteamOwnedGamesResponseDto.OwnedGameDto game1 = new SteamOwnedGamesResponseDto.OwnedGameDto();
        game1.setAppId(730L);
        game1.setName("Counter-Strike 2");
        game1.setPlaytimeForever(5000);
        games.add(game1);
        
        SteamOwnedGamesResponseDto.OwnedGameDto game2 = new SteamOwnedGamesResponseDto.OwnedGameDto();
        game2.setAppId(570L);
        game2.setName("Dota 2");
        game2.setPlaytimeForever(3000);
        games.add(game2);
        
        SteamOwnedGamesResponseDto.OwnedGameDto game3 = new SteamOwnedGamesResponseDto.OwnedGameDto();
        game3.setAppId(440L);
        game3.setName("Team Fortress 2");
        game3.setPlaytimeForever(1000);
        games.add(game3);
        
        response.setGames(games);
        ownedGamesResponse.setResponse(response);
        
        return ownedGamesResponse;
    }

    private SteamOwnedGamesResponseDto createLargeOwnedGamesResponse(int numGames) {
        SteamOwnedGamesResponseDto ownedGamesResponse = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse response = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        
        List<SteamOwnedGamesResponseDto.OwnedGameDto> games = new ArrayList<>();
        
        for (int i = 0; i < numGames; i++) {
            SteamOwnedGamesResponseDto.OwnedGameDto game = new SteamOwnedGamesResponseDto.OwnedGameDto();
            game.setAppId((long) (1000 + i));
            game.setName("Game " + i);
            game.setPlaytimeForever((numGames - i) * 100); // Descendente
            games.add(game);
        }
        
        response.setGames(games);
        ownedGamesResponse.setResponse(response);
        
        return ownedGamesResponse;
    }

    private AchievementStatsDto createBackendStats() {
        return AchievementStatsDto.builder()
                .steamId(TEST_STEAM_ID)
                .totalJuegosConLogros(3)
                .totalLogrosDesbloqueados(50)
                .totalLogrosDisponibles(100)
                .porcentajeGlobal(50.0)
                .juegosCompletos100(Arrays.asList(createGameProgress(730L, 10, 10)))
                .juegosCercanos100(Collections.emptyList())
                .juegosMasProgreso(Collections.emptyList())
                .build();
    }

    private AchievementStatsDto createBackendStatsWithMultipleLists() {
        return AchievementStatsDto.builder()
                .steamId(TEST_STEAM_ID)
                .totalJuegosConLogros(3)
                .totalLogrosDesbloqueados(50)
                .totalLogrosDisponibles(100)
                .porcentajeGlobal(50.0)
                .juegosCompletos100(Arrays.asList(createGameProgress(730L, 10, 10)))
                .juegosCercanos100(Arrays.asList(createGameProgress(570L, 9, 10)))
                .juegosMasProgreso(Arrays.asList(createGameProgress(440L, 5, 10)))
                .build();
    }

    private AchievementStatsDto.GameAchievementProgress createGameProgress(Long appId, int unlocked, int total) {
        AchievementStatsDto.GameAchievementProgress progress = new AchievementStatsDto.GameAchievementProgress();
        progress.setAppId(appId);
        progress.setUnlockedAchievements(unlocked);
        progress.setTotalAchievements(total);
        progress.setPercentage((double) unlocked / total * 100);
        return progress;
    }
}
