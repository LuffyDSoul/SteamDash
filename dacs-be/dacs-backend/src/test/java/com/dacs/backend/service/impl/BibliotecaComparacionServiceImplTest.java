package com.dacs.backend.service.impl;

import com.dacs.backend.dto.BibliotecaComparacionDto;
import com.dacs.backend.dto.SteamUserGamesInput;
import com.dacs.backend.entity.GameRecord;
import com.dacs.backend.repository.GameRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BibliotecaComparacionServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class BibliotecaComparacionServiceImplTest {

    @Mock
    private GameRecordServiceImpl gameRecordService;

    @Mock
    private GameRecordRepository gameRecordRepository;

    @InjectMocks
    private BibliotecaComparacionServiceImpl bibliotecaComparacionService;

    private SteamUserGamesInput usuario1;
    private SteamUserGamesInput usuario2;
    private List<SteamUserGamesInput> usuarios;

    @BeforeEach
    void setUp() {
        // Set default values for @Value fields
        ReflectionTestUtils.setField(bibliotecaComparacionService, "enrichmentLimit", 5);
        ReflectionTestUtils.setField(bibliotecaComparacionService, "freePriceFormat", "Gratuito");
        ReflectionTestUtils.setField(bibliotecaComparacionService, "missingPriceFormat", "No disponible");

        // Setup usuario 1
        SteamUserGamesInput.PlayerInfo playerInfo1 = SteamUserGamesInput.PlayerInfo.builder()
                .steamId("76561198000000001")
                .personaName("Player1")
                .avatarFull("http://avatar1.jpg")
                .profileUrl("http://profile1")
                .localCountryCode("US")
                .timeCreated(1234567890L)
                .build();

        List<SteamUserGamesInput.GameInfo> games1 = new ArrayList<>();
        games1.add(createGameInfo(730, "Counter-Strike 2", 1000, true, "Gratuito"));
        games1.add(createGameInfo(570, "Dota 2", 500, true, "Gratuito"));
        games1.add(createGameInfo(440, "Team Fortress 2", 300, true, "Gratuito"));

        usuario1 = SteamUserGamesInput.builder()
                .steamId("76561198000000001")
                .playerInfo(playerInfo1)
                .games(games1)
                .build();

        // Setup usuario 2
        SteamUserGamesInput.PlayerInfo playerInfo2 = SteamUserGamesInput.PlayerInfo.builder()
                .steamId("76561198000000002")
                .personaName("Player2")
                .avatarFull("http://avatar2.jpg")
                .profileUrl("http://profile2")
                .localCountryCode("AR")
                .timeCreated(1234567891L)
                .build();

        List<SteamUserGamesInput.GameInfo> games2 = new ArrayList<>();
        games2.add(createGameInfo(730, "Counter-Strike 2", 2000, true, "Gratuito"));
        games2.add(createGameInfo(570, "Dota 2", 1500, true, "Gratuito"));
        games2.add(createGameInfo(252490, "Rust", 800, false, "$39.99 USD"));

        usuario2 = SteamUserGamesInput.builder()
                .steamId("76561198000000002")
                .playerInfo(playerInfo2)
                .games(games2)
                .build();

        usuarios = Arrays.asList(usuario1, usuario2);

        // Setup mock repository responses with lenient() to avoid UnnecessaryStubbingException
        lenient().when(gameRecordRepository.findByAppIdIn(anyList())).thenReturn(new ArrayList<>());
        lenient().when(gameRecordService.enrichAndStore(anyList(), anyInt())).thenReturn(new HashMap<>());
    }

    private SteamUserGamesInput.GameInfo createGameInfo(long appId, String name, int playtime, boolean isFree, String price) {
        return SteamUserGamesInput.GameInfo.builder()
                .appId(appId)
                .name(name)
                .playtimeForever(playtime)
                .headerImage("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/header.jpg")
                .imgIconUrl("https://media.steampowered.com/steamcommunity/public/images/apps/" + appId + "/icon.jpg")
                .isFree(isFree)
                .price(price)
                .build();
    }

    @Test
    void testCompararBibliotecas_TwoUsers_Success() {
        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getUsuarios().size());
        assertEquals(2, result.getJuegosComunes().size()); // CS2 and Dota 2
        assertEquals(1, result.getJuegosUnicosPorUsuario().get("Player1").size()); // TF2
        assertEquals(1, result.getJuegosUnicosPorUsuario().get("Player2").size()); // Rust
        
        // Verify statistics
        assertNotNull(result.getEstadisticas());
        assertEquals(2, result.getEstadisticas().getJuegosComunes());
        assertEquals(2, result.getEstadisticas().getTotalUsuarios());
        
        // Verify common games
        BibliotecaComparacionDto.JuegoComparacionDto cs2 = result.getJuegosComunes().stream()
                .filter(j -> j.getAppId() == 730L)
                .findFirst()
                .orElse(null);
        assertNotNull(cs2);
        assertEquals("Counter-Strike 2", cs2.getName());
        assertEquals(2, cs2.getCantidadCopias());
        assertTrue(cs2.getTiempoJugadoPorUsuario().containsKey("Player1"));
        assertTrue(cs2.getTiempoJugadoPorUsuario().containsKey("Player2"));
        assertEquals(1000, cs2.getTiempoJugadoPorUsuario().get("Player1"));
        assertEquals(2000, cs2.getTiempoJugadoPorUsuario().get("Player2"));
    }

    @Test
    void testCompararBibliotecas_LessThanTwoUsers() {
        // Arrange
        List<SteamUserGamesInput> oneUser = Arrays.asList(usuario1);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bibliotecaComparacionService.compararBibliotecas(oneUser);
        });

        assertEquals("Se requieren al menos 2 usuarios para comparar", exception.getMessage());
    }

    @Test
    void testCompararBibliotecas_MoreThanSixUsers() {
        // Arrange
        List<SteamUserGamesInput> sevenUsers = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            sevenUsers.add(usuario1);
        }

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bibliotecaComparacionService.compararBibliotecas(sevenUsers);
        });

        assertEquals("El máximo de usuarios a comparar es 6", exception.getMessage());
    }

    @Test
    void testCompararBibliotecas_ThreeUsers() {
        // Arrange
        SteamUserGamesInput.PlayerInfo playerInfo3 = SteamUserGamesInput.PlayerInfo.builder()
                .steamId("76561198000000003")
                .personaName("Player3")
                .avatarFull("http://avatar3.jpg")
                .profileUrl("http://profile3")
                .build();

        List<SteamUserGamesInput.GameInfo> games3 = new ArrayList<>();
        games3.add(createGameInfo(730, "Counter-Strike 2", 300, true, "Gratuito"));
        games3.add(createGameInfo(271590, "GTA V", 1200, false, "$29.99 USD"));

        SteamUserGamesInput usuario3 = SteamUserGamesInput.builder()
                .steamId("76561198000000003")
                .playerInfo(playerInfo3)
                .games(games3)
                .build();

        List<SteamUserGamesInput> threeUsers = Arrays.asList(usuario1, usuario2, usuario3);

        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(threeUsers);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.getUsuarios().size());
        assertEquals(1, result.getJuegosComunes().stream()
                .filter(j -> j.getCantidadCopias() == 3)
                .count()); // Only CS2 is owned by all 3
        
        // Verify the game owned by all 3
        BibliotecaComparacionDto.JuegoComparacionDto cs2 = result.getJuegosComunes().stream()
                .filter(j -> j.getAppId() == 730L)
                .findFirst()
                .orElse(null);
        assertNotNull(cs2);
        assertEquals(3, cs2.getCantidadCopias());
    }

    @Test
    void testCompararBibliotecas_WithDatabaseEnrichment() {
        // Arrange
        GameRecord gameRecord = new GameRecord();
        gameRecord.setAppId(730L);
        gameRecord.setName("Counter-Strike 2");
        gameRecord.setHeaderImage("https://cdn.akamai.steamstatic.com/steam/apps/730/header.jpg");
        gameRecord.setIsFree(true);
        gameRecord.setPrice("Gratuito");
        gameRecord.setTags(Arrays.asList("Action", "FPS", "Multiplayer"));

        when(gameRecordRepository.findByAppIdIn(anyList()))
                .thenReturn(Arrays.asList(gameRecord));

        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        assertNotNull(result);
        verify(gameRecordRepository, times(1)).findByAppIdIn(anyList());
        
        // Verify that database data was applied
        BibliotecaComparacionDto.JuegoComparacionDto cs2 = result.getJuegosComunes().stream()
                .filter(j -> j.getAppId() == 730L)
                .findFirst()
                .orElse(null);
        assertNotNull(cs2);
        assertEquals("Gratuito", cs2.getPrice());
    }

    @Test
    void testCompararBibliotecas_WithExternalEnrichment() {
        // Arrange
        Map<Long, List<String>> tagsMap = new HashMap<>();
        tagsMap.put(730L, Arrays.asList("Action", "FPS", "Multiplayer"));
        tagsMap.put(570L, Arrays.asList("MOBA", "Strategy", "Free to Play"));

        when(gameRecordService.enrichAndStore(anyList(), anyInt())).thenReturn(tagsMap);

        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        assertNotNull(result);
        verify(gameRecordService, times(1)).enrichAndStore(anyList(), eq(5));
        
        // Verify tags were applied to common games
        BibliotecaComparacionDto.JuegoComparacionDto cs2 = result.getJuegosComunes().stream()
                .filter(j -> j.getAppId() == 730L)
                .findFirst()
                .orElse(null);
        assertNotNull(cs2);
        assertNotNull(cs2.getTags());
        assertEquals(3, cs2.getTags().size());
    }

    @Test
    void testCompararBibliotecas_CalculateStatistics() {
        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        BibliotecaComparacionDto.EstadisticasDto stats = result.getEstadisticas();
        assertNotNull(stats);
        assertEquals(2, stats.getJuegosComunes());
        assertEquals(2, stats.getTotalUsuarios());
        assertTrue(stats.getPorcentajeSimilitud() > 0);
        assertNotNull(stats.getCategoriaComparacion());
    }

    @Test
    void testCompararBibliotecas_CategoryForTwoUsers() {
        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        String categoria = result.getEstadisticas().getCategoriaComparacion();
        assertNotNull(categoria);
        // With 2 common games out of 4 unique = 50% similarity
        assertTrue(categoria.equals("Moderadamente similar") || 
                   categoria.equals("Muy similar") ||
                   categoria.equals("Poco similar"));
    }

    @Test
    void testCompararBibliotecas_NullGames() {
        // Arrange
        usuario1.setGames(null);
        usuario2.setGames(new ArrayList<>());

        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getJuegosComunes().size());
        assertEquals(2, result.getUsuarios().size());
    }

    @Test
    void testCompararBibliotecas_CalculateTotalHours() {
        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        BibliotecaComparacionDto.UsuarioComparacionDto user1 = result.getUsuarios().stream()
                .filter(u -> u.getSteamId().equals("76561198000000001"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(user1);
        // Usuario1 has 1000 + 500 + 300 = 1800 minutes = 30 hours
        assertEquals(30, user1.getTotalHorasJugadas());

        BibliotecaComparacionDto.UsuarioComparacionDto user2 = result.getUsuarios().stream()
                .filter(u -> u.getSteamId().equals("76561198000000002"))
                .findFirst()
                .orElse(null);
        
        assertNotNull(user2);
        // Usuario2 has 2000 + 1500 + 800 = 4300 minutes = 72 hours (rounded)
        assertEquals(72, user2.getTotalHorasJugadas());
    }

    @Test
    void testCompararBibliotecas_UniqueGamesPerUser() {
        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        List<BibliotecaComparacionDto.JuegoComparacionDto> player1Unique = 
                result.getJuegosUnicosPorUsuario().get("Player1");
        assertNotNull(player1Unique);
        assertEquals(1, player1Unique.size());
        assertEquals(440L, player1Unique.get(0).getAppId()); // Team Fortress 2

        List<BibliotecaComparacionDto.JuegoComparacionDto> player2Unique = 
                result.getJuegosUnicosPorUsuario().get("Player2");
        assertNotNull(player2Unique);
        assertEquals(1, player2Unique.size());
        assertEquals(252490L, player2Unique.get(0).getAppId()); // Rust
    }

    @Test
    void testCompararBibliotecas_GameWithNullPlaytime() {
        // Arrange
        usuario1.getGames().get(0).setPlaytimeForever(null);

        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        assertNotNull(result);
        BibliotecaComparacionDto.JuegoComparacionDto cs2 = result.getJuegosComunes().stream()
                .filter(j -> j.getAppId() == 730L)
                .findFirst()
                .orElse(null);
        assertNotNull(cs2);
        // Should handle null playtime gracefully
        assertTrue(cs2.getTiempoJugadoPorUsuario().containsKey("Player1"));
    }

    @Test
    void testCompararBibliotecas_PriceFormatting() {
        // Act
        BibliotecaComparacionDto result = bibliotecaComparacionService.compararBibliotecas(usuarios);

        // Assert
        BibliotecaComparacionDto.JuegoComparacionDto rust = result.getJuegosUnicosPorUsuario()
                .get("Player2")
                .stream()
                .filter(j -> j.getAppId() == 252490L)
                .findFirst()
                .orElse(null);
        
        assertNotNull(rust);
        assertEquals("$39.99 USD", rust.getPrice());
        assertFalse(rust.getIsFree());
    }
}
