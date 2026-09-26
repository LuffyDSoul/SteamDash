package com.dacs.backend.controller;

import com.dacs.backend.service.AchievementIngestionService;
import com.dacs.backend.service.AchievementQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador para manejar la ingesta y consulta de logros
 */
@Slf4j
@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementIngestionController {
    
    private final AchievementIngestionService achievementIngestionService;
    private final AchievementQueryService achievementQueryService;
    
    /**
     * Endpoint para iniciar ingesta de logros para múltiples juegos de un usuario
     * POST /backend/achievements/ingest (con contextPath)
     * Body: {
     *   "steamId": "76561198197534238",
     *   "appIds": [730, 440, 570, ...]
     * }
     */
    @PostMapping("/ingest")
    public ResponseEntity<Map<String, Object>> ingestAchievements(@RequestBody Map<String, Object> request) {
        String steamId = (String) request.get("steamId");
        List<Integer> appIdsInt = (List<Integer>) request.get("appIds");
        
        if (steamId == null || appIdsInt == null || appIdsInt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "steamId y appIds son requeridos"));
        }
        
        // Convertir Integer a Long
        List<Long> appIds = appIdsInt.stream()
            .map(Integer::longValue)
            .toList();
        
        log.info("Iniciando ingesta de logros para usuario {} con {} juegos", steamId, appIds.size());
        
        // Ejecutar ingesta en un thread separado para no bloquear la respuesta
        new Thread(() -> {
            achievementIngestionService.ingestAchievementsForUserGames(steamId, appIds);
        }).start();
        
        return ResponseEntity.accepted()
            .body(Map.of(
                "message", "Ingesta de logros iniciada en segundo plano",
                "steamId", steamId,
                "totalGames", appIds.size()
            ));
    }
    
    /**
     * Endpoint para ingestar schema de un juego específico
     * POST /backend/achievements/ingest/game/{appId} (con contextPath)
     */
    @PostMapping("/ingest/game/{appId}")
    public ResponseEntity<Map<String, Object>> ingestGameSchema(@PathVariable Long appId) {
        log.info("Ingesta manual de schema para appId {}", appId);
        
        boolean success = achievementIngestionService.ingestGameAchievementSchema(appId);
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                "message", "Schema de logros ingested exitosamente",
                "appId", appId,
                "success", true
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "message", "No se pudieron ingestar logros (puede que el juego no tenga logros)",
                "appId", appId,
                "success", false
            ));
        }
    }
    
    /**
     * Endpoint para ingestar progreso de un usuario en un juego específico
     * POST /backend/achievements/ingest/user/{steamId}/game/{appId} (con contextPath)
     */
    @PostMapping("/ingest/user/{steamId}/game/{appId}")
    public ResponseEntity<Map<String, Object>> ingestUserProgress(
            @PathVariable String steamId,
            @PathVariable Long appId) {
        
        log.info("Ingesta manual de progreso para usuario {} appId {}", steamId, appId);
        
        boolean success = achievementIngestionService.ingestUserAchievementProgress(steamId, appId);
        
        if (success) {
            return ResponseEntity.ok(Map.of(
                "message", "Progreso de logros ingested exitosamente",
                "steamId", steamId,
                "appId", appId,
                "success", true
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                "message", "No se pudo ingestar progreso de usuario",
                "steamId", steamId,
                "appId", appId,
                "success", false
            ));
        }
    }
    
    /**
     * Endpoint para contar cuántos juegos con logros tiene un usuario
     * GET /backend/achievements/count/{steamId}
     */
    @GetMapping("/count/{steamId}")
    public ResponseEntity<Map<String, Object>> countUserGamesWithAchievements(@PathVariable String steamId) {
        long count = achievementQueryService.countUserGamesWithAchievements(steamId);
        
        return ResponseEntity.ok(Map.of(
            "steamId", steamId,
            "gamesWithAchievements", count
        ));
    }
    
    /**
     * Endpoint para verificar qué juegos ya tienen logros en BD
     * POST /backend/achievements/check (con contextPath)
     * Body: {
     *   "steamId": "76561198197534238",
     *   "appIds": [730, 440, 570, ...]
     * }
     * Response: {
     *   "missingAppIds": [730, 570, ...]
     * }
     */
    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> checkMissingAchievements(@RequestBody Map<String, Object> request) {
        String steamId = (String) request.get("steamId");
        List<Integer> appIdsInt = (List<Integer>) request.get("appIds");
        
        if (steamId == null || appIdsInt == null || appIdsInt.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "steamId y appIds son requeridos"));
        }
        
        List<Long> appIds = appIdsInt.stream()
            .map(Integer::longValue)
            .toList();
        
        List<Long> missingAppIds = achievementQueryService.findMissingAchievements(steamId, appIds);
        
        log.info("Usuario {} tiene {}/{} juegos sin logros en BD", steamId, missingAppIds.size(), appIds.size());
        
        return ResponseEntity.ok(Map.of(
            "missingAppIds", missingAppIds,
            "total", appIds.size(),
            "missing", missingAppIds.size()
        ));
    }
    
    /**
     * Endpoint para obtener logros desde la BD
     * GET /backend/achievements/user/{steamId}/game/{appId} (con contextPath)
     */
    @GetMapping("/user/{steamId}/game/{appId}")
    public ResponseEntity<Map<String, Object>> getAchievements(
            @PathVariable String steamId,
            @PathVariable Long appId) {
        
        log.info("Consultando logros desde BD para usuario {} appId {}", steamId, appId);
        
        Map<String, Object> result = achievementQueryService.getGameAchievementsWithUserProgress(steamId, appId);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * Endpoint para obtener estadísticas de logros de todos los juegos de un usuario en una sola consulta
     * GET /backend/achievements/user/{steamId}/stats (con contextPath)
     */
    @GetMapping("/user/{steamId}/stats")
    public ResponseEntity<Map<String, Object>> getUserAchievementStats(@PathVariable String steamId) {
        
        log.info("Consultando estadísticas de logros para usuario {} (bulk)", steamId);
        
        Map<Long, Map<String, Object>> stats = achievementQueryService.getUserAchievementStatsBulk(steamId);
        
        return ResponseEntity.ok(Map.of(
            "steamId", steamId,
            "stats", stats
        ));
    }
}

