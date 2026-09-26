package com.dacs.backend.service.impl;

import com.dacs.backend.dto.UserNewsResponseDto;
import com.dacs.backend.entity.UserNewsCache;
import com.dacs.backend.repository.UserNewsCacheRepository;
import com.dacs.backend.service.UserNewsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for UserNewsServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class UserNewsServiceImplTest {

    @Mock
    private UserNewsCacheRepository newsCacheRepository;

    @InjectMocks
    private UserNewsServiceImpl userNewsService;

    @Captor
    private ArgumentCaptor<UserNewsCache> newsCacheCaptor;

    private static final String TEST_STEAM_ID = "76561198000000001";
    private static final Long TEST_APP_ID_1 = 730L;
    private static final Long TEST_APP_ID_2 = 570L;

    private List<UserNewsService.GameNewsInput> gamesWithNews;

    @BeforeEach
    void setUp() {
        gamesWithNews = new ArrayList<>();
    }

    @Test
    void testProcessUserNews_EmptyNewsList() {
        // Arrange
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_STEAM_ID, result.getSteamId());
        assertEquals(0, result.getTotalGames());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getPageSize());
        assertFalse(result.getHasMore());
        assertTrue(result.getGamesNews().isEmpty());
    }

    @Test
    void testProcessUserNews_AllNewNews() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "CS2", 3));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        assertEquals(1, result.getTotalGames());
        assertEquals(1, result.getGamesNews().size());
        
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        assertEquals(TEST_APP_ID_1, gameNews.getAppId());
        assertEquals("CS2", gameNews.getGameName());
        assertEquals(3, gameNews.getTotalNewsCount());
        assertEquals(2, gameNews.getLatestNews().size()); // Latest 2
        assertEquals(1, gameNews.getOlderNews().size());  // Remaining 1
        
        // All should be marked as new
        assertTrue(gameNews.getLatestNews().stream().allMatch(UserNewsResponseDto.GameNewsDto.NewsItemDto::getIsNew));
        assertTrue(gameNews.getOlderNews().stream().allMatch(UserNewsResponseDto.GameNewsDto.NewsItemDto::getIsNew));
        
        // Verify cache saves
        verify(newsCacheRepository, times(3)).save(any(UserNewsCache.class));
    }

    @Test
    void testProcessUserNews_MixedNewAndSeenNews() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "CS2", 3));
        
        Set<String> seenGids = new HashSet<>(Arrays.asList("gid_0")); // First news is seen
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(new ArrayList<>(seenGids));
        when(newsCacheRepository.findBySteamIdAndNewsGid(eq(TEST_STEAM_ID), eq("gid_0")))
                .thenReturn(Optional.of(createCachedNews("gid_0")));

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        List<UserNewsResponseDto.GameNewsDto.NewsItemDto> allNews = new ArrayList<>();
        allNews.addAll(gameNews.getLatestNews());
        allNews.addAll(gameNews.getOlderNews());
        
        // First news (gid_0) should not be new
        assertFalse(allNews.get(0).getIsNew());
        // Others should be new
        assertTrue(allNews.get(1).getIsNew());
        assertTrue(allNews.get(2).getIsNew());
        
        // Verify saves: 2 new saves (gid_1, gid_2) + 1 update for gid_0
        verify(newsCacheRepository, times(3)).save(any(UserNewsCache.class));
        verify(newsCacheRepository, times(1)).findBySteamIdAndNewsGid(TEST_STEAM_ID, "gid_0");
    }

    @Test
    void testProcessUserNews_Pagination() {
        // Arrange
        for (int i = 0; i < 5; i++) {
            gamesWithNews.add(createGameWithNews((long) (730 + i), "Game" + i, 2));
        }
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act - First page
        UserNewsResponseDto result1 = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 2);

        // Assert
        assertEquals(5, result1.getTotalGames());
        assertEquals(2, result1.getGamesNews().size());
        assertEquals(0, result1.getPage());
        assertTrue(result1.getHasMore());

        // Act - Second page
        UserNewsResponseDto result2 = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 1, 2);

        // Assert
        assertEquals(5, result2.getTotalGames());
        assertEquals(2, result2.getGamesNews().size());
        assertEquals(1, result2.getPage());
        assertTrue(result2.getHasMore());

        // Act - Last page
        UserNewsResponseDto result3 = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 2, 2);

        // Assert
        assertEquals(5, result3.getTotalGames());
        assertEquals(1, result3.getGamesNews().size());
        assertEquals(2, result3.getPage());
        assertFalse(result3.getHasMore());
    }

    @Test
    void testProcessUserNews_SortedByLatestNewsDate() {
        // Arrange
        UserNewsService.GameNewsInput game1 = createGameWithNews(TEST_APP_ID_1, "Game1", 2);
        game1.news.get(0).date = 1000000L; // Older
        
        UserNewsService.GameNewsInput game2 = createGameWithNews(TEST_APP_ID_2, "Game2", 2);
        game2.news.get(0).date = 2000000L; // Newer
        
        gamesWithNews.add(game1);
        gamesWithNews.add(game2);
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        assertEquals(2, result.getGamesNews().size());
        // Game2 should be first (newer news)
        assertEquals(TEST_APP_ID_2, result.getGamesNews().get(0).getAppId());
        assertEquals(TEST_APP_ID_1, result.getGamesNews().get(1).getAppId());
    }

    @Test
    void testProcessUserNews_EmptyNewsForGame() {
        // Arrange
        UserNewsService.GameNewsInput gameWithNoNews = new UserNewsService.GameNewsInput();
        gameWithNoNews.appId = TEST_APP_ID_1;
        gameWithNoNews.gameName = "Game1";
        gameWithNoNews.news = Collections.emptyList();
        
        gamesWithNews.add(gameWithNoNews);
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        assertEquals(0, result.getTotalGames());
        assertEquals(0, result.getGamesNews().size());
    }

    @Test
    void testProcessUserNews_NullNewsForGame() {
        // Arrange
        UserNewsService.GameNewsInput gameWithNullNews = new UserNewsService.GameNewsInput();
        gameWithNullNews.appId = TEST_APP_ID_1;
        gameWithNullNews.gameName = "Game1";
        gameWithNullNews.news = null;
        
        gamesWithNews.add(gameWithNullNews);
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        assertEquals(0, result.getTotalGames());
        assertEquals(0, result.getGamesNews().size());
    }

    @Test
    void testProcessUserNews_VerifyGameImageUrl() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "CS2", 1));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        String expectedUrl = String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/library_600x900.jpg", TEST_APP_ID_1);
        assertEquals(expectedUrl, gameNews.getGameImageUrl());
    }

    @Test
    void testProcessUserNews_NewsSortedByDateDescending() {
        // Arrange
        UserNewsService.GameNewsInput game = new UserNewsService.GameNewsInput();
        game.appId = TEST_APP_ID_1;
        game.gameName = "Game1";
        game.news = new ArrayList<>();
        
        // Add news in random order
        game.news.add(createNewsItem("gid_2", 2000000L));
        game.news.add(createNewsItem("gid_1", 3000000L)); // Newest
        game.news.add(createNewsItem("gid_3", 1000000L)); // Oldest
        
        gamesWithNews.add(game);
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        List<UserNewsResponseDto.GameNewsDto.NewsItemDto> allNews = new ArrayList<>();
        allNews.addAll(gameNews.getLatestNews());
        allNews.addAll(gameNews.getOlderNews());
        
        assertEquals("gid_1", allNews.get(0).getGid()); // Newest first
        assertEquals("gid_2", allNews.get(1).getGid());
        assertEquals("gid_3", allNews.get(2).getGid()); // Oldest last
    }

    // Tests for markNewsAsSeen

    @Test
    void testMarkNewsAsSeen_NewNews() {
        // Arrange
        List<UserNewsService.NewsToMark> newsToMark = Arrays.asList(
                createNewsToMark(TEST_APP_ID_1, "gid_1", 1000000L),
                createNewsToMark(TEST_APP_ID_1, "gid_2", 1000100L)
        );
        
        when(newsCacheRepository.findBySteamIdAndAppIdAndNewsGid(anyString(), anyLong(), anyString()))
                .thenReturn(Optional.empty());

        // Act
        userNewsService.markNewsAsSeen(TEST_STEAM_ID, newsToMark);

        // Assert
        verify(newsCacheRepository, times(2)).save(newsCacheCaptor.capture());
        List<UserNewsCache> saved = newsCacheCaptor.getAllValues();
        assertEquals(TEST_STEAM_ID, saved.get(0).getSteamId());
        assertEquals("gid_1", saved.get(0).getNewsGid());
        assertEquals("gid_2", saved.get(1).getNewsGid());
    }

    @Test
    void testMarkNewsAsSeen_ExistingNews() {
        // Arrange
        List<UserNewsService.NewsToMark> newsToMark = Arrays.asList(
                createNewsToMark(TEST_APP_ID_1, "gid_1", 1000000L)
        );
        
        when(newsCacheRepository.findBySteamIdAndAppIdAndNewsGid(TEST_STEAM_ID, TEST_APP_ID_1, "gid_1"))
                .thenReturn(Optional.of(createCachedNews("gid_1")));

        // Act
        userNewsService.markNewsAsSeen(TEST_STEAM_ID, newsToMark);

        // Assert
        verify(newsCacheRepository, never()).save(any());
    }

    @Test
    void testMarkNewsAsSeen_EmptyList() {
        // Act
        userNewsService.markNewsAsSeen(TEST_STEAM_ID, Collections.emptyList());

        // Assert
        verify(newsCacheRepository, never()).findBySteamIdAndAppIdAndNewsGid(anyString(), anyLong(), anyString());
        verify(newsCacheRepository, never()).save(any());
    }

    // Tests for cleanOldCache

    @Test
    void testCleanOldCache() {
        // Act
        userNewsService.cleanOldCache();

        // Assert
        verify(newsCacheRepository, times(1)).deleteByLastCheckedAtBefore(any(LocalDateTime.class));
    }

    @Test
    void testCleanOldCache_Verify30DayCutoff() {
        // Arrange
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // Act
        userNewsService.cleanOldCache();

        // Assert
        verify(newsCacheRepository, times(1)).deleteByLastCheckedAtBefore(dateCaptor.capture());
        LocalDateTime cutoffDate = dateCaptor.getValue();
        
        // Verify it's approximately 30 days ago (with 1 hour tolerance)
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(30);
        assertTrue(Math.abs(java.time.Duration.between(cutoffDate, expectedCutoff).toHours()) < 1);
    }

    @Test
    void testProcessUserNews_LastNewsDateSet() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "Game1", 2));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        assertNotNull(gameNews.getLastNewsDate());
        // Should be the date of the first (newest) news item
        assertEquals(gamesWithNews.get(0).news.get(0).date, gameNews.getLastNewsDate());
    }

    @Test
    void testProcessUserNews_PageOutOfBounds() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "Game1", 1));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 10, 10);

        // Assert
        assertEquals(1, result.getTotalGames());
        assertEquals(0, result.getGamesNews().size());
        assertEquals(10, result.getPage());
        assertFalse(result.getHasMore());
    }

    @Test
    void testProcessUserNews_SingleNewsItem() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "Game1", 1));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        assertEquals(1, gameNews.getLatestNews().size());
        assertEquals(0, gameNews.getOlderNews().size());
        assertEquals(1, gameNews.getTotalNewsCount());
    }

    @Test
    void testProcessUserNews_ExactlyTwoNewsItems() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "Game1", 2));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        assertEquals(2, gameNews.getLatestNews().size());
        assertEquals(0, gameNews.getOlderNews().size());
        assertEquals(2, gameNews.getTotalNewsCount());
    }

    @Test
    void testProcessUserNews_ManyNewsItems() {
        // Arrange
        gamesWithNews.add(createGameWithNews(TEST_APP_ID_1, "Game1", 10));
        
        when(newsCacheRepository.findAllNewsGidsBySteamId(TEST_STEAM_ID)).thenReturn(Collections.emptyList());

        // Act
        UserNewsResponseDto result = userNewsService.processUserNews(TEST_STEAM_ID, gamesWithNews, 0, 10);

        // Assert
        UserNewsResponseDto.GameNewsDto gameNews = result.getGamesNews().get(0);
        assertEquals(2, gameNews.getLatestNews().size());
        assertEquals(8, gameNews.getOlderNews().size());
        assertEquals(10, gameNews.getTotalNewsCount());
    }

    // Helper methods
    private UserNewsService.GameNewsInput createGameWithNews(Long appId, String gameName, int newsCount) {
        UserNewsService.GameNewsInput game = new UserNewsService.GameNewsInput();
        game.appId = appId;
        game.gameName = gameName;
        game.news = new ArrayList<>();
        
        for (int i = 0; i < newsCount; i++) {
            UserNewsService.GameNewsInput.NewsItemInput newsItem = createNewsItem("gid_" + i, 1000000L - (i * 1000));
            game.news.add(newsItem);
        }
        
        return game;
    }

    private UserNewsService.GameNewsInput.NewsItemInput createNewsItem(String gid, Long date) {
        UserNewsService.GameNewsInput.NewsItemInput newsItem = new UserNewsService.GameNewsInput.NewsItemInput();
        newsItem.gid = gid;
        newsItem.title = "News " + gid;
        newsItem.url = "http://news.url/" + gid;
        newsItem.isExternalUrl = false;
        newsItem.author = "Author";
        newsItem.contents = "Content";
        newsItem.feedLabel = "Feed";
        newsItem.date = date;
        newsItem.feedName = "Feed Name";
        newsItem.feedType = 1;
        newsItem.appId = TEST_APP_ID_1;
        return newsItem;
    }

    private UserNewsService.NewsToMark createNewsToMark(Long appId, String gid, Long date) {
        UserNewsService.NewsToMark newsToMark = new UserNewsService.NewsToMark();
        newsToMark.appId = appId;
        newsToMark.gid = gid;
        newsToMark.date = date;
        return newsToMark;
    }

    private UserNewsCache createCachedNews(String gid) {
        return UserNewsCache.builder()
                .id(1L)
                .steamId(TEST_STEAM_ID)
                .appId(TEST_APP_ID_1)
                .newsGid(gid)
                .newsDate(1000000L)
                .firstSeenAt(LocalDateTime.now().minusDays(1))
                .lastCheckedAt(LocalDateTime.now())
                .build();
    }
}
