package com.dacs.backend.service;

import com.dacs.backend.dto.AchievementStatsDto;
import com.dacs.backend.entity.UserGameAchievement;
import com.dacs.backend.repository.GameAchievementRepository;
import com.dacs.backend.repository.UserGameAchievementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AchievementStatsService
 */
@ExtendWith(MockitoExtension.class)
class AchievementStatsServiceTest {

    @Mock
    private UserGameAchievementRepository userGameAchievementRepository;

    @Mock
    private GameAchievementRepository gameAchievementRepository;

    @InjectMocks
    private AchievementStatsService achievementStatsService;

    private static final String TEST_STEAM_ID = "76561198000000001";
    private static final Long GAME_1_APP_ID = 730L;
    private static final Long GAME_2_APP_ID = 570L;
    private static final Long GAME_3_APP_ID = 440L;

    private List<UserGameAchievement> userAchievements;

    @BeforeEach
    void setUp() {
        userAchievements = new ArrayList<>();
    }

    @Test
    void testCalculateAchievementStats_NoAchievements() {
        // Arrange
        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(Collections.emptyList());

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

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
        
        verify(userGameAchievementRepository, times(1)).findBySteamId(TEST_STEAM_ID);
        verify(gameAchievementRepository, never()).countByAppId(anyLong());
    }

