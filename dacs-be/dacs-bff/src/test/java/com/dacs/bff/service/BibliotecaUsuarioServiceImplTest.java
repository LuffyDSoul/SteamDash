package com.dacs.bff.service;

import com.dacs.bff.api.client.ApiBackendClient;
import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.client.BackendClient;
import com.dacs.bff.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BibliotecaUsuarioServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class BibliotecaUsuarioServiceImplTest {

    @Mock
    private ApiConectorClient apiConectorClient;

    @Mock
    private ApiConectorService apiConectorService;

    @Mock
    private ApiBackendClient apiBackendClient;

    @Mock
    private BackendClient backendClient;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BibliotecaUsuarioServiceImpl bibliotecaUsuarioService;

    private static final String TEST_STEAM_ID = "76561198000000000";
    private static final Integer TEST_APP_ID = 730;

    // ==================== GET BIBLIOTECA USUARIO TESTS ====================

    @Test
    void testGetBibliotecaUsuario_Success_BasicFlow() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_STEAM_ID, result.getSteamId());
        assertEquals("TestUser", result.getPersonaName());
        assertEquals("http://avatar.url", result.getAvatarUrl());
        assertEquals(3, result.getTotalJuegos());
        assertEquals(3, result.getJuegos().size());
        
        verify(apiConectorService, times(1)).getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true));
        verify(apiConectorClient, times(1)).getPlayerSummaries(eq(TEST_STEAM_ID));
    }

    @Test
    void testGetBibliotecaUsuario_NullOwnedGames_ReturnsEmptyBiblioteca() {
        // Arrange
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(null);

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_STEAM_ID, result.getSteamId());
        assertEquals(0, result.getTotalJuegos());
        assertTrue(result.getJuegos().isEmpty());
        
        verifyNoInteractions(apiConectorClient);
        verifyNoInteractions(apiBackendClient);
    }

    @Test
    void testGetBibliotecaUsuario_NullInnerResponse_ReturnsEmptyBiblioteca() {
        // Arrange
        SteamOwnedGamesResponseDto response = new SteamOwnedGamesResponseDto();
        response.setResponse(null);
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(response);

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalJuegos());
    }

    @Test
    void testGetBibliotecaUsuario_NullGames_ReturnsEmptyBiblioteca() {
        // Arrange
        SteamOwnedGamesResponseDto response = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse inner = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        inner.setGames(null);
        response.setResponse(inner);
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(response);

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalJuegos());
    }

    @Test
    void testGetBibliotecaUsuario_NullPlayerInfo_UsesDefaultPersonaName() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(null);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals("Usuario", result.getPersonaName());
        assertNull(result.getAvatarUrl());
    }

    @Test
    void testGetBibliotecaUsuario_FiltersTestGames() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponseWithTestGames();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        // Should filter out test games
        assertTrue(result.getTotalJuegos() < 7); // Less than original 7 games with test names
        
        // Verify no game names contain filtered keywords
        for (JuegoUsuarioDto juego : result.getJuegos()) {
            String lowerName = juego.getName().toLowerCase();
            assertFalse(lowerName.contains("public"));
            assertFalse(lowerName.contains("test"));
            assertFalse(lowerName.contains("beta"));
            assertFalse(lowerName.contains("server"));
        }
    }

    @Test
    void testGetBibliotecaUsuario_EnrichesWithRecentlyPlayed() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        SteamRecentlyPlayedGamesResponseDto recentlyPlayed = createRecentlyPlayedResponseWithPlaytime();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(recentlyPlayed);
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        
        // Find CS2 game (730)
        Optional<JuegoUsuarioDto> cs2 = result.getJuegos().stream()
                .filter(j -> j.getAppId() != null && j.getAppId() == 730)
                .findFirst();
        
        assertTrue(cs2.isPresent());
        assertEquals(120, cs2.get().getPlaytime2Weeks()); // Enriched from recently played
    }

    @Test
    void testGetBibliotecaUsuario_AddsBorrowedGamesFromFamilySharing() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        SteamRecentlyPlayedGamesResponseDto recentlyPlayed = createRecentlyPlayedWithBorrowedGame();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(recentlyPlayed);
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        
        // Should have 4 games (3 owned + 1 borrowed)
        assertEquals(4, result.getJuegos().size());
        
        // Find borrowed game
        Optional<JuegoUsuarioDto> borrowed = result.getJuegos().stream()
                .filter(j -> j.getAppId() != null && j.getAppId() == 999)
                .findFirst();
        
        assertTrue(borrowed.isPresent());
        assertEquals(true, borrowed.get().getIsBorrowed());
        assertEquals("Borrowed Game", borrowed.get().getName());
    }

    @Test
    void testGetBibliotecaUsuario_EnrichesWithDatabaseInfo() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        List<BibliotecaJuegoDto> dbGames = createDatabaseGames();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(dbGames);
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        
        // Find CS2 game (730)
        Optional<JuegoUsuarioDto> cs2 = result.getJuegos().stream()
                .filter(j -> j.getAppId() != null && j.getAppId() == 730)
                .findFirst();
        
        assertTrue(cs2.isPresent());
        assertEquals("$14.99", cs2.get().getPrice()); // From DB
        assertEquals(false, cs2.get().getIsFree()); // From DB
        assertEquals(2, cs2.get().getTags().size()); // From DB
        assertTrue(cs2.get().getTags().contains("FPS"));
        assertTrue(cs2.get().getTags().contains("Multiplayer"));
    }

    @Test
    void testGetBibliotecaUsuario_EnrichesWithAchievementStats() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        Map<String, Object> achievementStats = createAchievementStatsResponse();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(achievementStats);

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        
        // Find CS2 game (730)
        Optional<JuegoUsuarioDto> cs2 = result.getJuegos().stream()
                .filter(j -> j.getAppId() != null && j.getAppId() == 730)
                .findFirst();
        
        assertTrue(cs2.isPresent());
        assertEquals(167, cs2.get().getTotalAchievements());
        assertEquals(50, cs2.get().getUnlockedAchievements());
    }

    @Test
    void testGetBibliotecaUsuario_HandlesRecentlyPlayedError_ContinuesProcessing() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenThrow(new RuntimeException("API Error"));
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalJuegos()); // Still processes successfully
    }

    @Test
    void testGetBibliotecaUsuario_HandlesDatabaseEnrichmentError_SetsDefaultPrices() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenThrow(new RuntimeException("DB Error"));
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenReturn(createEmptyStatsResponse());

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        
        // All games should have N/A price
        for (JuegoUsuarioDto juego : result.getJuegos()) {
            assertEquals("N/A", juego.getPrice());
        }
    }

    @Test
    void testGetBibliotecaUsuario_HandlesAchievementStatsError_ContinuesProcessing() {
        // Arrange
        SteamOwnedGamesResponseDto ownedGamesResponse = createOwnedGamesResponse();
        SteamPlayerSummariesResponseDto playerInfo = createPlayerInfo();
        
        when(apiConectorService.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(ownedGamesResponse);
        when(apiConectorClient.getPlayerSummaries(eq(TEST_STEAM_ID)))
                .thenReturn(playerInfo);
        when(apiConectorClient.getRecentlyPlayedGames(eq(TEST_STEAM_ID)))
                .thenReturn(createRecentlyPlayedResponse());
        when(apiBackendClient.getBibliotecaJuegosDesdeDb(anyList()))
                .thenReturn(Collections.emptyList());
        when(apiBackendClient.getUserAchievementStats(eq(TEST_STEAM_ID)))
                .thenThrow(new RuntimeException("Stats Error"));

        // Act
        BibliotecaUsuarioDto result = bibliotecaUsuarioService.getBibliotecaUsuario(TEST_STEAM_ID, false);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getTotalJuegos());
        
        // Games should not have achievement stats
        for (JuegoUsuarioDto juego : result.getJuegos()) {
            assertNull(juego.getTotalAchievements());
            assertNull(juego.getUnlockedAchievements());
        }
    }

    // ==================== HELPER METHODS ====================

    private SteamOwnedGamesResponseDto createOwnedGamesResponse() {
        SteamOwnedGamesResponseDto response = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse inner = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        
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
        
        inner.setGames(games);
        response.setResponse(inner);
        
        return response;
    }

    private SteamOwnedGamesResponseDto createOwnedGamesResponseWithTestGames() {
        SteamOwnedGamesResponseDto response = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse inner = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        
        List<SteamOwnedGamesResponseDto.OwnedGameDto> games = new ArrayList<>();
        
        games.add(createGame(730L, "Counter-Strike 2", 5000));
        games.add(createGame(731L, "CS2 Public Test", 100));
        games.add(createGame(732L, "Game Beta Test", 50));
        games.add(createGame(733L, "Private Server Tools", 10));
        games.add(createGame(734L, "Playtest Version", 5));
        games.add(createGame(570L, "Dota 2", 3000));
        games.add(createGame(440L, "Team Fortress 2", 1000));
        
        inner.setGames(games);
        response.setResponse(inner);
        
        return response;
    }

    private SteamOwnedGamesResponseDto.OwnedGameDto createGame(Long appId, String name, Integer playtime) {
        SteamOwnedGamesResponseDto.OwnedGameDto game = new SteamOwnedGamesResponseDto.OwnedGameDto();
        game.setAppId(appId);
        game.setName(name);
        game.setPlaytimeForever(playtime);
        return game;
    }

    private SteamPlayerSummariesResponseDto createPlayerInfo() {
        SteamPlayerSummariesResponseDto response = new SteamPlayerSummariesResponseDto();
        SteamPlayerSummariesResponseDto.ResponseDto inner = new SteamPlayerSummariesResponseDto.ResponseDto();
        
        SteamPlayerSummariesResponseDto.PlayerDto player = new SteamPlayerSummariesResponseDto.PlayerDto();
        player.setPersonaName("TestUser");
        player.setAvatarFull("http://avatar.url");
        
        inner.setPlayers(Arrays.asList(player));
        response.setResponse(inner);
        
        return response;
    }

    private SteamRecentlyPlayedGamesResponseDto createRecentlyPlayedResponse() {
        SteamRecentlyPlayedGamesResponseDto response = new SteamRecentlyPlayedGamesResponseDto();
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse inner = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse();
        inner.setGames(Collections.emptyList());
        response.setResponse(inner);
        return response;
    }

    private SteamRecentlyPlayedGamesResponseDto createRecentlyPlayedResponseWithPlaytime() {
        SteamRecentlyPlayedGamesResponseDto response = new SteamRecentlyPlayedGamesResponseDto();
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse inner = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse();
        
        List<SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedGameDto> games = new ArrayList<>();
        
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedGameDto game = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedGameDto();
        game.setAppId(730L);
        game.setName("Counter-Strike 2");
        game.setPlaytime2Weeks(120);
        game.setPlaytimeForever(5000);
        games.add(game);
        
        inner.setGames(games);
        response.setResponse(inner);
        
        return response;
    }

    private SteamRecentlyPlayedGamesResponseDto createRecentlyPlayedWithBorrowedGame() {
        SteamRecentlyPlayedGamesResponseDto response = new SteamRecentlyPlayedGamesResponseDto();
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse inner = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedResponse();
        
        List<SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedGameDto> games = new ArrayList<>();
        
        // Borrowed game (not in owned games)
        SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedGameDto borrowed = new SteamRecentlyPlayedGamesResponseDto.RecentlyPlayedGameDto();
        borrowed.setAppId(999L);
        borrowed.setName("Borrowed Game");
        borrowed.setPlaytime2Weeks(50);
        borrowed.setPlaytimeForever(50);
        games.add(borrowed);
        
        inner.setGames(games);
        response.setResponse(inner);
        
        return response;
    }

    private List<BibliotecaJuegoDto> createDatabaseGames() {
        List<BibliotecaJuegoDto> games = new ArrayList<>();
        
        BibliotecaJuegoDto game = new BibliotecaJuegoDto();
        game.setAppId(730L);
        game.setPrice("$14.99");
        game.setIsFree(false);
        game.setHeaderImage("https://custom.header.url/730.jpg");
        
        List<Tags> tags = new ArrayList<>();
        Tags tag1 = new Tags();
        tag1.setTag("FPS");
        Tags tag2 = new Tags();
        tag2.setTag("Multiplayer");
        tags.add(tag1);
        tags.add(tag2);
        game.setTags(tags);
        
        games.add(game);
        
        return games;
    }

    private Map<String, Object> createEmptyStatsResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("stats", new HashMap<Long, Map<String, Object>>());
        return response;
    }

    private Map<String, Object> createAchievementStatsResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<Long, Map<String, Object>> stats = new HashMap<>();
        
        Map<String, Object> cs2Stats = new HashMap<>();
        cs2Stats.put("totalAchievements", 167);
        cs2Stats.put("unlockedAchievements", 50);
        stats.put(730L, cs2Stats);
        
        response.put("stats", stats);
        return response;
    }
}
