package com.dacs.bff.api.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.dacs.bff.dto.ItemDto;
import com.dacs.bff.dto.SteamGameDto;
import com.dacs.bff.dto.SteamOwnedGamesResponseDto;

@FeignClient(
		name = "apiConectorClient", 
		url = "${feign.client.config.apiconectorclient.url}"
		)
public interface ApiConectorClient {

	   @GetMapping("/ping")
	   String ping();	   
	   
	   // Endpoints Steam API
	   @GetMapping("/steam/game/{appId}")
	   SteamGameDto getSteamGameDetails(@PathVariable("appId") String appId);
	   
	   @GetMapping("/steam/user/{steamId}/games")
	   SteamOwnedGamesResponseDto getUserOwnedGames(
			   @PathVariable("steamId") String steamId,
			   @RequestParam(name = "includeAppInfo", defaultValue = "true") Boolean includeAppInfo,
			   @RequestParam(name = "includePlayedFreeGames", defaultValue = "true") Boolean includePlayedFreeGames
	   );

	   // Endpoint para juegos jugados recientemente (últimas 2 semanas)
	   @GetMapping("/steam/user/{steamId}/recently")
	   com.dacs.bff.dto.SteamRecentlyPlayedGamesResponseDto getRecentlyPlayedGames(
		   @PathVariable("steamId") String steamId
	   );

	    // Endpoint to get global most played games from conector
	    @GetMapping("/steam/mostPlayedGames")
	    Object getMostPlayedGames();

		// Obtener noticias de un juego desde el conector
		@GetMapping("/steam/game/{appId}/news")
		com.dacs.bff.dto.SteamNewsResponseDto getNewsForApp(@PathVariable("appId") String appId,
															@RequestParam(name = "count", required = false) Integer count,
															@RequestParam(name = "maxLength", required = false) Integer maxLength);

	// Endpoint to get player summaries from conector
	@GetMapping("/steam/player-summaries")
	com.dacs.bff.dto.SteamPlayerSummariesResponseDto getPlayerSummaries(@RequestParam("steamids") String steamIds);

	// Llamada al conector para obtener appdetails desde SteamSpy
	@GetMapping("/steam/spy/appdetails/{appId}")
	java.util.Map<String, Object> getSteamSpyAppDetails(@PathVariable("appId") String appId);
	
	// Obtener detalles completos de un juego desde Steam Store API
	@GetMapping("/steam/game/{appId}")
	java.util.Map<String, Object> getGameDetailsFromStore(@PathVariable("appId") String appId);

	// Resolver vanity URL a steamid64
	@GetMapping("/steam/resolve-vanity")
	com.dacs.bff.dto.SteamResolveVanityResponseDto resolveVanity(@RequestParam("vanityurl") String vanityUrl);

	// Obtener logros del jugador para un juego específico
	@GetMapping("/steam/user/{steamId}/game/{appId}/achievements")
	java.util.Map<String, Object> getPlayerAchievements(
		@PathVariable("steamId") String steamId,
		@PathVariable("appId") String appId
	);

	// Obtener esquema de logros de un juego
	@GetMapping("/steam/game/{appId}/schema")
	java.util.Map<String, Object> getGameAchievementSchema(@PathVariable("appId") String appId);
}