    @Test
    void testCalculateAchievementStats_SingleGame100Percent() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 10); // 10/10 achievements
        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID))
                .thenReturn(10L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalJuegosConLogros());
        assertEquals(10, result.getTotalLogrosDesbloqueados());
        assertEquals(10, result.getTotalLogrosDisponibles());
        assertEquals(100.0, result.getPorcentajeGlobal(), 0.001);
        assertEquals(1, result.getJuegosCompletos100().size());
        assertTrue(result.getJuegosCercanos100().isEmpty());
        
        AchievementStatsDto.GameAchievementProgress game = result.getJuegosCompletos100().get(0);
        assertEquals(GAME_1_APP_ID, game.getAppId());
        assertEquals(10, game.getTotalAchievements());
        assertEquals(10, game.getUnlockedAchievements());
        assertEquals(100.0, game.getPercentage(), 0.001);
    }

    @Test
    void testCalculateAchievementStats_MultipleGames() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 10); // 100%
        createAchievementsForGame(GAME_2_APP_ID, 20, 18); // 90%
        createAchievementsForGame(GAME_3_APP_ID, 15, 5);  // 33.33%

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);
        when(gameAchievementRepository.countByAppId(GAME_2_APP_ID)).thenReturn(20L);
        when(gameAchievementRepository.countByAppId(GAME_3_APP_ID)).thenReturn(15L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(3, result.getTotalJuegosConLogros());
        assertEquals(33, result.getTotalLogrosDesbloqueados()); // 10 + 18 + 5
        assertEquals(45, result.getTotalLogrosDisponibles());   // 10 + 20 + 15
        assertEquals(73.33, result.getPorcentajeGlobal(), 0.01);
        assertEquals(1, result.getJuegosCompletos100().size());
        assertEquals(1, result.getJuegosCercanos100().size());
        
        // Verify order - highest percentage first
        assertEquals(GAME_1_APP_ID, result.getJuegosMasProgreso().get(0).getAppId());
    }

    @Test
    void testCalculateAchievementStats_CercanosA100() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 100, 85); // 85%
        createAchievementsForGame(GAME_2_APP_ID, 50, 45);  // 90%
        createAchievementsForGame(GAME_3_APP_ID, 20, 16);  // 80%

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(100L);
        when(gameAchievementRepository.countByAppId(GAME_2_APP_ID)).thenReturn(50L);
        when(gameAchievementRepository.countByAppId(GAME_3_APP_ID)).thenReturn(20L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(3, result.getJuegosCercanos100().size());
        assertTrue(result.getJuegosCompletos100().isEmpty());
        
        // Verify they are sorted by percentage descending
        List<AchievementStatsDto.GameAchievementProgress> cercanos = result.getJuegosCercanos100();
        assertTrue(cercanos.get(0).getPercentage() >= cercanos.get(1).getPercentage());
        assertTrue(cercanos.get(1).getPercentage() >= cercanos.get(2).getPercentage());
    }

    @Test
    void testCalculateAchievementStats_WithPlaytimeOrder() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 5);  // 50%
        createAchievementsForGame(GAME_2_APP_ID, 20, 15); // 75%
        createAchievementsForGame(GAME_3_APP_ID, 15, 10); // 66.67%

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);
        when(gameAchievementRepository.countByAppId(GAME_2_APP_ID)).thenReturn(20L);
        when(gameAchievementRepository.countByAppId(GAME_3_APP_ID)).thenReturn(15L);

        // Playtime order: GAME_2, GAME_1, GAME_3
        List<Long> topGamesWithPlaytime = Arrays.asList(GAME_2_APP_ID, GAME_1_APP_ID, GAME_3_APP_ID);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, topGamesWithPlaytime);

        // Assert
        assertEquals(3, result.getJuegosMasProgreso().size());
        // Should be ordered by playtime, not percentage
        assertEquals(GAME_2_APP_ID, result.getJuegosMasProgreso().get(0).getAppId());
        assertEquals(GAME_1_APP_ID, result.getJuegosMasProgreso().get(1).getAppId());
        assertEquals(GAME_3_APP_ID, result.getJuegosMasProgreso().get(2).getAppId());
    }

    @Test
    void testCalculateAchievementStats_LimitTo10Games() {
        // Arrange
        for (int i = 0; i < 15; i++) {
            createAchievementsForGame((long) (1000 + i), 10, 5);
            when(gameAchievementRepository.countByAppId((long) (1000 + i))).thenReturn(10L);
        }

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(15, result.getTotalJuegosConLogros());
        assertTrue(result.getJuegosMasProgreso().size() <= 10);
        assertTrue(result.getJuegosCercanos100().size() <= 10);
    }

    @Test
    void testCalculateAchievementStats_SkipsGamesWithNoSchema() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 5);
        createAchievementsForGame(GAME_2_APP_ID, 20, 10);

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);
        when(gameAchievementRepository.countByAppId(GAME_2_APP_ID)).thenReturn(0L); // No schema

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        // The service counts distinct appIds from user achievements, then filters by schema
        // So totalJuegosConLogros will be 2, but only 1 will have valid progress data
        assertEquals(2, result.getTotalJuegosConLogros()); // Counts both games (from user achievements)
        assertEquals(5, result.getTotalLogrosDesbloqueados()); // Only from GAME_1
        assertEquals(10, result.getTotalLogrosDisponibles()); // Only from GAME_1
        assertEquals(1, result.getJuegosMasProgreso().size()); // Only GAME_1 has valid data
    }

    @Test
    void testCalculateAchievementStats_MixedUnlockedStates() {
        // Arrange
        List<UserGameAchievement> achievements = new ArrayList<>();
        
        // Game 1: 5 unlocked, 5 locked
        for (int i = 0; i < 10; i++) {
            achievements.add(createAchievement(GAME_1_APP_ID, "ACH_" + i, i < 5));
        }
        
        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(achievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(5, result.getTotalLogrosDesbloqueados());
        assertEquals(10, result.getTotalLogrosDisponibles());
        assertEquals(50.0, result.getPorcentajeGlobal(), 0.001);
    }

    @Test
    void testCalculateAchievementStats_ZeroPercentage() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 0); // 0%

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(0, result.getTotalLogrosDesbloqueados());
        assertEquals(0.0, result.getPorcentajeGlobal(), 0.001);
        assertTrue(result.getJuegosCompletos100().isEmpty());
        assertTrue(result.getJuegosCercanos100().isEmpty());
    }

    @Test
    void testCalculateAchievementStats_EdgeCase80Percent() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 8); // Exactly 80%

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(1, result.getJuegosCercanos100().size());
        assertEquals(80.0, result.getJuegosCercanos100().get(0).getPercentage(), 0.001);
    }

    @Test
    void testCalculateAchievementStats_EdgeCase99Point9Percent() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 1000, 999); // 99.9%

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(1000L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, null);

        // Assert
        assertEquals(1, result.getJuegosCercanos100().size());
        assertTrue(result.getJuegosCompletos100().isEmpty());
        assertEquals(99.9, result.getJuegosCercanos100().get(0).getPercentage(), 0.001);
    }

    @Test
    void testCalculateAchievementStats_EmptyPlaytimeList() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 5);

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);

        // Act
        AchievementStatsDto result = achievementStatsService.calculateAchievementStats(
                TEST_STEAM_ID, Collections.emptyList());

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getJuegosMasProgreso().size());
    }

    @Test
    void testCalculateAchievementStats_VerifyRepositoryCalls() {
        // Arrange
        createAchievementsForGame(GAME_1_APP_ID, 10, 5);
        createAchievementsForGame(GAME_2_APP_ID, 20, 10);

        when(userGameAchievementRepository.findBySteamId(TEST_STEAM_ID))
                .thenReturn(userAchievements);
        when(gameAchievementRepository.countByAppId(GAME_1_APP_ID)).thenReturn(10L);
        when(gameAchievementRepository.countByAppId(GAME_2_APP_ID)).thenReturn(20L);

        // Act
        achievementStatsService.calculateAchievementStats(TEST_STEAM_ID, null);

        // Assert
        verify(userGameAchievementRepository, times(1)).findBySteamId(TEST_STEAM_ID);
        verify(gameAchievementRepository, times(1)).countByAppId(GAME_1_APP_ID);
        verify(gameAchievementRepository, times(1)).countByAppId(GAME_2_APP_ID);
        verifyNoMoreInteractions(userGameAchievementRepository);
        verifyNoMoreInteractions(gameAchievementRepository);
    }

    // Helper methods
    private void createAchievementsForGame(Long appId, int total, int unlocked) {
        for (int i = 0; i < total; i++) {
            boolean achieved = i < unlocked;
            UserGameAchievement achievement = createAchievement(appId, "ACHIEVEMENT_" + i, achieved);
            userAchievements.add(achievement);
        }
    }

    private UserGameAchievement createAchievement(Long appId, String name, boolean achieved) {
        return UserGameAchievement.builder()
                .id((long) (Math.random() * 100000))
                .steamId(TEST_STEAM_ID)
                .appId(appId)
                .achievementName(name)
                .achieved(achieved)
                .unlockTime(achieved ? System.currentTimeMillis() / 1000 : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
