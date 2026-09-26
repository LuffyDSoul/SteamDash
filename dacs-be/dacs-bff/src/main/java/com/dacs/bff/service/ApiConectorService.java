package com.dacs.bff.service;

import com.dacs.bff.dto.SteamGameDto;
import com.dacs.bff.dto.SteamOwnedGamesResponseDto;

public interface ApiConectorService {
	
	public String ping();
	
	// Métodos Steam API
	public SteamGameDto getSteamGameDetails(String appId);
    
	// Obtener noticias de un juego (vista mapeada)
	public java.util.List<com.dacs.bff.dto.NewsViewDto> getNewsForApp(String appId, Integer count, Integer maxLength);
	
	// Método para obtener biblioteca de juegos de un usuario
	public SteamOwnedGamesResponseDto getUserOwnedGames(String steamId, Boolean includeAppInfo, Boolean includePlayedFreeGames);
}
