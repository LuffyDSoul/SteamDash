package com.dacs.backend.service;

import com.dacs.backend.entity.GameAchievement;
import com.dacs.backend.entity.UserGameAchievement;
import com.dacs.backend.repository.GameAchievementRepository;
import com.dacs.backend.repository.UserGameAchievementRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AchievementIngestionService
 */
@ExtendWith(MockitoExtension.class)
class AchievementIngestionServiceTest {

    @Mock
    private GameAchievementRepository gameAchievementRepository;

    @Mock
    private UserGameAchievementRepository userGameAchievementRepository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private AchievementIngestionService achievementIngestionService;

    @Captor
    private ArgumentCaptor<List<GameAchievement>> gameAchievementCaptor;

    @Captor
    private ArgumentCaptor<List<UserGameAchievement>> userAchievementCaptor;

    private static final String TEST_STEAM_ID = "76561198000000001";
    private static final Long TEST_APP_ID = 730L;
    private static final String CONECTOR_URL = "http://localhost:9002";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(achievementIngestionService, "conectorUrl", CONECTOR_URL);
        // Reset rate limit timer
        ReflectionTestUtils.setField(achievementIngestionService, "lastApiCallTime", 0L);
    }

    // Tests for ingestAchievementsForUserGames

    @Test
    void testIngestAchievementsForUserGames_AllGamesExistInDB() {
        // Arrange
        List<Long> appIds = Arrays.asList(730L, 570L, 440L);
        
        when(gameAchievementRepository.existsByAppId(anyLong())).thenReturn(true);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(anyString(), anyLong())).thenReturn(true);

        // Act
        achievementIngestionService.ingestAchievementsForUserGames(TEST_STEAM_ID, appIds);

        // Assert
        verify(gameAchievementRepository, times(3)).existsByAppId(anyLong());
        verify(userGameAchievementRepository, times(3)).existsBySteamIdAndAppId(eq(TEST_STEAM_ID), anyLong());
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    void testIngestAchievementsForUserGames_IngestNewSchema() {
        // Arrange
        List<Long> appIds = Arrays.asList(TEST_APP_ID);
        
        Map<String, Object> schemaResponse = createSchemaResponse();
        
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(false);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(false);
        when(restTemplate.getForObject(contains("/schema"), eq(Map.class))).thenReturn(schemaResponse);
        
        Map<String, Object> progressResponse = createProgressResponse(true);
        when(restTemplate.getForObject(contains("/achievements"), eq(Map.class))).thenReturn(progressResponse);
        when(userGameAchievementRepository.findBySteamIdAndAppId(anyString(), anyLong())).thenReturn(Collections.emptyList());

        // Act
        achievementIngestionService.ingestAchievementsForUserGames(TEST_STEAM_ID, appIds);

        // Assert
        // Schema is ingested once in ingestAchievementsForUserGames, then again when loading schema for user progress
        verify(gameAchievementRepository, atLeastOnce()).saveAll(gameAchievementCaptor.capture());
        List<GameAchievement> savedAchievements = gameAchievementCaptor.getValue();
        assertEquals(3, savedAchievements.size());
        assertEquals("ACH_1", savedAchievements.get(0).getAchievementName());
    }

    @Test
    void testIngestAchievementsForUserGames_EmptyAppIdList() {
        // Arrange
        List<Long> appIds = Collections.emptyList();

        // Act
        achievementIngestionService.ingestAchievementsForUserGames(TEST_STEAM_ID, appIds);

        // Assert
        verify(gameAchievementRepository, never()).existsByAppId(anyLong());
        verify(userGameAchievementRepository, never()).existsBySteamIdAndAppId(anyString(), anyLong());
    }

    @Test
    void testIngestAchievementsForUserGames_HandleErrors() {
        // Arrange
        List<Long> appIds = Arrays.asList(730L, 570L);
        
        when(gameAchievementRepository.existsByAppId(730L)).thenReturn(false);
        when(restTemplate.getForObject(contains("730"), eq(Map.class)))
                .thenThrow(new RuntimeException("API Error"));
        
        when(gameAchievementRepository.existsByAppId(570L)).thenReturn(true);
        when(userGameAchievementRepository.existsBySteamIdAndAppId(TEST_STEAM_ID, 570L)).thenReturn(true);

        // Act
        achievementIngestionService.ingestAchievementsForUserGames(TEST_STEAM_ID, appIds);

        // Assert - should continue processing despite error
        verify(gameAchievementRepository, times(2)).existsByAppId(anyLong());
    }

    // Tests for ingestGameAchievementSchema

    @Test
    void testIngestGameAchievementSchema_Success() {
        // Arrange
        Map<String, Object> response = createSchemaResponse();
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = achievementIngestionService.ingestGameAchievementSchema(TEST_APP_ID);

        // Assert
        assertTrue(result);
        verify(gameAchievementRepository, times(1)).saveAll(gameAchievementCaptor.capture());
        List<GameAchievement> saved = gameAchievementCaptor.getValue();
        assertEquals(3, saved.size());
        assertEquals(TEST_APP_ID, saved.get(0).getAppId());
        assertEquals("ACH_1", saved.get(0).getAchievementName());
        assertEquals("Achievement 1", saved.get(0).getDisplayName());
    }

    @Test
    void testIngestGameAchievementSchema_EmptyResponse() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        // Act
        boolean result = achievementIngestionService.ingestGameAchievementSchema(TEST_APP_ID);

        // Assert
        assertFalse(result);
        verify(gameAchievementRepository, never()).saveAll(anyList());
    }

    @Test
    void testIngestGameAchievementSchema_NoAchievements() {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> game = new HashMap<>();
        game.put("gameName", "Test Game");
        response.put("game", game);
        
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = achievementIngestionService.ingestGameAchievementSchema(TEST_APP_ID);

        // Assert
        assertFalse(result);
        verify(gameAchievementRepository, never()).saveAll(anyList());
    }

    @Test
    void testIngestGameAchievementSchema_ApiError() {
        // Arrange
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        // Act
        boolean result = achievementIngestionService.ingestGameAchievementSchema(TEST_APP_ID);

        // Assert
        assertFalse(result);
        verify(gameAchievementRepository, never()).saveAll(anyList());
    }

    // Tests for ingestUserAchievementProgress

    @Test
    void testIngestUserAchievementProgress_Success() {
        // Arrange
        Map<String, Object> response = createProgressResponse(true);
        
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID))
                .thenReturn(Collections.emptyList()) // Initial check
                .thenReturn(createSavedAchievements()); // After saving (verification)

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertTrue(result);
        // When initial list is empty, no deletion occurs
        verify(userGameAchievementRepository, times(1)).saveAll(userAchievementCaptor.capture());
        
        List<UserGameAchievement> saved = userAchievementCaptor.getValue();
        assertEquals(3, saved.size());
        assertTrue(saved.get(0).getAchieved()); // First one is unlocked
        assertFalse(saved.get(2).getAchieved()); // Third one is locked
    }

    @Test
    void testIngestUserAchievementProgress_LoadsSchemaIfNotExists() {
        // Arrange
        Map<String, Object> schemaResponse = createSchemaResponse();
        Map<String, Object> progressResponse = createProgressResponse(true);
        
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(false);
        when(restTemplate.getForObject(contains("/schema"), eq(Map.class))).thenReturn(schemaResponse);
        when(restTemplate.getForObject(contains("/achievements"), eq(Map.class))).thenReturn(progressResponse);
        when(userGameAchievementRepository.findBySteamIdAndAppId(anyString(), anyLong()))
                .thenReturn(Collections.emptyList())
                .thenReturn(createSavedAchievements());

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertTrue(result);
        verify(gameAchievementRepository, times(1)).saveAll(anyList()); // Schema saved
        verify(userGameAchievementRepository, times(1)).saveAll(anyList()); // Progress saved
    }

    @Test
    void testIngestUserAchievementProgress_DeletesExistingRecords() {
        // Arrange
        List<UserGameAchievement> existing = createSavedAchievements();
        Map<String, Object> response = createProgressResponse(true);
        
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID))
                .thenReturn(existing) // Initial check
                .thenReturn(createSavedAchievements()); // After refresh
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertTrue(result);
        verify(userGameAchievementRepository, times(1)).deleteBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID);
        verify(userGameAchievementRepository, atLeastOnce()).flush(); // Called after delete and after save
        verify(entityManager, times(1)).clear();
    }

    @Test
    void testIngestUserAchievementProgress_NoAchievementsInResponse() {
        // Arrange
        Map<String, Object> response = createProgressResponse(false);
        List<GameAchievement> gameAchievements = createGameAchievements();
        
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        when(gameAchievementRepository.findByAppId(TEST_APP_ID)).thenReturn(gameAchievements);
        when(userGameAchievementRepository.countBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID)).thenReturn(0L);

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertTrue(result);
        verify(userGameAchievementRepository, times(1)).saveAll(userAchievementCaptor.capture());
        List<UserGameAchievement> saved = userAchievementCaptor.getValue();
        assertTrue(saved.stream().allMatch(a -> !a.getAchieved())); // All locked
    }

    @Test
    void testIngestUserAchievementProgress_EmptyResponse() {
        // Arrange
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertFalse(result);
        verify(userGameAchievementRepository, never()).saveAll(anyList());
    }

    @Test
    void testIngestUserAchievementProgress_ApiException() {
        // Arrange
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(userGameAchievementRepository.findBySteamIdAndAppId(TEST_STEAM_ID, TEST_APP_ID))
                .thenReturn(Collections.emptyList());
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void testIngestUserAchievementProgress_UnlockTimeHandling() {
        // Arrange
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> playerstats = new HashMap<>();
        List<Map<String, Object>> achievements = new ArrayList<>();
        
        Map<String, Object> ach1 = new HashMap<>();
        ach1.put("apiname", "ACH_1");
        ach1.put("achieved", 1);
        ach1.put("unlocktime", 1638360000); // Integer unlock time
        achievements.add(ach1);
        
        Map<String, Object> ach2 = new HashMap<>();
        ach2.put("apiname", "ACH_2");
        ach2.put("achieved", 1);
        ach2.put("unlocktime", 1638370000L); // Long unlock time
        achievements.add(ach2);
        
        playerstats.put("achievements", achievements);
        response.put("playerstats", playerstats);
        
        when(gameAchievementRepository.existsByAppId(TEST_APP_ID)).thenReturn(true);
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        when(userGameAchievementRepository.findBySteamIdAndAppId(anyString(), anyLong()))
                .thenReturn(Collections.emptyList())
                .thenReturn(createSavedAchievements());

        // Act
        boolean result = achievementIngestionService.ingestUserAchievementProgress(TEST_STEAM_ID, TEST_APP_ID);

        // Assert
        assertTrue(result);
        verify(userGameAchievementRepository, times(1)).saveAll(userAchievementCaptor.capture());
        List<UserGameAchievement> saved = userAchievementCaptor.getValue();
        assertEquals(2, saved.size());
        assertNotNull(saved.get(0).getUnlockTime());
        assertNotNull(saved.get(1).getUnlockTime());
    }

    // Helper methods
    private Map<String, Object> createSchemaResponse() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> game = new HashMap<>();
        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> achievements = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> ach = new HashMap<>();
            ach.put("name", "ACH_" + i);
            ach.put("displayName", "Achievement " + i);
            ach.put("description", "Description " + i);
            ach.put("icon", "http://icon" + i + ".jpg");
            ach.put("icongray", "http://icongray" + i + ".jpg");
            ach.put("hidden", 0);
            achievements.add(ach);
        }
        
        stats.put("achievements", achievements);
        game.put("availableGameStats", stats);
        response.put("game", game);
        
        return response;
    }

    private Map<String, Object> createProgressResponse(boolean includeAchievements) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> playerstats = new HashMap<>();
        
        if (includeAchievements) {
            List<Map<String, Object>> achievements = new ArrayList<>();
            
            Map<String, Object> ach1 = new HashMap<>();
            ach1.put("apiname", "ACH_1");
            ach1.put("achieved", 1);
            ach1.put("unlocktime", 1638360000);
            achievements.add(ach1);
            
            Map<String, Object> ach2 = new HashMap<>();
            ach2.put("apiname", "ACH_2");
            ach2.put("achieved", 1);
            ach2.put("unlocktime", 1638370000);
            achievements.add(ach2);
            
            Map<String, Object> ach3 = new HashMap<>();
            ach3.put("apiname", "ACH_3");
            ach3.put("achieved", 0);
            ach3.put("unlocktime", 0);
            achievements.add(ach3);
            
            playerstats.put("achievements", achievements);
        }
        
        response.put("playerstats", playerstats);
        return response;
    }

    private List<GameAchievement> createGameAchievements() {
        List<GameAchievement> achievements = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            achievements.add(GameAchievement.builder()
                    .id((long) i)
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

    private List<UserGameAchievement> createSavedAchievements() {
        List<UserGameAchievement> achievements = new ArrayList<>();
        achievements.add(UserGameAchievement.builder()
                .id(1L)
                .steamId(TEST_STEAM_ID)
                .appId(TEST_APP_ID)
                .achievementName("ACH_1")
                .achieved(true)
                .unlockTime(1638360000L)
                .createdAt(LocalDateTime.now())
                .build());
        achievements.add(UserGameAchievement.builder()
                .id(2L)
                .steamId(TEST_STEAM_ID)
                .appId(TEST_APP_ID)
                .achievementName("ACH_2")
                .achieved(true)
                .unlockTime(1638370000L)
                .createdAt(LocalDateTime.now())
                .build());
        achievements.add(UserGameAchievement.builder()
                .id(3L)
                .steamId(TEST_STEAM_ID)
                .appId(TEST_APP_ID)
                .achievementName("ACH_3")
                .achieved(false)
                .unlockTime(null)
                .createdAt(LocalDateTime.now())
                .build());
        return achievements;
    }
}
