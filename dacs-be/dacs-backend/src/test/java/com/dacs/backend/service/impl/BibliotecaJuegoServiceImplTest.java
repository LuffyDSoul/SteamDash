package com.dacs.backend.service.impl;

import com.dacs.backend.dto.BibliotecaJuegoDto;
import com.dacs.backend.entity.GameRecord;
import com.dacs.backend.repository.GameRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BibliotecaJuegoServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class BibliotecaJuegoServiceImplTest {

    @Mock
    private GameRecordRepository gameRecordRepository;

    @Mock
    private GameRecordServiceImpl gameRecordService;

    @InjectMocks
    private BibliotecaJuegoServiceImpl bibliotecaJuegoService;

    private GameRecord testGameRecord;
    private static final Long TEST_APP_ID = 730L;

    @BeforeEach
    void setUp() {
        // Set default values for @Value fields
        ReflectionTestUtils.setField(bibliotecaJuegoService, "freePriceFormat", "Gratuito");
        ReflectionTestUtils.setField(bibliotecaJuegoService, "missingPriceFormat", "N/A");

        testGameRecord = new GameRecord();
        testGameRecord.setAppId(TEST_APP_ID);
        testGameRecord.setName("Counter-Strike 2");
        testGameRecord.setHeaderImage("https://cdn.akamai.steamstatic.com/steam/apps/730/header.jpg");
        testGameRecord.setImgVertical("https://cdn.akamai.steamstatic.com/steam/apps/730/library_600x900.jpg");
        testGameRecord.setImgIconUrl("https://media.steampowered.com/steamcommunity/public/images/apps/730/icon.jpg");
        testGameRecord.setIsFree(true);
        testGameRecord.setPrice("Gratuito");
        testGameRecord.setTags(Arrays.asList("Action", "FPS", "Multiplayer"));
    }

    @Test
    void testObtenerJuego_RefreshSuccessful() {
        // Arrange
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertEquals("Counter-Strike 2", result.getName());
        assertEquals("Gratuito", result.getPrice());
        assertTrue(result.getIsFree());
        assertEquals(3, result.getTags().size());
        assertEquals("Action", result.getTags().get(0).getTag());
        verify(gameRecordService, times(1)).refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull());
        verify(gameRecordRepository, never()).findByAppId(anyLong());
    }

    @Test
    void testObtenerJuego_RefreshFailsFallbackToDatabase() {
        // Arrange
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenThrow(new RuntimeException("External API error"));
        when(gameRecordRepository.findByAppId(TEST_APP_ID))
                .thenReturn(Optional.of(testGameRecord));

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertEquals("Counter-Strike 2", result.getName());
        verify(gameRecordService, times(1)).refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull());
        verify(gameRecordRepository, times(1)).findByAppId(TEST_APP_ID);
    }

    @Test
    void testObtenerJuego_RefreshFailsNoDatabaseEntry() {
        // Arrange
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenThrow(new RuntimeException("External API error"));
        when(gameRecordRepository.findByAppId(TEST_APP_ID))
                .thenReturn(Optional.empty());

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertNull(result.getName());
        assertEquals("N/A", result.getPrice());
        assertNull(result.getIsFree());
        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/730/library_600x900.jpg", result.getImgVertical());
        assertEquals("https://store.steampowered.com/app/730", result.getStoreUrl());
        assertTrue(result.getTags().isEmpty());
        verify(gameRecordService, times(1)).refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull());
        verify(gameRecordRepository, times(1)).findByAppId(TEST_APP_ID);
    }

    @Test
    void testObtenerJuego_NullAppId() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bibliotecaJuegoService.obtenerJuego(null);
        });

        assertEquals("appId es requerido", exception.getMessage());
        verify(gameRecordService, never()).refreshAndUpsert(anyLong(), any(), any());
        verify(gameRecordRepository, never()).findByAppId(anyLong());
    }

    @Test
    void testObtenerJuego_WithNullTags() {
        // Arrange
        testGameRecord.setTags(null);
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void testObtenerJuego_WithEmptyTags() {
        // Arrange
        testGameRecord.setTags(new ArrayList<>());
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void testObtenerJuego_PaidGame() {
        // Arrange
        testGameRecord.setIsFree(false);
        testGameRecord.setPrice("$19.99 USD");
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertEquals("$19.99 USD", result.getPrice());
        assertFalse(result.getIsFree());
    }

    @Test
    void testObtenerJuego_StoreUrlFormat() {
        // Arrange
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals("https://store.steampowered.com/app/730", result.getStoreUrl());
    }

    @Test
    void testMapToDto_AllFieldsPresent() {
        // Arrange
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP_ID, result.getAppId());
        assertEquals("Counter-Strike 2", result.getName());
        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/730/header.jpg", result.getHeaderImage());
        assertEquals("https://cdn.akamai.steamstatic.com/steam/apps/730/library_600x900.jpg", result.getImgVertical());
        assertEquals("https://media.steampowered.com/steamcommunity/public/images/apps/730/icon.jpg", result.getImgIconUrl());
        assertTrue(result.getIsFree());
        assertEquals("Gratuito", result.getPrice());
        assertEquals("https://store.steampowered.com/app/730", result.getStoreUrl());
        assertNotNull(result.getTags());
        assertEquals(3, result.getTags().size());
    }

    @Test
    void testMapToDto_VerifyTagsMapping() {
        // Arrange
        List<String> tagsList = Arrays.asList("Action", "FPS", "Multiplayer", "Shooter");
        testGameRecord.setTags(tagsList);
        when(gameRecordService.refreshAndUpsert(eq(TEST_APP_ID), isNull(), isNull()))
                .thenReturn(testGameRecord);

        // Act
        BibliotecaJuegoDto result = bibliotecaJuegoService.obtenerJuego(TEST_APP_ID);

        // Assert
        assertNotNull(result.getTags());
        assertEquals(4, result.getTags().size());
        assertEquals("Action", result.getTags().get(0).getTag());
        assertEquals("FPS", result.getTags().get(1).getTag());
        assertEquals("Multiplayer", result.getTags().get(2).getTag());
        assertEquals("Shooter", result.getTags().get(3).getTag());
    }
}
