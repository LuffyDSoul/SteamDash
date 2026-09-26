package com.dacs.backend.service;

import com.dacs.backend.entity.GameAchievement;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AchievementQueryService
 */
@ExtendWith(MockitoExtension.class)
class AchievementQueryServiceTest {

    @Mock
    private GameAchievementRepository gameAchievementRepository;

    @Mock
    private UserGameAchievementRepository userGameAchievementRepository;

    @InjectMocks
    private AchievementQueryService achievementQueryService;

    private static final String TEST_STEAM_ID = "76561198000000001";
    private static final Long TEST_APP_ID = 730L;

    @BeforeEach
    void setUp() {
    }

    // Tests for countUserGamesWithAchievements

    @Test
    void testCountUserGamesWithAchievements_NoGames() {
        // Arrange
        when(userGameAchievementRepository.countDistinctAppIdBySteamId(TEST_STEAM_ID)).thenReturn(0L);

        // Act
        long count = achievementQueryService.countUserGamesWithAchievements(TEST_STEAM_ID);

        // Assert
        assertEquals(0L, count);
        verify(userGameAchievementRepository, times(1)).countDistinctAppIdBySteamId(TEST_STEAM_ID);
    }

    @Test
    void testCountUserGamesWithAchievements_MultipleGames() {
        // Arrange
        when(userGameAchievementRepository.countDistinctAppIdBySteamId(TEST_STEAM_ID)).thenReturn(5L);

        // Act
        long count = achievementQueryService.countUserGamesWithAchievements(TEST_STEAM_ID);

        // Assert
        assertEquals(5L, count);
    }

    // Tests for findMissingAchievements

    @Test
    void testFindMissingAchievements_AllExist() {
        // Arrange
        List<Long> appIds = Arrays.asList(730L, 570L, 440L);
        
        when(gameAchievementRepository.existsByAppId(anyLong())).thenReturn(true);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(anyString(), anyLong())).thenReturn(true);

        // Act
        List<Long> missing = achievementQueryService.findMissingAchievements(TEST_STEAM_ID, appIds);

