package com.dacs.bff.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.client.BackendClient;
import com.dacs.bff.dto.BibliotecaComparacionDto;
import com.dacs.bff.dto.SteamOwnedGamesResponseDto;
import com.dacs.bff.dto.SteamPlayerSummariesResponseDto;
import com.dacs.bff.dto.SteamUserGamesInput;
import com.dacs.bff.dto.SteamUserGamesInput.GameInfo;
import com.dacs.bff.dto.SteamUserGamesInput.PlayerInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BibliotecaComparacionOrchestrationService {

    @Autowired
    private ApiConectorService apiConectorService;

    @Autowired
    private ApiConectorClient apiConectorClient;

    @Autowired
    private BackendClient backendClient;

    @Autowired
    private SteamIdResolverService steamIdResolverService;

    /**
     * Comparación de 2 usuarios
     */
    public BibliotecaComparacionDto compararBibliotecas(String steamId1, String steamId2) {
        log.info("BFF - Orquestando comparacion de bibliotecas entre {} y {}", steamId1, steamId2);

        try {
            List<String> steamIds = Arrays.asList(steamId1, steamId2);
            return compararBibliotecas(steamIds);
        } catch (Exception e) {
            log.error("Error en la orquestacion de comparacion: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al comparar bibliotecas: " + e.getMessage());
        }
    }
    
    /**
     * Comparación de múltiples usuarios (2-6)
     */
    public BibliotecaComparacionDto compararBibliotecas(List<String> steamIds) {
        log.info("BFF - Orquestando comparacion de bibliotecas entre {} usuarios", steamIds.size());

        if (steamIds == null || steamIds.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se requieren al menos 2 usuarios para comparar");
        }
        
        if (steamIds.size() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El máximo de usuarios a comparar es 6");
        }

        try {
            // Obtener datos de todos los usuarios
            List<SteamUserGamesInput> usuariosInputs = new ArrayList<>();
            for (String steamId : steamIds) {
                usuariosInputs.add(obtenerDatosUsuario(steamId));
            }

            BibliotecaComparacionDto resultado = backendClient.compararBibliotecasMultiples(usuariosInputs);

            log.info("BFF - Comparacion completada exitosamente");
            return resultado;

        } catch (Exception e) {
            log.error("Error en la orquestacion de comparacion: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al comparar bibliotecas: " + e.getMessage());
        }
    }

    private SteamUserGamesInput obtenerDatosUsuario(String steamId) {
        log.info("BFF - Obteniendo datos del usuario {}", steamId);

        // Safeguard: ensure resolved steamid64 in case caller skipped resolution
        String resolvedId;
        try {
            resolvedId = steamIdResolverService.resolve(steamId);
        } catch (IllegalArgumentException e) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: " + steamId);
        }

        SteamPlayerSummariesResponseDto profileResponse = apiConectorClient.getPlayerSummaries(resolvedId);
        if (profileResponse == null || profileResponse.getResponse() == null ||
                profileResponse.getResponse().getPlayers() == null ||
                profileResponse.getResponse().getPlayers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado: " + resolvedId);
        }

        SteamPlayerSummariesResponseDto.PlayerDto player = profileResponse.getResponse().getPlayers().get(0);

        SteamOwnedGamesResponseDto gamesResponse = apiConectorService.getUserOwnedGames(resolvedId, true, true);
        if (gamesResponse == null || gamesResponse.getResponse() == null ||
                gamesResponse.getResponse().getGames() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Biblioteca no encontrada para usuario: " + resolvedId);
        }

        List<SteamOwnedGamesResponseDto.OwnedGameDto> games = gamesResponse.getResponse().getGames();
        
        log.info("BFF - Usuario {} tiene {} juegos en su biblioteca", resolvedId, games.size());

        PlayerInfo playerInfo = PlayerInfo.builder()
                .steamId(steamId)
                .personaName(player.getPersonaName())
                .avatarFull(player.getAvatarFull())
                .profileUrl(player.getProfileUrl())
                .localCountryCode(player.getLocalCountryCode())
                .timeCreated(player.getTimeCreated())
                .build();

        // Construir lista de juegos con datos básicos de la biblioteca
        // NO obtenemos detalles individuales de cada juego para evitar cientos de llamadas HTTP
        List<GameInfo> gameInfoList = games.stream()
        .map(game -> {
            // img_icon_url returned by GetOwnedGames (include_appinfo) is a partial path like 
            // "/a/123456/abcdef1234567890.jpg" — build a full CDN URL when possible.
            String imgIconHash = game.getImgIconUrl();
            String imgIconUrl = null;
            if (imgIconHash != null && !imgIconHash.isBlank() && game.getAppId() != null) {
                // Steam CDN pattern for icons: https://media.steampowered.com/steamcommunity/public/images/apps/{appid}/{hash}.jpg
                imgIconUrl = String.format("https://media.steampowered.com/steamcommunity/public/images/apps/%d/%s.jpg", game.getAppId(), imgIconHash);
            }

            return GameInfo.builder()
                .appId(game.getAppId().intValue())
                .name(game.getName())
                .playtimeForever(game.getPlaytimeForever())
                // No disponible en owned games API (header image viene de appdetails)
                .headerImage(null)
                .imgIconUrl(imgIconUrl)
                .isFree(null) // No disponible en owned games API
                .price("N/A") // Valor por defecto - transformación simple del BFF
                .build();
        })
        .collect(Collectors.toList());

        return SteamUserGamesInput.builder()
                .steamId(resolvedId)
                .playerInfo(playerInfo)
                .games(gameInfoList)
                .build();
    }
}
