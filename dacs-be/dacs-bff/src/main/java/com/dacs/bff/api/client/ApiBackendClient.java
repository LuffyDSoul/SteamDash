package com.dacs.bff.api.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.dacs.bff.dto.BuildInfoDTO;
import com.dacs.bff.dto.BibliotecaJuegoDto;


@FeignClient(
			name = "apiBackendClient", 
			url = "${feign.client.config.apiBackendClient.url}"
			)

public interface ApiBackendClient {

    @GetMapping("/ping")
    String ping();
    
    @GetMapping("/version")
    BuildInfoDTO version();

        // Biblioteca - juego único
        @GetMapping("/biblioteca-juego/{appId}")
        BibliotecaJuegoDto getBibliotecaJuego(@PathVariable("appId") Long appId);

    @PostMapping("/biblioteca-juego/db/bulk")
    java.util.List<BibliotecaJuegoDto> getBibliotecaJuegosDesdeDb(@RequestBody java.util.List<Long> appIds);
    
    // Guardar o actualizar un juego en la biblioteca
    @PostMapping("/biblioteca-juego")
    BibliotecaJuegoDto upsertBibliotecaJuego(@RequestBody BibliotecaJuegoDto juego);
    
    // Búsqueda de juegos por tags
    @GetMapping("/biblioteca-juego/search/by-tags")
    java.util.List<Long> buscarAppIdsPorTags(@RequestParam("tags") java.util.List<String> tags);
    
    // Endpoints de logros (achievements)
    // Obtener logros desde la base de datos
    @GetMapping("/achievements/user/{steamId}/game/{appId}")
    java.util.Map<String, Object> getAchievementsFromDatabase(
        @PathVariable("steamId") String steamId,
        @PathVariable("appId") Long appId
    );
    
    // Obtener estadísticas de logros de todos los juegos de un usuario (bulk query)
    @GetMapping("/achievements/user/{steamId}/stats")
    java.util.Map<String, Object> getUserAchievementStats(
        @PathVariable("steamId") String steamId
    );
    
    // Obtener estadísticas completas de logros con agregaciones y top lists
    @PostMapping("/achievement-stats/{steamId}")
    java.util.Map<String, Object> getUserAchievementStatsComplete(
        @PathVariable("steamId") String steamId,
        @RequestBody java.util.List<Long> topGames
    );
    
    // Ingerir logros de un usuario para un juego específico (llama a Steam API y guarda en BD)
    @PostMapping("/achievements/ingest/user/{steamId}/game/{appId}")
    java.util.Map<String, Object> ingestUserGameAchievements(
        @PathVariable("steamId") String steamId,
        @PathVariable("appId") Long appId
    );

    // Endpoints de Noticias de Usuario
    @PostMapping("/api/user-news/process")
    com.dacs.bff.dto.UserNewsResponseDto processUserNews(@RequestBody com.dacs.bff.dto.UserNewsRequestDto request);
}


