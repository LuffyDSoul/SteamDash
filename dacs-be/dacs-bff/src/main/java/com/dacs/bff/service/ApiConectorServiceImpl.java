package com.dacs.bff.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.dto.SteamGameDto;
import com.dacs.bff.dto.SteamOwnedGamesResponseDto;
import com.dacs.bff.exeption.BffException;
import com.dacs.bff.exeption.ConectorException;
import com.dacs.bff.exeption.ErrorEnum;
import com.dacs.bff.dto.SteamNewsResponseDto;
import com.dacs.bff.dto.NewsViewDto;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApiConectorServiceImpl implements ApiConectorService {

	@Autowired
	private ApiConectorClient apiConectorClient;
	
	@Override
	public String ping() {
		return apiConectorClient.ping();
	}

	// Métodos Steam API
	@Override
	public SteamGameDto getSteamGameDetails(String appId) {
		if(appId == null || appId.trim().isEmpty()) {
			throw new BffException(ErrorEnum.DATO_VALOR_INCORRECTO,"App ID no puede ser nulo o vacío");
		}
		
		try {
			return apiConectorClient.getSteamGameDetails(appId);
		} catch (Exception e) {
			throw new ConectorException(ErrorEnum.DATO_SIN_VALOR_INGRESADO, 
				"Error al obtener datos del juego Steam: " + e.getMessage());
		}
	}
	
	@Override
	public SteamOwnedGamesResponseDto getUserOwnedGames(String steamId, Boolean includeAppInfo, Boolean includePlayedFreeGames) {
		if(steamId == null || steamId.trim().isEmpty()) {
			throw new BffException(ErrorEnum.DATO_VALOR_INCORRECTO,"Steam ID no puede ser nulo o vacío");
		}
		
		try {
			return apiConectorClient.getUserOwnedGames(steamId, includeAppInfo, includePlayedFreeGames);
		} catch (Exception e) {
			throw new ConectorException(ErrorEnum.DATO_SIN_VALOR_INGRESADO, 
				"Error al obtener biblioteca de juegos del usuario: " + e.getMessage());
		}
	}

	@Override
	public java.util.List<NewsViewDto> getNewsForApp(String appId, Integer count, Integer maxLength) {
		if(appId == null || appId.trim().isEmpty()) {
			throw new BffException(ErrorEnum.DATO_VALOR_INCORRECTO,"App ID no puede ser nulo o vacío");
		}
		try {
			SteamNewsResponseDto resp = apiConectorClient.getNewsForApp(appId, count, maxLength);
			if (resp == null || resp.getAppNews() == null || resp.getAppNews().getNewsItems() == null) {
				return null;
			}

			DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

			List<NewsViewDto> list = resp.getAppNews().getNewsItems().stream().map(item -> {
				NewsViewDto v = new NewsViewDto();
				v.setTitle(item.getTitle());
				// filter out STEAM_CLAN_IMAGE style urls
				String contents = item.getContents();
				if (contents != null) {
					contents = contents.replaceAll("\\{STEAM_CLAN_IMAGE}\\/[0-9]+\\/[a-z0-9]+\\.[a-z0-9]+",""
					);
				}
				v.setContents(contents);
				v.setUrl(item.getUrl());
				v.setDate(item.getDate());
				if (item.getDate() != null) {
					v.setDateFormatted(df.format(Instant.ofEpochSecond(item.getDate())));
				}
				// headerImageUrl left null as placeholder
				v.setHeaderImageUrl(null);
				return v;
			}).collect(Collectors.toList());

			return list;
		} catch (Exception e) {
			throw new ConectorException(ErrorEnum.DATO_SIN_VALOR_INGRESADO,
				"Error al obtener noticias del juego desde conector: " + e.getMessage());
		}
	}
}
