package com.dacs.bff.service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.dto.SteamResolveVanityResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SteamIdResolverService
 */
@ExtendWith(MockitoExtension.class)
class SteamIdResolverServiceTest {

    @Mock
    private ApiConectorClient apiConectorClient;

    @InjectMocks
    private SteamIdResolverService steamIdResolverService;

    private static final String VALID_STEAM_ID = "76561198000000000";
    private static final String VANITY_URL = "testuser";

    // ==================== RESOLVE WITH STEAM ID64 TESTS ====================

    @Test
    void testResolve_WithValidSteamId64_ReturnsSameId() {
        // Act
        String result = steamIdResolverService.resolve(VALID_STEAM_ID);

        // Assert
        assertEquals(VALID_STEAM_ID, result);
        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testResolve_WithDifferentValidSteamId64_ReturnsSameId() {
        // Arrange
        String steamId = "76561198123456789";

        // Act
        String result = steamIdResolverService.resolve(steamId);

        // Assert
        assertEquals(steamId, result);
        verifyNoInteractions(apiConectorClient);
    }

    // ==================== RESOLVE WITH VANITY URL TESTS ====================

    @Test
    void testResolve_WithVanityUrl_ReturnsResolvedSteamId() {
        // Arrange
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(1);
        innerResponse.setSteamid(VALID_STEAM_ID);
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(VANITY_URL))).thenReturn(response);

        // Act
        String result = steamIdResolverService.resolve(VANITY_URL);

        // Assert
        assertEquals(VALID_STEAM_ID, result);
        verify(apiConectorClient, times(1)).resolveVanity(eq(VANITY_URL));
    }

    @Test
    void testResolve_WithVanityUrlNotFound_ThrowsException() {
        // Arrange
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(42); // 42 means not found
        innerResponse.setMessage("No match");
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(VANITY_URL))).thenReturn(response);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve(VANITY_URL);
        });

        assertEquals("No match", exception.getMessage());
        verify(apiConectorClient, times(1)).resolveVanity(eq(VANITY_URL));
    }

    @Test
    void testResolve_WithVanityUrlNullResponse_ThrowsException() {
        // Arrange
        when(apiConectorClient.resolveVanity(eq(VANITY_URL))).thenReturn(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve(VANITY_URL);
        });

        assertTrue(exception.getMessage().contains("No se pudo resolver vanity URL"));
        verify(apiConectorClient, times(1)).resolveVanity(eq(VANITY_URL));
    }

    @Test
    void testResolve_WithVanityUrlNullInnerResponse_ThrowsException() {
        // Arrange
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        response.setResponse(null);

        when(apiConectorClient.resolveVanity(eq(VANITY_URL))).thenReturn(response);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve(VANITY_URL);
        });

        assertTrue(exception.getMessage().contains("No se pudo resolver vanity URL"));
    }

    @Test
    void testResolve_WithVanityUrlEmptySteamId_ThrowsException() {
        // Arrange
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(1);
        innerResponse.setSteamid("");
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(VANITY_URL))).thenReturn(response);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve(VANITY_URL);
        });

        assertTrue(exception.getMessage().contains("No se pudo resolver vanity URL"));
    }

    // ==================== NULL AND EMPTY INPUT TESTS ====================

    @Test
    void testResolve_WithNullInput_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve(null);
        });

        assertEquals("steamId/vanity no provisto", exception.getMessage());
        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testResolve_WithEmptyString_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve("");
        });

        assertEquals("steamId/vanity no provisto", exception.getMessage());
        verifyNoInteractions(apiConectorClient);
    }

    @Test
    void testResolve_WithBlankString_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve("   ");
        });

        assertEquals("steamId/vanity no provisto", exception.getMessage());
        verifyNoInteractions(apiConectorClient);
    }

    // ==================== EDGE CASES TESTS ====================

    @Test
    void testResolve_WithShortNumericString_CallsApi() {
        // Arrange (not 17 digits)
        String shortId = "1234567890";
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(1);
        innerResponse.setSteamid(VALID_STEAM_ID);
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(shortId))).thenReturn(response);

        // Act
        String result = steamIdResolverService.resolve(shortId);

        // Assert
        assertEquals(VALID_STEAM_ID, result);
        verify(apiConectorClient, times(1)).resolveVanity(eq(shortId));
    }

    @Test
    void testResolve_WithLongNumericString_CallsApi() {
        // Arrange (more than 17 digits)
        String longId = "765611980000000001";
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(1);
        innerResponse.setSteamid(VALID_STEAM_ID);
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(longId))).thenReturn(response);

        // Act
        String result = steamIdResolverService.resolve(longId);

        // Assert
        assertEquals(VALID_STEAM_ID, result);
        verify(apiConectorClient, times(1)).resolveVanity(eq(longId));
    }

    @Test
    void testResolve_WithAlphanumericString_CallsApi() {
        // Arrange
        String alphanumeric = "user123";
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(1);
        innerResponse.setSteamid(VALID_STEAM_ID);
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(alphanumeric))).thenReturn(response);

        // Act
        String result = steamIdResolverService.resolve(alphanumeric);

        // Assert
        assertEquals(VALID_STEAM_ID, result);
        verify(apiConectorClient, times(1)).resolveVanity(eq(alphanumeric));
    }

    @Test
    void testResolve_WithSpecialCharactersVanity_CallsApi() {
        // Arrange
        String specialVanity = "user-name_123";
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(1);
        innerResponse.setSteamid(VALID_STEAM_ID);
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(specialVanity))).thenReturn(response);

        // Act
        String result = steamIdResolverService.resolve(specialVanity);

        // Assert
        assertEquals(VALID_STEAM_ID, result);
        verify(apiConectorClient, times(1)).resolveVanity(eq(specialVanity));
    }

    @Test
    void testResolve_WithSuccessNotOne_ThrowsException() {
        // Arrange
        SteamResolveVanityResponseDto response = new SteamResolveVanityResponseDto();
        SteamResolveVanityResponseDto.InnerResponse innerResponse = new SteamResolveVanityResponseDto.InnerResponse();
        innerResponse.setSuccess(0); // Not 1
        innerResponse.setSteamid(VALID_STEAM_ID);
        response.setResponse(innerResponse);

        when(apiConectorClient.resolveVanity(eq(VANITY_URL))).thenReturn(response);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            steamIdResolverService.resolve(VANITY_URL);
        });

        assertTrue(exception.getMessage().contains("No se pudo resolver vanity URL"));
    }
}
