package com.dacs.bff.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.dto.SteamGameDto;
import com.dacs.bff.dto.SteamPlayerSummariesResponseDto;
import com.dacs.bff.dto.SteamOwnedGamesResponseDto;
import com.dacs.bff.dto.SteamNewsResponseDto;
import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.service.ApiConectorService;
import com.dacs.bff.service.SteamIdResolverService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controlador BFF para integración con Steam API externa
 */
@Slf4j
@RestController
@RequestMapping(value = "/steam")
public class SteamController {

	@Autowired
	private ApiConectorService apiConectorService;

	@Autowired
	private ApiConectorClient apiConectorClient;

	@Autowired
	private SteamIdResolverService steamIdResolverService;

	@GetMapping("/game/{appId}")
	public ResponseEntity<SteamGameDto> getSteamGameDetails(@PathVariable String appId) {
		log.info("BFF - Obteniendo detalles del juego Steam con App ID: {}", appId);

		try {
			SteamGameDto gameDetails = apiConectorService.getSteamGameDetails(appId);
			if (gameDetails != null) {
				return new ResponseEntity<>(gameDetails, HttpStatus.OK);
			} else {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
		} catch (Exception e) {
			log.error("Error al obtener detalles del juego Steam: {}", e.getMessage());
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Proxy endpoint to obtain player summaries from conector
	 * GET /steam/player-summaries?steamids=123
	 * Supports both Steam IDs and vanity URLs
	 */
	@GetMapping("/player-summaries")
	public ResponseEntity<SteamPlayerSummariesResponseDto> getPlayerSummaries(@RequestParam String steamids) {
		try {
			// Resolve vanity URLs to Steam IDs
			String[] ids = steamids.split(",");
			StringBuilder resolvedIds = new StringBuilder();
			
			for (int i = 0; i < ids.length; i++) {
				String id = ids[i].trim();
				try {
					String resolved = steamIdResolverService.resolve(id);
					if (i > 0) {
						resolvedIds.append(",");
					}
					resolvedIds.append(resolved);
				} catch (IllegalArgumentException e) {
					log.warn("No se pudo resolver '{}': {}", id, e.getMessage());
					// Continue with next ID instead of failing the entire request
				}
			}
			
			if (resolvedIds.length() == 0) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			
			SteamPlayerSummariesResponseDto resp = apiConectorClient.getPlayerSummaries(resolvedIds.toString());
			return new ResponseEntity<>(resp, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error al obtener player summaries desde conector: {}", e.getMessage());
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Proxy endpoint to obtain owned games for a user from conector
	 * GET /steam/user/{steamId}/games
	 */
	@GetMapping("/user/{steamId}/games")
	public ResponseEntity<SteamOwnedGamesResponseDto> getUserGames(@PathVariable String steamId) {
		try {
			String resolvedId = steamIdResolverService.resolve(steamId);
			SteamOwnedGamesResponseDto games = apiConectorClient.getUserOwnedGames(resolvedId, true, true);
			return new ResponseEntity<>(games, HttpStatus.OK);
		} catch (IllegalArgumentException e) {
			log.warn("Error resolviendo vanity: {}", e.getMessage());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			log.error("Error al obtener juegos del usuario desde conector: {}", e.getMessage());
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Proxy endpoint to obtain news for a specific game from conector
	 * GET /steam/game/{appId}/news
	 */
	@GetMapping("/game/{appId}/news")
	public ResponseEntity<SteamNewsResponseDto> getGameNews(
			@PathVariable String appId,
			@RequestParam(required = false) Integer count,
			@RequestParam(required = false) Integer maxLength) {
		try {
			log.info("BFF - Obteniendo noticias para el juego con App ID: {}, count: {}, maxLength: {}", 
				appId, count, maxLength);
			SteamNewsResponseDto news = apiConectorClient.getNewsForApp(appId, count, maxLength);
			return new ResponseEntity<>(news, HttpStatus.OK);
		} catch (Exception e) {
			log.error("Error al obtener noticias del juego desde conector: {}", e.getMessage(), e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}