package com.dacs.conector.api.client;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dacs.conector.dto.steamDTO.SteamAppDetailsResponseDto;

@FeignClient(
		name = "steamApiClient", 
		url = "${steam.api.store-url}"
		)
public interface SteamApiClient {

	// Store API - No requiere API Key
	@GetMapping("/api/appdetails")
	Map<String, SteamAppDetailsResponseDto> getAppDetails(@RequestParam("appids") String appIds,
														 @RequestParam(value = "cc", required = false) String countryCode);
}