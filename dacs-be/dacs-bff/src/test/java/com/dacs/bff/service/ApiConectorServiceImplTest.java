package com.dacs.bff.service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.dto.*;
import com.dacs.bff.exeption.BffException;
import com.dacs.bff.exeption.ConectorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ApiConectorServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class ApiConectorServiceImplTest {

    @Mock
    private ApiConectorClient apiConectorClient;

    @InjectMocks
    private ApiConectorServiceImpl apiConectorService;

    private static final String TEST_APP_ID = "730";
    private static final String TEST_STEAM_ID = "76561198000000000";

    // ==================== PING TESTS ====================

    @Test
    void testPing_Success() {
        // Arrange
        when(apiConectorClient.ping()).thenReturn("pong");

        // Act
        String result = apiConectorService.ping();

        // Assert
        assertEquals("pong", result);
        verify(apiConectorClient, times(1)).ping();
    }

    // ==================== GET STEAM GAME DETAILS TESTS ====================

    @Test
    void testGetSteamGameDetails_Success() {
        // Arrange
        SteamGameDto gameDto = new SteamGameDto();
        gameDto.setName("Counter-Strike 2");
        gameDto.setSteamAppId(730L);

        when(apiConectorClient.getSteamGameDetails(eq(TEST_APP_ID))).thenReturn(gameDto);

        // Act
        SteamGameDto result = apiConectorService.getSteamGameDetails(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals("Counter-Strike 2", result.getName());
        assertEquals(730L, result.getSteamAppId());
        verify(apiConectorClient, times(1)).getSteamGameDetails(eq(TEST_APP_ID));
    }

    @Test
    void testGetSteamGameDetails_NullAppId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getSteamGameDetails(null);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetSteamGameDetails_EmptyAppId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getSteamGameDetails("");
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetSteamGameDetails_BlankAppId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getSteamGameDetails("   ");
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetSteamGameDetails_ClientThrowsException_WrapsInConectorException() {
        // Arrange
        when(apiConectorClient.getSteamGameDetails(eq(TEST_APP_ID)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(ConectorException.class, () -> {
            apiConectorService.getSteamGameDetails(TEST_APP_ID);
        });
    }

    // ==================== GET USER OWNED GAMES TESTS ====================

    @Test
    void testGetUserOwnedGames_Success() {
        // Arrange
        SteamOwnedGamesResponseDto response = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse ownedGames = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        ownedGames.setGameCount(150);
        response.setResponse(ownedGames);

        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenReturn(response);

        // Act
        SteamOwnedGamesResponseDto result = apiConectorService.getUserOwnedGames(TEST_STEAM_ID, true, true);

        // Assert
        assertNotNull(result);
        assertEquals(150, result.getResponse().getGameCount());
        verify(apiConectorClient, times(1)).getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true));
    }

    @Test
    void testGetUserOwnedGames_WithFalseFlags_Success() {
        // Arrange
        SteamOwnedGamesResponseDto response = new SteamOwnedGamesResponseDto();
        SteamOwnedGamesResponseDto.OwnedGamesResponse ownedGames = new SteamOwnedGamesResponseDto.OwnedGamesResponse();
        response.setResponse(ownedGames);

        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(false), eq(false)))
                .thenReturn(response);

        // Act
        SteamOwnedGamesResponseDto result = apiConectorService.getUserOwnedGames(TEST_STEAM_ID, false, false);

        // Assert
        assertNotNull(result);
        verify(apiConectorClient, times(1)).getUserOwnedGames(eq(TEST_STEAM_ID), eq(false), eq(false));
    }

    @Test
    void testGetUserOwnedGames_NullSteamId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getUserOwnedGames(null, true, true);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetUserOwnedGames_EmptySteamId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getUserOwnedGames("", true, true);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetUserOwnedGames_BlankSteamId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getUserOwnedGames("   ", true, true);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetUserOwnedGames_ClientThrowsException_WrapsInConectorException() {
        // Arrange
        when(apiConectorClient.getUserOwnedGames(eq(TEST_STEAM_ID), eq(true), eq(true)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(ConectorException.class, () -> {
            apiConectorService.getUserOwnedGames(TEST_STEAM_ID, true, true);
        });
    }

    // ==================== GET NEWS FOR APP TESTS ====================

    @Test
    void testGetNewsForApp_Success() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        
        List<SteamNewsResponseDto.NewsItemDto> newsItems = new ArrayList<>();
        SteamNewsResponseDto.NewsItemDto newsItem = new SteamNewsResponseDto.NewsItemDto();
        newsItem.setTitle("Test News");
        newsItem.setContents("Test content");
        newsItem.setUrl("http://test.com");
        newsItem.setDate(1638360000L);
        newsItems.add(newsItem);
        
        appNews.setNewsItems(newsItems);
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test News", result.get(0).getTitle());
        assertEquals("Test content", result.get(0).getContents());
        assertEquals("http://test.com", result.get(0).getUrl());
        assertNotNull(result.get(0).getDateFormatted());
        verify(apiConectorClient, times(1)).getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300));
    }

    @Test
    void testGetNewsForApp_WithContentsContainingSteamClanImage_FiltersOutImageUrls() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        
        List<SteamNewsResponseDto.NewsItemDto> newsItems = new ArrayList<>();
        SteamNewsResponseDto.NewsItemDto newsItem = new SteamNewsResponseDto.NewsItemDto();
        newsItem.setTitle("Test News");
        newsItem.setContents("Some text {STEAM_CLAN_IMAGE}/12345/abc123.jpg more text");
        newsItem.setUrl("http://test.com");
        newsItem.setDate(1638360000L);
        newsItems.add(newsItem);
        
        appNews.setNewsItems(newsItems);
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Some text  more text", result.get(0).getContents());
    }

    @Test
    void testGetNewsForApp_WithNullContents_HandlesGracefully() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        
        List<SteamNewsResponseDto.NewsItemDto> newsItems = new ArrayList<>();
        SteamNewsResponseDto.NewsItemDto newsItem = new SteamNewsResponseDto.NewsItemDto();
        newsItem.setTitle("Test News");
        newsItem.setContents(null);
        newsItem.setUrl("http://test.com");
        newsItems.add(newsItem);
        
        appNews.setNewsItems(newsItems);
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getContents());
    }

    @Test
    void testGetNewsForApp_NullResponse_ReturnsNull() {
        // Arrange
        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(null);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetNewsForApp_NullAppNews_ReturnsNull() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        newsResponse.setAppNews(null);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetNewsForApp_NullNewsItems_ReturnsNull() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        appNews.setNewsItems(null);
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetNewsForApp_EmptyNewsItems_ReturnsEmptyList() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        appNews.setNewsItems(Collections.emptyList());
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetNewsForApp_NullAppId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getNewsForApp(null, 10, 300);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetNewsForApp_EmptyAppId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getNewsForApp("", 10, 300);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetNewsForApp_BlankAppId_ThrowsException() {
        // Act & Assert
        assertThrows(BffException.class, () -> {
            apiConectorService.getNewsForApp("   ", 10, 300);
        });

        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testGetNewsForApp_ClientThrowsException_WrapsInConectorException() {
        // Arrange
        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenThrow(new RuntimeException("API Error"));

        // Act & Assert
        assertThrows(ConectorException.class, () -> {
            apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);
        });
    }

    @Test
    void testGetNewsForApp_WithNullDate_DoesNotSetDateFormatted() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        
        List<SteamNewsResponseDto.NewsItemDto> newsItems = new ArrayList<>();
        SteamNewsResponseDto.NewsItemDto newsItem = new SteamNewsResponseDto.NewsItemDto();
        newsItem.setTitle("Test News");
        newsItem.setContents("Test content");
        newsItem.setUrl("http://test.com");
        newsItem.setDate(null);
        newsItems.add(newsItem);
        
        appNews.setNewsItems(newsItems);
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getDate());
        assertNull(result.get(0).getDateFormatted());
    }

    @Test
    void testGetNewsForApp_MultipleNewsItems_ConvertsAll() {
        // Arrange
        SteamNewsResponseDto newsResponse = new SteamNewsResponseDto();
        SteamNewsResponseDto.AppNewsDto appNews = new SteamNewsResponseDto.AppNewsDto();
        
        List<SteamNewsResponseDto.NewsItemDto> newsItems = new ArrayList<>();
        
        SteamNewsResponseDto.NewsItemDto newsItem1 = new SteamNewsResponseDto.NewsItemDto();
        newsItem1.setTitle("News 1");
        newsItem1.setContents("Content 1");
        newsItem1.setUrl("http://test1.com");
        newsItem1.setDate(1638360000L);
        newsItems.add(newsItem1);
        
        SteamNewsResponseDto.NewsItemDto newsItem2 = new SteamNewsResponseDto.NewsItemDto();
        newsItem2.setTitle("News 2");
        newsItem2.setContents("Content 2");
        newsItem2.setUrl("http://test2.com");
        newsItem2.setDate(1638370000L);
        newsItems.add(newsItem2);
        
        appNews.setNewsItems(newsItems);
        newsResponse.setAppNews(appNews);

        when(apiConectorClient.getNewsForApp(eq(TEST_APP_ID), eq(10), eq(300)))
                .thenReturn(newsResponse);

        // Act
        List<NewsViewDto> result = apiConectorService.getNewsForApp(TEST_APP_ID, 10, 300);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("News 1", result.get(0).getTitle());
        assertEquals("News 2", result.get(1).getTitle());
    }
}