        // Assert
        assertTrue(missing.isEmpty());
    }

    @Test
    void testFindMissingAchievements_MissingGameSchema() {
        // Arrange
        List<Long> appIds = Arrays.asList(730L, 570L, 440L);
        
        when(gameAchievementRepository.existsByAppId(730L)).thenReturn(true);
        when(gameAchievementRepository.existsByAppId(570L)).thenReturn(false); // Missing schema
        when(gameAchievementRepository.existsByAppId(440L)).thenReturn(true);
        
        when(userGameAchievementRepository.existsBySteamIdAndAppId(eq(TEST_STEAM_ID), anyLong())).thenReturn(true);

        // Act
        List<Long> missing = achievementQueryService.findMissingAchievements(TEST_STEAM_ID, appIds);

        // Assert
        assertEquals(1, missing.size());
        assertTrue(missing.contains(570L));
    }

    @Test
    void testFindMissingAchievements_MissingUserProgress() {
        // Arrange
        List<Long> appIds = Arrays.asList(730L, 570L, 440L);
        
        when(gameAchievementRepository.existsByAppId(anyLong())).thenReturn(true);
        
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, 730L)).thenReturn(true);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, 570L)).thenReturn(false); // Missing progress
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, 440L)).thenReturn(true);

        // Act
        List<Long> missing = achievementQueryService.findMissingAchievements(TEST_STEAM_ID, appIds);

        // Assert
        assertEquals(1, missing.size());
        assertTrue(missing.contains(570L));
    }

    @Test
    void testFindMissingAchievements_MultipleMissing() {
        // Arrange
        List<Long> appIds = Arrays.asList(730L, 570L, 440L);
        
        when(gameAchievementRepository.existsByAppId(730L)).thenReturn(true);
        when(gameAchievementRepository.existsByAppId(570L)).thenReturn(false);
        when(gameAchievementRepository.existsByAppId(440L)).thenReturn(false);
        
        when(userGameAchievementRepository.existsBySteamIdAndAppId(anyString(), anyLong())).thenReturn(true);

        // Act
        List<Long> missing = achievementQueryService.findMissingAchievements(TEST_STEAM_ID, appIds);

        // Assert
        assertEquals(2, missing.size());
        assertTrue(missing.contains(570L));
        assertTrue(missing.contains(440L));
    }

    @Test
    void testFindMissingAchievements_EmptyList() {
        // Arrange
        List<Long> appIds = Collections.emptyList();

        // Act
        List<Long> missing = achievementQueryService.findMissingAchievements(TEST_STEAM_ID, appIds);

        // Assert
        assertTrue(missing.isEmpty());
    }

    // Tests for getGameAchievementsWithUserProgress

    @Test
    void testGetGameAchievementsWithUserProgress_NoGameAchievements() {
        // Arrange
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.get("success"));
        assertTrue(result.containsKey("error"));
        verify(userGameAchievementRepository, never()).findBySteamIdAndAppId(anyString(), anyLong());
    }

    @Test
    void testGetGameAchievementsWithUserProgress_AllUnlocked() {
        // Arrange
        List<GameAchievement> gameAchievements = createGameAchievements(3);
        List<UserGameAchievement> userProgress = createUserProgress(3, 3);
        
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(gameAchievements);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(userProgress);

        // Act
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertEquals(true, result.get("success"));
        assertEquals(TEST_STEAM_ID, result.get("steamId"));
        assertEquals(TEST_APP_ID, result.get("appId"));
        assertEquals(3, result.get("totalAchievements"));
        assertEquals(3, result.get("unlockedAchievements"));
        
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) result.get("achievements");
        assertEquals(3, achievements.size());
        assertTrue(achievements.stream().allMatch(a -> (Boolean) a.get("achieved")));
    }

    @Test
    void testGetGameAchievementsWithUserProgress_PartiallyUnlocked() {
        // Arrange
        List<GameAchievement> gameAchievements = createGameAchievements(5);
        List<UserGameAchievement> userProgress = createUserProgress(5, 2); // Only 2 unlocked
        
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(gameAchievements);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(userProgress);

        // Act
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertEquals(true, result.get("success"));
        assertEquals(5, result.get("totalAchievements"));
        assertEquals(2, result.get("unlockedAchievements"));
        
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) result.get("achievements");
        long unlockedCount = achievements.stream().filter(a -> (Boolean) a.get("achieved")).count();
        assertEquals(2, unlockedCount);
    }

    @Test
    void testGetGameAchievementsWithUserProgress_NoUserProgress() {
        // Arrange
        List<GameAchievement> gameAchievements = createGameAchievements(3);
        
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(gameAchievements);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertEquals(true, result.get("success"));
        assertEquals(3, result.get("totalAchievements"));
        assertEquals(0, result.get("unlockedAchievements"));
        
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) result.get("achievements");
        assertEquals(3, achievements.size());
        assertTrue(achievements.stream().allMatch(a -> !(Boolean) a.get("achieved")));
        assertTrue(achievements.stream().allMatch(a -> a.get("unlocktime") == null));
    }

    @Test
    void testGetGameAchievementsWithUserProgress_AchievementDataComplete() {
        // Arrange
        List<GameAchievement> gameAchievements = Arrays.asList(
                GameAchievement.builder()
                        .id(1L)
                        .appId(TEST_APP_ID)
                        .achievementName("ACH_1")
                        .displayName("First Achievement")
                        .description("Get the first achievement")
                        .iconUrl("http://icon.jpg")
                        .iconGrayUrl("http://icongray.jpg")
                        .hidden(0)
                        .build()
        );
        
        List<UserGameAchievement> userProgress = Arrays.asList(
                UserGameAchievement.builder()
                        .steamId(TEST_STEAM_ID)
                        .appId(TEST_APP_ID)
                        .achievementName("ACH_1")
                        .achieved(true)
                        .unlockTime(1638360000L)
                        .build()
        );
        
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(gameAchievements);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(userProgress);

        // Act
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) result.get("achievements");
        Map<String, Object> ach = achievements.get(0);
        
        assertEquals("ACH_1", ach.get("apiname"));
        assertEquals("ACH_1", ach.get("name"));
        assertEquals("First Achievement", ach.get("displayName"));
        assertEquals("Get the first achievement", ach.get("description"));
        assertEquals("http://icon.jpg", ach.get("icon"));
        assertEquals("http://icongray.jpg", ach.get("icongray"));
        assertEquals(0, ach.get("hidden"));
        assertEquals(true, ach.get("achieved"));
        assertEquals(1638360000L, ach.get("unlocktime"));
    }

    @Test
    void testGetGameAchievementsWithUserProgress_HiddenAchievementHandling() {
        // Arrange
        List<GameAchievement> gameAchievements = Arrays.asList(
                GameAchievement.builder()
                        .appId(TEST_APP_ID)
                        .achievementName("ACH_1")
                        .hidden(1) // Hidden
                        .build(),
                GameAchievement.builder()
                        .appId(TEST_APP_ID)
                        .achievementName("ACH_2")
                        .hidden(null) // Null should default to 0
                        .build()
        );
        
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(gameAchievements);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID))
                .thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) result.get("achievements");
        assertEquals(1, achievements.get(0).get("hidden"));
        assertEquals(0, achievements.get(1).get("hidden"));
    }

    // Tests for hasAchievementsInDatabase

    @Test
    void testHasAchievementsInDatabase_BothExist() {
        // Arrange
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(true);

        // Act
        boolean result = achievementQueryService.hasAchievementsInDatabase(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertTrue(result);
    }

    @Test
    void testHasAchievementsInDatabase_MissingGameSchema() {
        // Arrange
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(false);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(true);

        // Act
        boolean result = achievementQueryService.hasAchievementsInDatabase(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void testHasAchievementsInDatabase_MissingUserProgress() {
        // Arrange
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(false);

        // Act
        boolean result = achievementQueryService.hasAchievementsInDatabase(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void testHasAchievementsInDatabase_BothMissing() {
        // Arrange
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(false);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(false);

        // Act
        boolean result = achievementQueryService.hasAchievementsInDatabase(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertFalse(result);
    }

    // Tests for getUserAchievementStatsBulk

    @Test
    void testGetUserAchievementStatsBulk_NoGames() {
        // Arrange
        when(userGameAchievementRepository.getAchievementStatsByUser(TEST_STEAM_ID))
                .thenReturn(Collections.emptyList());

        // Act
        Map<Long, Map<String, Object>> result = achievementQueryService.getUserAchievementStatsBulk(TEST_STEAM_ID);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetUserAchievementStatsBulk_MultipleGames() {
        // Arrange
        List<Object[]> stats = Arrays.asList(
                new Object[]{730L, 10L, 5L},
                new Object[]{570L, 20L, 15L},
                new Object[]{440L, 15L, 0L}
        );
        
        when(userGameAchievementRepository.getAchievementStatsByUser(TEST_STEAM_ID)).thenReturn(stats);

        // Act
        Map<Long, Map<String, Object>> result = achievementQueryService.getUserAchievementStatsBulk(TEST_STEAM_ID);

        // Assert
        assertEquals(3, result.size());
        
        assertEquals(10, result.get(730L).get("totalAchievements"));
        assertEquals(5, result.get(730L).get("unlockedAchievements"));
        
        assertEquals(20, result.get(570L).get("totalAchievements"));
        assertEquals(15, result.get(570L).get("unlockedAchievements"));
        
        assertEquals(15, result.get(440L).get("totalAchievements"));
        assertEquals(0, result.get(440L).get("unlockedAchievements"));
    }

    @Test
    void testGetUserAchievementStatsBulk_NullUnlockedCount() {
        // Arrange
        Object[] row = new Object[]{730L, 10L, null}; // Null unlocked count
        List<Object[]> stats = Collections.singletonList(row);
        
        when(userGameAchievementRepository.getAchievementStatsByUser(TEST_STEAM_ID)).thenReturn(stats);

        // Act
        Map<Long, Map<String, Object>> result = achievementQueryService.getUserAchievementStatsBulk(TEST_STEAM_ID);

        // Assert
        assertEquals(1, result.size());
        assertEquals(10, result.get(730L).get("totalAchievements"));
        assertEquals(0, result.get(730L).get("unlockedAchievements")); // Should default to 0
    }

    // Helper methods
    private List<GameAchievement> createGameAchievements(int count) {
        List<GameAchievement> achievements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            achievements.add(GameAchievement.builder()
                    .id((long) (i + 1))
                    .appId(TEST_APP_ID)
                    .achievementName("ACH_" + i)
                    .displayName("Achievement " + i)
                    .description("Description " + i)
                    .iconUrl("http://icon" + i + ".jpg")
                    .iconGrayUrl("http://icongray" + i + ".jpg")
                    .hidden(0)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return achievements;
    }

    private List<UserGameAchievement> createUserProgress(int total, int unlocked) {
        List<UserGameAchievement> progress = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            boolean achieved = i < unlocked;
            progress.add(UserGameAchievement.builder()
                    .id((long) (i + 1))
                    .steamId(TEST_STEAM_ID)
                    .appId(TEST_APP_ID)
                    .achievementName("ACH_" + i)
                    .achieved(achieved)
                    .unlockTime(achieved ? System.currentTimeMillis() / 1000 : null)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        return progress;
    }
}
