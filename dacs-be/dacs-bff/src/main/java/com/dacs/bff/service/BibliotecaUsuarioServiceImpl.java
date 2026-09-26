package com.dacs.bff.service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.api.client.ApiBackendClient;
import com.dacs.bff.client.BackendClient;
import com.dacs.bff.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de biblioteca de usuario
 */
@Slf4j
@Service
public class BibliotecaUsuarioServiceImpl implements BibliotecaUsuarioService {

    @Autowired
    private ApiConectorClient apiConectorClient;

    @Autowired
    private ApiConectorService apiConectorService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ApiBackendClient apiBackendClient;
    
    @Autowired
    private BackendClient backendClient;

    /**
     * Verifica si un juego debe ser excluido basándose en su nombre
     * @param gameName Nombre del juego
     * @return true si el juego debe ser excluido, false si debe mostrarse
     */
    private boolean shouldExcludeGame(String gameName) {
        if (gameName == null || gameName.trim().isEmpty()) {
            return false;
        }
        
        String lowerName = gameName.toLowerCase();
        
        // Whitelist: Juegos que NUNCA deben ser excluidos
        if (lowerName.equals("rising storm/red orchestra 2 multiplayer")) {
            return false;
        }
        
        // Filtrar juegos que contienen estas palabras (case-insensitive)
        if (lowerName.contains("public") ||
            lowerName.contains("publictest") ||
            lowerName.contains("private") ||
            lowerName.contains("server") ||
            lowerName.contains("beta test") ||
            lowerName.contains("public beta") ||
            lowerName.contains("playtest") ||
            lowerName.contains("unstable") ||
            lowerName.contains("staging branch")) {
            return true;
        }
        
        // Filtrar "Multiplayer", "Multi-Player" o "Multi Player" como palabra completa
        if (gameName.matches("(?i).*\\bmultiplayer\\b.*") ||
            gameName.matches("(?i).*\\bmulti-player\\b.*") ||
            gameName.matches("(?i).*\\bmulti\\s+player\\b.*")) {
            return true;
        }
        
        // "Test" debe ser palabra completa (usando regex con word boundaries)
        if (gameName.matches("(?i).*\\btest\\b.*")) {
            return true;
        }
        
        return false;
    }

    @Override
    public BibliotecaUsuarioDto getBibliotecaUsuario(String steamId, boolean refresh) {
        log.info("Obteniendo biblioteca del usuario: {} (refresh: {})", steamId, refresh);

        try {
            // 1. Obtener juegos del usuario (siempre desde Steam cuando refresh=true)
            SteamOwnedGamesResponseDto ownedGames = apiConectorService.getUserOwnedGames(steamId, true, true);
            
            if (ownedGames == null || ownedGames.getResponse() == null || ownedGames.getResponse().getGames() == null) {
                log.warn("No se encontraron juegos para el usuario: {}", steamId);
                return createEmptyBiblioteca(steamId);
            }

            // 2. Obtener información del jugador
            SteamPlayerSummariesResponseDto playerInfo = apiConectorClient.getPlayerSummaries(steamId);
            String personaName = "Usuario";
            String avatarUrl = null;
            
            if (playerInfo != null && playerInfo.getResponse() != null 
                    && playerInfo.getResponse().getPlayers() != null 
                    && !playerInfo.getResponse().getPlayers().isEmpty()) {
                SteamPlayerSummariesResponseDto.PlayerDto player = playerInfo.getResponse().getPlayers().get(0);
                personaName = player.getPersonaName();
                avatarUrl = player.getAvatarFull();
            }

            // 3. Convertir juegos a DTOs base (sin playtime2Weeks aún)
            List<JuegoUsuarioDto> juegos = ownedGames.getResponse().getGames().stream()
                    .map(this::convertToJuegoUsuarioDto)
                    .collect(Collectors.toList());

            // 3.a Obtener juegos jugados recientemente para playtime_2weeks (SIEMPRE actualizado desde Steam)
            // IMPORTANTE: getRecentlyPlayedGames incluye juegos prestados de biblioteca familiar
            try {
                log.info("Obteniendo juegos recientes con playtime_2weeks para usuario: {}", steamId);
                var recently = apiConectorClient.getRecentlyPlayedGames(steamId);
                if (recently != null && recently.getResponse() != null && recently.getResponse().getGames() != null) {
                    Map<Long, Integer> twoWeeksByAppId = recently.getResponse().getGames().stream()
                        .filter(g -> g.getAppId() != null && g.getPlaytime2Weeks() != null)
                        .collect(Collectors.toMap(g -> g.getAppId(), g -> g.getPlaytime2Weeks(), (a,b) -> a));
                    
                    log.info("Encontrados {} juegos con playtime reciente", twoWeeksByAppId.size());
                    
                    // Crear un Set de appIds que ya tenemos en la biblioteca
                    Set<Long> existingAppIds = juegos.stream()
                        .map(j -> j.getAppId() != null ? Long.valueOf(j.getAppId()) : null)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());
                    
                    // Actualizar playtime2Weeks de juegos existentes
                    for (JuegoUsuarioDto j : juegos) {
                        if (j.getAppId() != null && twoWeeksByAppId.containsKey(j.getAppId().longValue())) {
                            j.setPlaytime2Weeks(twoWeeksByAppId.get(j.getAppId().longValue()));
                        }
                    }
                    
                    // Agregar juegos prestados de biblioteca familiar que no están en la biblioteca
                    // (solo los que se jugaron en las últimas 2 semanas)
                    for (var recentGame : recently.getResponse().getGames()) {
                        Long appId = recentGame.getAppId();
                        if (appId != null && !existingAppIds.contains(appId) && recentGame.getPlaytime2Weeks() != null && recentGame.getPlaytime2Weeks() > 0) {
                            // Este es un juego prestado de biblioteca familiar
                            log.info("Agregando juego prestado de biblioteca familiar: {} ({})", recentGame.getName(), appId);
                            
                            JuegoUsuarioDto borrowedGame = JuegoUsuarioDto.builder()
                                .appId(appId.intValue())
                                .name(recentGame.getName())
                                .playtimeForever(recentGame.getPlaytimeForever() != null ? recentGame.getPlaytimeForever() : 0)
                                .playtime2Weeks(recentGame.getPlaytime2Weeks())
                                .headerImage("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/header.jpg")
                                .storeUrl("https://store.steampowered.com/app/" + appId)
                                .isBorrowed(true) // Marcar como prestado
                                .build();
                            
                            juegos.add(borrowedGame);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo obtener juegos recientes para {}: {}", steamId, e.getMessage());
            }

            // 3.b Enriquecer con precios y tags desde la base de datos (sin refrescar externas)
            try {
                List<Long> appIds = juegos.stream()
                        .map(j -> j.getAppId() != null ? Long.valueOf(j.getAppId()) : null)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());
                if (!appIds.isEmpty()) {
                    List<BibliotecaJuegoDto> dbGames = apiBackendClient.getBibliotecaJuegosDesdeDb(appIds);
                    if (dbGames != null && !dbGames.isEmpty()) {
                        Map<Long, BibliotecaJuegoDto> byId = dbGames.stream()
                                .filter(d -> d.getAppId() != null)
                                .collect(Collectors.toMap(BibliotecaJuegoDto::getAppId, d -> d, (a,b) -> a));
                        for (JuegoUsuarioDto j : juegos) {
                            Long id = j.getAppId() != null ? Long.valueOf(j.getAppId()) : null;
                            if (id != null && byId.containsKey(id)) {
                                BibliotecaJuegoDto d = byId.get(id);
                                // Merge header image (prefer DB if present)
                                if (d.getHeaderImage() != null && !d.getHeaderImage().isEmpty()) {
                                    j.setHeaderImage(d.getHeaderImage());
                                }
                                // Set price and isFree from DB
                                if (d.getPrice() != null && !d.getPrice().isEmpty()) {
                                    j.setPrice(d.getPrice());
                                } else if (j.getPrice() == null || j.getPrice().isEmpty()) {
                                    j.setPrice("N/A");
                                }
                                if (d.getIsFree() != null) {
                                    j.setIsFree(d.getIsFree());
                                }
                                // Tags from DB
                                if (d.getTags() != null && !d.getTags().isEmpty()) {
                                    java.util.List<String> tags = new java.util.ArrayList<>();
                                    for (com.dacs.bff.dto.Tags t : d.getTags()) {
                                        if (t != null && t.getTag() != null && !t.getTag().isEmpty()) {
                                            tags.add(t.getTag());
                                        }
                                    }
                                    j.setTags(tags);
                                }
                                // storeUrl ya está seteado al patrón correcto
                            } else {
                                // No hay dato en DB para este juego
                                if (j.getPrice() == null || j.getPrice().isEmpty()) {
                                    j.setPrice("N/A");
                                }
                            }
                        }
                    } else {
                        // No hay datos en DB, setear N/A por defecto si falta
                        for (JuegoUsuarioDto j : juegos) {
                            if (j.getPrice() == null || j.getPrice().isEmpty()) j.setPrice("N/A");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo enriquecer precios desde DB: {}", e.getMessage());
                for (JuegoUsuarioDto j : juegos) {
                    if (j.getPrice() == null || j.getPrice().isEmpty()) j.setPrice("N/A");
                }
            }

            // 4. Obtener estadísticas de logros desde BD en una sola consulta (optimizado)
            log.info("📊 [INICIO] Consultando estadísticas de logros para usuario {} (optimizado)", steamId);
            long startTime = System.currentTimeMillis();
            
            Map<Long, Map<String, Object>> achievementStats = new HashMap<>();
            try {
                Map<String, Object> statsResponse = apiBackendClient.getUserAchievementStats(steamId);
                long queryTime = System.currentTimeMillis() - startTime;
                log.info("⏱️ Consulta HTTP completada en {}ms", queryTime);
                
                if (statsResponse != null && statsResponse.containsKey("stats")) {
                    @SuppressWarnings("unchecked")
                    Map<Long, Map<String, Object>> stats = (Map<Long, Map<String, Object>>) statsResponse.get("stats");
                    if (stats != null) {
                        achievementStats = stats;
                        log.info("✅ Estadísticas obtenidas para {} juegos en {}ms", achievementStats.size(), queryTime);
                    }
                } else {
                    log.warn("⚠️ Respuesta vacía o sin campo 'stats'");
                }
            } catch (Exception e) {
                long errorTime = System.currentTimeMillis() - startTime;
                log.error("❌ Error al obtener estadísticas de logros después de {}ms: {}", errorTime, e.getMessage(), e);
            }
            
            // Asignar estadísticas a cada juego
            for (JuegoUsuarioDto juego : juegos) {
                if (juego.getAppId() == null) continue;
                
                Long appId = Long.valueOf(juego.getAppId());
                Map<String, Object> stats = achievementStats.get(appId);
                
                if (stats != null) {
                    Object totalObj = stats.get("totalAchievements");
                    Object unlockedObj = stats.get("unlockedAchievements");
                    
                    juego.setTotalAchievements(totalObj instanceof Integer ? (Integer) totalObj : null);
                    juego.setUnlockedAchievements(unlockedObj instanceof Integer ? (Integer) unlockedObj : null);
                    
                    log.debug("✅ Logros para {} ({}): {}/{}", 
                             juego.getName(), appId, juego.getUnlockedAchievements(), juego.getTotalAchievements());
                }
            }
            
            log.info("✅ Consulta de logros completada para biblioteca del usuario {}", steamId);
            
            // 5. FILTRO: Excluir juegos de test/beta/privados/servers
            int sizeBeforeFilter = juegos.size();
            juegos = juegos.stream()
                    .filter(j -> !shouldExcludeGame(j.getName()))
                    .collect(Collectors.toList());
            log.info("Juegos después de filtro por nombre: {} (antes: {})", juegos.size(), sizeBeforeFilter);
            
            // 6. Construir respuesta
            BibliotecaUsuarioDto biblioteca = BibliotecaUsuarioDto.builder()
                    .steamId(steamId)
                    .personaName(personaName)
                    .avatarUrl(avatarUrl)
                    .totalJuegos(juegos.size()) // Usar tamaño filtrado
                    .juegos(juegos)
                    .build();
            
            return biblioteca;

        } catch (Exception e) {
            log.error("Error al obtener biblioteca del usuario {}: {}", steamId, e.getMessage(), e);
            throw new RuntimeException("Error al obtener biblioteca del usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public RefreshJuegoResponseDto refreshJuego(String steamId, Integer appId) {
        log.info("=== INICIANDO REFRESH COMPLETO ===");
        log.info("Usuario: {}, AppId: {}", steamId, appId);

        try {
            // 1. Obtener detalles del juego desde Steam AppDetails
            log.info("PASO 1: Obteniendo detalles desde Steam API");
            SteamGameDto appDetails = apiConectorService.getSteamGameDetails(appId.toString());
            log.info("✅ Detalles obtenidos: {}", appDetails.getName());

            // 2. Obtener imagen de biblioteca
            String libraryImageUrl = String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/library_600x900.jpg", appId);
            
            // Verificar si la imagen existe
            try {
                restTemplate.headForHeaders(java.util.Objects.requireNonNull(libraryImageUrl));
            } catch (Exception e) {
                log.warn("Imagen de biblioteca no disponible para appId {}", appId);
                libraryImageUrl = null;
            }

            // 3. Extraer tags del juego (máximo 5)
            java.util.List<Tags> tags = new java.util.ArrayList<>();
            if (appDetails.getGenres() != null && appDetails.getGenres().length > 0) {
                int count = 0;
                for (SteamGameDto.GenreDto genre : appDetails.getGenres()) {
                    if (count >= 5) break;
                    tags.add(Tags.builder().tag(genre.getDescription()).build());
                    count++;
                }
            }

            // 4. Construir DTO para backend
            BibliotecaJuegoDto bibliotecaDto = BibliotecaJuegoDto.builder()
                    .appId(Long.valueOf(appId))
                    .name(appDetails.getName())
                    .headerImage(appDetails.getHeaderImage())
                    .imgVertical(libraryImageUrl)
                    .imgIconUrl(String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/capsule_184x69.jpg", appId))
                    .price(formatPrice(appDetails))
                    .isFree(appDetails.getIsFree())
                    .storeUrl("https://store.steampowered.com/app/" + appId)
                    .tags(tags)
                    .build();

            // 5. GUARDAR/ACTUALIZAR en backend
            log.info("PASO 2: Guardando/actualizando juego en BD");
            BibliotecaJuegoDto persisted = null;
            try {
                persisted = apiBackendClient.upsertBibliotecaJuego(bibliotecaDto);
                log.info("✅ Juego guardado en BD: {}", persisted.getName());
            } catch (Exception e) {
                log.error("❌ Error al guardar juego en BD: {}", e.getMessage());
            }

            // 6. REFRESCAR LOGROS desde Steam API (borra los existentes y recarga desde Steam)
            log.info("PASO 3: Refrescando logros desde Steam API");
            log.info("🔄 Se eliminarán todos los logros existentes de este juego para el usuario y se recargarán desde Steam");
            try {
                apiBackendClient.ingestUserGameAchievements(steamId, Long.valueOf(appId));
                log.info("✅ Logros eliminados y recargados exitosamente desde Steam API");
            } catch (Exception e) {
                log.warn("⚠️ Error al refrescar logros (puede ser normal si el juego no tiene logros): {}", e.getMessage());
            }

            // 7. Construir respuesta
            JuegoUsuarioDto juegoActualizado = JuegoUsuarioDto.builder()
                    .appId(appId)
                    .name(persisted != null ? persisted.getName() : appDetails.getName())
                    .headerImage(persisted != null ? persisted.getHeaderImage() : appDetails.getHeaderImage())
                    .price(persisted != null ? persisted.getPrice() : formatPrice(appDetails))
                    .isFree(persisted != null ? persisted.getIsFree() : appDetails.getIsFree())
                    .storeUrl("https://store.steampowered.com/app/" + appId)
                    .libraryImage(libraryImageUrl)
                    .build();

            log.info("=== REFRESH COMPLETO FINALIZADO ===");
            return RefreshJuegoResponseDto.builder()
                    .appId(appId)
                    .success(true)
                    .message("Juego y logros actualizados exitosamente")
                    .juegoActualizado(juegoActualizado)
                    .build();

        } catch (Exception e) {
            log.error("❌ Error al hacer refresh del juego {}: {}", appId, e.getMessage(), e);
            return RefreshJuegoResponseDto.builder()
                    .appId(appId)
                    .success(false)
                    .message("Error al actualizar el juego: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Convierte un OwnedGameDto a JuegoUsuarioDto
     */
    private JuegoUsuarioDto convertToJuegoUsuarioDto(SteamOwnedGamesResponseDto.OwnedGameDto ownedGame) {
        String headerImageUrl = String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/header.jpg", ownedGame.getAppId());
        
    return JuegoUsuarioDto.builder()
        .appId(ownedGame.getAppId().intValue())
        .name(ownedGame.getName())
        .playtimeForever(ownedGame.getPlaytimeForever())
        // playtime2Weeks se completará luego con la llamada a recently played
        .playtime2Weeks(0)
        .headerImage(headerImageUrl)
        .storeUrl("https://store.steampowered.com/app/" + ownedGame.getAppId())
        .build();
    }

    /**
     * Crea una biblioteca vacía para un usuario
     */
    private BibliotecaUsuarioDto createEmptyBiblioteca(String steamId) {
        return BibliotecaUsuarioDto.builder()
                .steamId(steamId)
                .personaName("Usuario")
                .totalJuegos(0)
                .juegos(new ArrayList<>())
                .build();
    }

    /**
     * Formatea el precio del juego
     */
    private String formatPrice(SteamGameDto game) {
        if (game.getIsFree() != null && game.getIsFree()) {
            return "Gratis";
        }
        
        if (game.getPriceOverview() != null) {
            String finalPrice = game.getPriceOverview().getFinalFormatted();
            if (finalPrice != null && !finalPrice.isEmpty()) {
                return finalPrice;
            }
        }
        
        return "N/A";
    }

	@Override
	public JuegoAleatorioDto getJuegoAleatorio(String steamId, Integer maxHoras, String tags, Long excludeAppId) {
		log.info("Obteniendo juego aleatorio para usuario: {}, maxHoras: {}, tags: {}, excludeAppId: {}", steamId, maxHoras, tags, excludeAppId);        try {
            // 1. Obtener biblioteca del usuario
            SteamOwnedGamesResponseDto ownedGames = apiConectorService.getUserOwnedGames(steamId, true, true);
            
            if (ownedGames == null || ownedGames.getResponse() == null || 
                ownedGames.getResponse().getGames() == null || 
                ownedGames.getResponse().getGames().isEmpty()) {
                log.warn("No se encontraron juegos para el usuario: {}", steamId);
                throw new RuntimeException("El usuario no tiene juegos en su biblioteca");
            }

			// 2. Parsear tags si vienen
			List<String> tagsABuscar = new ArrayList<>();
			if (tags != null && !tags.trim().isEmpty()) {
				tagsABuscar = Arrays.asList(tags.split(","));
				log.info("Tags a buscar: {}", tagsABuscar);
			}
			final List<String> tagsFinales = tagsABuscar;

			// 3. Filtrar juegos según criterio (no jugados o con menos de maxHoras)
			List<SteamOwnedGamesResponseDto.OwnedGameDto> juegosFiltrados = ownedGames.getResponse().getGames().stream()
				.filter(game -> {
					Integer playtimeMinutes = game.getPlaytimeForever();
					if (playtimeMinutes == null) playtimeMinutes = 0;
					
					// Convertir maxHoras a minutos
					if (maxHoras == null) {
						// Solo juegos no jugados (0 minutos)
						return playtimeMinutes == 0;
					} else {
						// Juegos con menos de maxHoras
						int maxMinutos = maxHoras * 60;
						return playtimeMinutes <= maxMinutos;
					}
				})
				.collect(Collectors.toList());			if (juegosFiltrados.isEmpty()) {
				String criterio = maxHoras == null ? "no jugados" : "con menos de " + maxHoras + " horas";
				log.warn("No se encontraron juegos {} para el usuario: {}", criterio, steamId);
				throw new RuntimeException("No hay juegos " + criterio + " en tu biblioteca");
			}

			// 4. Si hay tags, filtrar primero a nivel de base de datos (OPTIMIZACIÓN)
			if (!tagsFinales.isEmpty()) {
				log.info("Filtrando {} juegos por tags a nivel de BD: {}", juegosFiltrados.size(), tagsFinales);
				
				// Llamar al backend para obtener appIds que tengan al menos una de las tags
				List<Long> appIdsConTags = null;
				try {
					appIdsConTags = apiBackendClient.buscarAppIdsPorTags(tagsFinales);
					log.info("Backend encontró {} juegos con las tags especificadas", appIdsConTags != null ? appIdsConTags.size() : 0);
				} catch (Exception e) {
					log.error("Error al buscar juegos por tags en backend: {}", e.getMessage());
					throw new RuntimeException("Error al filtrar por tags");
				}
				
				if (appIdsConTags == null || appIdsConTags.isEmpty()) {
					log.warn("No se encontraron juegos con las tags especificadas: {}", tagsFinales);
					throw new RuntimeException("No se encontraron juegos con las tags: " + String.join(", ", tagsFinales));
				}
				
				// Crear un Set para búsqueda rápida
				final java.util.Set<Long> appIdsConTagsSet = new java.util.HashSet<>(appIdsConTags);
				
				// Filtrar la lista de juegos del usuario para quedarnos solo con los que tienen las tags
				juegosFiltrados = juegosFiltrados.stream()
					.filter(game -> appIdsConTagsSet.contains(game.getAppId()))
					.collect(Collectors.toList());
				
				log.info("Juegos del usuario filtrados por tags: {}", juegosFiltrados.size());
				
				if (juegosFiltrados.isEmpty()) {
					log.warn("El usuario no tiene juegos con las tags especificadas: {}", tagsFinales);
					throw new RuntimeException("No tienes juegos en tu biblioteca con las tags: " + String.join(", ", tagsFinales));
				}
			}
			
			// 5. Excluir appId si se especificó (para evitar repetir el último juego)
			if (excludeAppId != null) {
				log.info("Excluyendo appId {} de la selección", excludeAppId);
				int sizeBeforeExclude = juegosFiltrados.size();
				juegosFiltrados = juegosFiltrados.stream()
					.filter(game -> !game.getAppId().equals(excludeAppId))
					.collect(Collectors.toList());
				log.info("Juegos después de excluir: {} (antes: {})", juegosFiltrados.size(), sizeBeforeExclude);
				
				if (juegosFiltrados.isEmpty()) {
					log.warn("No quedan juegos después de excluir el appId {}", excludeAppId);
					throw new RuntimeException("No hay otros juegos disponibles con los criterios seleccionados");
				}
			}
			
			// 6. FILTRO: Excluir juegos de test/beta/privados/servers por nombre
			int sizeBeforeNameFilter = juegosFiltrados.size();
			juegosFiltrados = juegosFiltrados.stream()
				.filter(game -> !shouldExcludeGame(game.getName()))
				.collect(Collectors.toList());
			log.info("Juegos después de filtro por nombre: {} (antes: {})", juegosFiltrados.size(), sizeBeforeNameFilter);
			
			if (juegosFiltrados.isEmpty()) {
				log.warn("No quedan juegos después de aplicar filtros de nombre");
				throw new RuntimeException("No hay juegos disponibles después de aplicar todos los filtros");
			}
			
            // 7. Seleccionar un juego aleatorio
            java.util.Random random = new java.util.Random();
            SteamOwnedGamesResponseDto.OwnedGameDto juegoSeleccionado = 
                juegosFiltrados.get(random.nextInt(juegosFiltrados.size()));

            Long appId = juegoSeleccionado.getAppId();
            log.info("Juego aleatorio seleccionado: {} (appId: {})", juegoSeleccionado.getName(), appId);

            // 8. Obtener información completa del juego desde el backend (que enriquece con Steam AppDetails)
            BibliotecaJuegoDto juegoCompleto = null;
            try {
                juegoCompleto = apiBackendClient.getBibliotecaJuego(appId);
            } catch (Exception e) {
                log.warn("No se pudo obtener juego desde backend, continuando con datos básicos: {}", e.getMessage());
            }

            // 5. Construir respuesta con toda la información disponible
            JuegoAleatorioDto.JuegoAleatorioDtoBuilder builder = JuegoAleatorioDto.builder()
                .appId(appId)
                .name(juegoSeleccionado.getName())
                .playtimeForever(juegoSeleccionado.getPlaytimeForever())
                .headerImage(String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/header.jpg", appId))
                .storeUrl("https://store.steampowered.com/app/" + appId);

            // Enriquecer con datos básicos del backend si están disponibles
            if (juegoCompleto != null) {
                builder.libraryImage(juegoCompleto.getImgVertical())
                    .isFree(juegoCompleto.getIsFree());

                // Formatear precio
                if (juegoCompleto.getIsFree() != null && juegoCompleto.getIsFree()) {
                    builder.price("Gratis");
                } else if (juegoCompleto.getPrice() != null) {
                    builder.price(juegoCompleto.getPrice());
                }

                // Tags (usar los campos disponibles en BibliotecaJuegoDto)
                if (juegoCompleto.getTags() != null && !juegoCompleto.getTags().isEmpty()) {
                    List<String> tagNames = juegoCompleto.getTags().stream()
                        .map(tag -> tag.getTag())
                        .filter(tag -> tag != null && !tag.isEmpty())
                        .collect(Collectors.toList());
                    builder.tags(tagNames);
                }
            }
            
            // Intentar obtener más información directamente desde Steam AppDetails si es necesario
            try {
                SteamGameDto steamDetails = apiConectorClient.getSteamGameDetails(appId.toString());
                if (steamDetails != null) {
                    // Enriquecer con información de Steam
                    if (steamDetails.getShortDescription() != null && !steamDetails.getShortDescription().isEmpty()) {
                        builder.shortDescription(steamDetails.getShortDescription());
                    }
                    if (steamDetails.getDetailedDescription() != null && !steamDetails.getDetailedDescription().isEmpty()) {
                        builder.description(steamDetails.getDetailedDescription());
                    }
                    if (steamDetails.getHeaderImage() != null && !steamDetails.getHeaderImage().isEmpty()) {
                        builder.backgroundImage(steamDetails.getHeaderImage());
                    }
                    if (steamDetails.getReleaseDate() != null && steamDetails.getReleaseDate().getDate() != null) {
                        builder.releaseDate(steamDetails.getReleaseDate().getDate());
                    }
                    
                    // Developers (es un array de String, no una lista)
                    if (steamDetails.getDevelopers() != null && steamDetails.getDevelopers().length > 0) {
                        builder.developers(java.util.Arrays.asList(steamDetails.getDevelopers()));
                    }
                    
                    // Publishers (es un array de String, no una lista)
                    if (steamDetails.getPublishers() != null && steamDetails.getPublishers().length > 0) {
                        builder.publishers(java.util.Arrays.asList(steamDetails.getPublishers()));
                    }
                    
                    // Genres (es un array, no una lista)
                    if (steamDetails.getGenres() != null && steamDetails.getGenres().length > 0) {
                        List<String> genreNames = java.util.Arrays.stream(steamDetails.getGenres())
                            .map(genre -> genre.getDescription())
                            .filter(desc -> desc != null && !desc.isEmpty())
                            .collect(Collectors.toList());
                        if (!genreNames.isEmpty()) {
                            builder.genres(genreNames);
                        }
                    }
                    
                    // Categories (es un array, no una lista)
                    if (steamDetails.getCategories() != null && steamDetails.getCategories().length > 0) {
                        List<String> categoryNames = java.util.Arrays.stream(steamDetails.getCategories())
                            .map(category -> category.getDescription())
                            .filter(desc -> desc != null && !desc.isEmpty())
                            .collect(Collectors.toList());
                        if (!categoryNames.isEmpty()) {
                            builder.categories(categoryNames);
                        }
                    }
                    
                    // Screenshots (es un array, no una lista)
                    if (steamDetails.getScreenshots() != null && steamDetails.getScreenshots().length > 0) {
                        List<String> screenshotUrls = java.util.Arrays.stream(steamDetails.getScreenshots())
                            .map(screenshot -> screenshot.getPathFull())
                            .filter(url -> url != null && !url.isEmpty())
                            .collect(Collectors.toList());
                        if (!screenshotUrls.isEmpty()) {
                            builder.screenshots(screenshotUrls);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("No se pudo enriquecer con AppDetails de Steam para appId {}: {}", appId, e.getMessage());
                // Continuar con los datos básicos que ya tenemos
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error al obtener juego aleatorio para usuario {}: {}", steamId, e.getMessage(), e);
            throw new RuntimeException("Error al buscar juego aleatorio: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public GameAchievementsDto getGameAchievements(String steamId, Long appId) {
        log.info("Obteniendo logros del juego {} para usuario {}", appId, steamId);

        try {
            // 1. Intentar obtener desde la base de datos primero
            Map<String, Object> dbResult = null;
            try {
                dbResult = apiBackendClient.getAchievementsFromDatabase(steamId, appId);
                
                if (dbResult != null && Boolean.TRUE.equals(dbResult.get("success"))) {
                    log.info("Logros encontrados en BD para usuario {} appId {}", steamId, appId);
                    return convertMapToAchievementsDto(dbResult);
                }
            } catch (Exception e) {
                log.warn("Error al consultar BD para logros de appId {}: {}. Intentando desde Steam API...", appId, e.getMessage());
            }
            
            log.debug("Logros no encontrados en BD, consultando Steam API y guardando...");

            // 2. Si no está en BD, ingestar desde Steam API (llama al backend que hace ingesta)
            try {
                Map<String, Object> ingestResult = apiBackendClient.ingestUserGameAchievements(steamId, appId);
                
                if (ingestResult != null && Boolean.TRUE.equals(ingestResult.get("success"))) {
                    log.info("Logros ingested exitosamente para usuario {} appId {}", steamId, appId);
                    
                    // Ahora obtener desde BD
                    try {
                        dbResult = apiBackendClient.getAchievementsFromDatabase(steamId, appId);
                        if (dbResult != null && Boolean.TRUE.equals(dbResult.get("success"))) {
                            return convertMapToAchievementsDto(dbResult);
                        }
                    } catch (Exception e) {
                        log.warn("Error al obtener logros desde BD después de ingesta para appId {}: {}", appId, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Error al ingestar logros desde Steam API para appId {}: {}", appId, e.getMessage());
            }

            // 3. Si la ingesta falla, intentar obtener directamente desde Steam API (fallback legacy)
            log.debug("Ingesta fallida, obteniendo directamente desde Steam API como fallback...");
            return getAchievementsDirectlyFromSteam(steamId, appId);

        } catch (Exception e) {
            log.error("Error al obtener logros del juego {} para usuario {}: {}", appId, steamId, e.getMessage(), e);
            return GameAchievementsDto.builder()
                    .steamId(steamId)
                    .appId(appId)
                    .success(false)
                    .error("Error al obtener logros: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Método auxiliar para obtener logros directamente desde Steam API (fallback)
     * Esto NO guarda en BD, solo obtiene los datos en tiempo real
     */
    @SuppressWarnings("unchecked")
    private GameAchievementsDto getAchievementsDirectlyFromSteam(String steamId, Long appId) {
        try {
            // 2.1 Obtener logros del jugador
            Map<String, Object> playerAchievementsRaw = null;
            try {
                playerAchievementsRaw = apiConectorClient.getPlayerAchievements(steamId, appId.toString());
            } catch (Exception e) {
                log.warn("Error al obtener logros del jugador para appId {}: {}", appId, e.getMessage());
                // Si falla la llamada, es probable que el juego no tenga logros o el perfil sea privado
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("No existen logros para este juego o el perfil es privado")
                        .build();
            }
            
            // 2. Obtener esquema de logros del juego
            Map<String, Object> achievementSchemaRaw = null;
            try {
                achievementSchemaRaw = apiConectorClient.getGameAchievementSchema(appId.toString());
            } catch (feign.FeignException.NotFound e) {
                log.warn("Schema no encontrado (404) para appId {}: El juego no tiene logros", appId);
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("Este juego no tiene logros disponibles")
                        .build();
            } catch (feign.FeignException.InternalServerError e) {
                log.warn("Error interno del servidor (500) al obtener schema para appId {}: La API de Steam no pudo procesar la solicitud", appId);
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("Este juego no tiene logros disponibles o la información no está accesible actualmente")
                        .build();
            } catch (feign.FeignException e) {
                log.warn("Error Feign al obtener esquema de logros para appId {}: {} (Status: {})", 
                        appId, e.getMessage(), e.status());
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("No se pudo obtener información de logros para este juego")
                        .build();
            } catch (Exception e) {
                log.warn("Error inesperado al obtener esquema de logros para appId {}: {}", appId, e.getMessage());
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("No existen logros para este juego")
                        .build();
            }

            // Verificar que las respuestas no estén vacías
            if (playerAchievementsRaw == null || playerAchievementsRaw.isEmpty()) {
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("No existen logros para este juego")
                        .build();
            }

            if (achievementSchemaRaw == null || achievementSchemaRaw.isEmpty()) {
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("No existen logros para este juego")
                        .build();
            }

            // Verificar que las respuestas sean exitosas
            Map<String, Object> playerstats = (Map<String, Object>) playerAchievementsRaw.get("playerstats");
            if (playerstats == null || !(Boolean) playerstats.getOrDefault("success", false)) {
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("El juego no tiene logros o el perfil es privado")
                        .build();
            }

            Map<String, Object> game = (Map<String, Object>) achievementSchemaRaw.get("game");
            if (game == null || game.isEmpty()) {
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .success(false)
                        .error("No existen logros para este juego")
                        .build();
            }

            // Obtener información básica
            String gameName = (String) playerstats.getOrDefault("gameName", "");
            List<Map<String, Object>> playerAchievementsList = (List<Map<String, Object>>) playerstats.get("achievements");
            Object availableGameStatsObj = game.get("availableGameStats");

            if (availableGameStatsObj == null) {
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .gameName(gameName)
                        .success(false)
                        .error("No existen logros para este juego")
                        .build();
            }
            
            // playerAchievementsList puede ser null si el jugador no ha desbloqueado ningún logro aún
            // Esto NO significa que el juego no tenga logros
            if (playerAchievementsList == null) {
                playerAchievementsList = new ArrayList<>();
                log.debug("Player has no achievement data yet for appId: {}", appId);
            }

            // availableGameStats puede ser un Map si solo hay un elemento o una List
            List<Map<String, Object>> schemaAchievements = null;
            
            log.debug("availableGameStatsObj type: {}", availableGameStatsObj.getClass().getName());
            log.debug("availableGameStatsObj content: {}", availableGameStatsObj);
            
            if (availableGameStatsObj instanceof Map) {
                Map<String, Object> statsMap = (Map<String, Object>) availableGameStatsObj;
                
                // Intentar obtener achievements directamente
                Object achievementsObj = statsMap.get("achievements");
                if (achievementsObj instanceof List) {
                    schemaAchievements = (List<Map<String, Object>>) achievementsObj;
                    log.debug("Found achievements list in Map: {} items", schemaAchievements.size());
                } else {
                    log.warn("No achievements list found in Map. Map keys: {}", statsMap.keySet());
                }
            } else if (availableGameStatsObj instanceof List) {
                List<Map<String, Object>> availableGameStats = (List<Map<String, Object>>) availableGameStatsObj;
                log.debug("availableGameStats is a List with {} items", availableGameStats.size());
                if (!availableGameStats.isEmpty()) {
                    Map<String, Object> firstItem = availableGameStats.get(0);
                    Object achievementsObj = firstItem.get("achievements");
                    if (achievementsObj instanceof List) {
                        schemaAchievements = (List<Map<String, Object>>) achievementsObj;
                        log.debug("Found achievements list in first item: {} items", schemaAchievements.size());
                    }
                }
            }
            
            if (schemaAchievements == null || schemaAchievements.isEmpty()) {
                log.warn("No schema achievements found for appId: {}. availableGameStatsObj was: {}", appId, availableGameStatsObj);
                return GameAchievementsDto.builder()
                        .steamId(steamId)
                        .appId(appId)
                        .gameName(gameName)
                        .success(false)
                        .error("Este juego no tiene logros disponibles")
                        .build();
            }

            // Crear mapa de logros del jugador por apiname para búsqueda rápida
            Map<String, Map<String, Object>> playerAchievementsMap = playerAchievementsList.stream()
                    .collect(Collectors.toMap(
                            a -> (String) a.get("apiname"),
                            a -> a,
                            (a1, a2) -> a1 // En caso de duplicados, mantener el primero
                    ));

            // Combinar información usando el esquema como base (para obtener TODOS los logros)
            // Esto garantiza que se muestren todos los logros del juego, no solo los que el jugador ha tocado
            List<CombinedAchievementDto> combinedAchievements = new ArrayList<>();
            int unlockedCount = 0;

            for (Map<String, Object> schema : schemaAchievements) {
                String apiname = (String) schema.get("name");
                String displayName = (String) schema.getOrDefault("displayName", apiname);
                String description = (String) schema.getOrDefault("description", "");
                String icon = (String) schema.getOrDefault("icon", "");
                String icongray = (String) schema.getOrDefault("icongray", icon);
                Integer hidden = (Integer) schema.getOrDefault("hidden", 0);

                // Buscar si el jugador tiene progreso en este logro
                Map<String, Object> playerAch = playerAchievementsMap.get(apiname);
                Integer achieved = 0;
                Long unlocktime = 0L;
                
                if (playerAch != null) {
                    achieved = (Integer) playerAch.getOrDefault("achieved", 0);
                    unlocktime = ((Number) playerAch.getOrDefault("unlocktime", 0)).longValue();
                }

                CombinedAchievementDto combined = CombinedAchievementDto.builder()
                        .apiname(apiname)
                        .displayName(displayName)
                        .description(description)
                        .icon(icon)
                        .icongray(icongray)
                        .achieved(achieved == 1)
                        .unlocktime(unlocktime)
                        .hidden(hidden == 1)
                        .build();

                combinedAchievements.add(combined);

                if (achieved == 1) {
                    unlockedCount++;
                }
            }

            return GameAchievementsDto.builder()
                    .steamId(steamId)
                    .appId(appId)
                    .gameName(gameName)
                    .totalAchievements(combinedAchievements.size())
                    .unlockedAchievements(unlockedCount)
                    .achievements(combinedAchievements)
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("Error al obtener logros del juego {} para usuario {}: {}", appId, steamId, e.getMessage(), e);
            return GameAchievementsDto.builder()
                    .steamId(steamId)
                    .appId(appId)
                    .success(false)
                    .error("Error al obtener logros: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public GameAchievementsDto refreshGameAchievements(String steamId, Long appId) {
        log.info("Refrescando forzadamente logros del juego {} para usuario {} desde Steam API", appId, steamId);
        
        try {
            // 1. Forzar ingesta desde Steam API (esto llama al backend que guarda en BD)
            Map<String, Object> ingestResult = apiBackendClient.ingestUserGameAchievements(steamId, appId);
            
            if (ingestResult != null && Boolean.TRUE.equals(ingestResult.get("success"))) {
                log.info("Logros refrescados exitosamente para usuario {} appId {}", steamId, appId);
                
                // 2. Obtener desde BD los datos actualizados
                try {
                    Map<String, Object> dbResult = apiBackendClient.getAchievementsFromDatabase(steamId, appId);
                    if (dbResult != null && Boolean.TRUE.equals(dbResult.get("success"))) {
                        return convertMapToAchievementsDto(dbResult);
                    }
                } catch (Exception e) {
                    log.warn("Error al obtener logros desde BD después de refresh para appId {}: {}", appId, e.getMessage());
                }
            }
            
            // Si falla la ingesta o no se pueden obtener desde BD, fallback a Steam API directo
            log.warn("Refresh desde BD falló, obteniendo directamente desde Steam API");
            return getAchievementsDirectlyFromSteam(steamId, appId);
            
        } catch (Exception e) {
            log.error("Error al refrescar logros del juego {} para usuario {}: {}", appId, steamId, e.getMessage(), e);
            return GameAchievementsDto.builder()
                    .steamId(steamId)
                    .appId(appId)
                    .success(false)
                    .error("Error al refrescar logros: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public SyncBibliotecaResponseDto syncBiblioteca(String steamId) {
        log.info("Iniciando sincronización de biblioteca para usuario: {}", steamId);
        
        int total = 0;
        int sincronizados = 0;
        int errores = 0;
        List<String> juegosConError = new ArrayList<>();

        try {
            // 1. Obtener todos los juegos del usuario desde Steam
            SteamOwnedGamesResponseDto ownedGames = apiConectorService.getUserOwnedGames(steamId, true, true);
            
            if (ownedGames == null || ownedGames.getResponse() == null || ownedGames.getResponse().getGames() == null) {
                log.warn("No se encontraron juegos para el usuario: {}", steamId);
                return SyncBibliotecaResponseDto.builder()
                        .total(0)
                        .sincronizados(0)
                        .errores(0)
                        .mensaje("No se encontraron juegos en la biblioteca del usuario")
                        .juegosConError(new ArrayList<>())
                        .build();
            }

            List<SteamOwnedGamesResponseDto.OwnedGameDto> games = ownedGames.getResponse().getGames();
            total = games.size();
            log.info("Encontrados {} juegos en la biblioteca del usuario {}", total, steamId);

            // 2. Para cada juego, verificar si existe en DB y enriquecerlo si no existe
            for (SteamOwnedGamesResponseDto.OwnedGameDto game : games) {
                Long appId = game.getAppId();
                if (appId == null) {
                    continue;
                }

                try {
                    // Verificar si el juego ya existe en la base de datos
                    List<BibliotecaJuegoDto> dbGames = apiBackendClient.getBibliotecaJuegosDesdeDb(List.of(appId));
                    
                    if (dbGames != null && !dbGames.isEmpty()) {
                        // Ya existe en DB, no hacer nada
                        log.debug("Juego {} ya existe en DB, saltando", appId);
                        continue;
                    }

                    // No existe en DB, hacer llamadas para enriquecer
                    log.info("🔄 Sincronizando: {} ({})", game.getName(), appId);

                    // Llamar a appdetails y steamspy
                    @SuppressWarnings("unchecked")
                    Map<String, Object> appDetails = null;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> steamSpyDetails = null;

                    try {
                        // Llamada a Steam App Details
                        appDetails = apiConectorClient.getGameDetailsFromStore(String.valueOf(appId));
                    } catch (Exception e) {
                        log.warn("Error al obtener appdetails para {}: {}", appId, e.getMessage());
                    }

                    try {
                        // Llamada a SteamSpy
                        steamSpyDetails = apiConectorClient.getSteamSpyAppDetails(String.valueOf(appId));
                    } catch (Exception e) {
                        log.warn("Error al obtener steamspy para {}: {}", appId, e.getMessage());
                    }

                    // Procesar y guardar en la base de datos
                    boolean saved = procesarYGuardarJuego(appId, game.getName(), game.getImgIconUrl(), 
                                                         appDetails, steamSpyDetails);

                    if (saved) {
                        sincronizados++;
                        log.info("✅ Juego {} ({}) sincronizado exitosamente", game.getName(), appId);
                        
                        // Ingestar logros del juego inmediatamente después de guardarlo
                        try {
                            log.info("🏆 Sincronizando logros para {} ({})", game.getName(), appId);
                            boolean achievementsIngested = backendClient.ingestarLogrosJuegoIndividual(steamId, appId);
                            if (achievementsIngested) {
                                log.info("✅ Logros sincronizados para {} ({})", game.getName(), appId);
                            } else {
                                log.warn("⚠️ No se pudieron sincronizar logros para {} ({})", game.getName(), appId);
                            }
                        } catch (Exception eAch) {
                            log.warn("⚠️ Error al sincronizar logros para {} ({}): {}", game.getName(), appId, eAch.getMessage());
                        }
                    } else {
                        errores++;
                        juegosConError.add(game.getName() + " (" + appId + ")");
                        log.warn("❌ No se pudo sincronizar el juego {} ({})", game.getName(), appId);
                    }

                    // Esperar 2 segundos antes de la siguiente llamada
                    Thread.sleep(2000);

                } catch (Exception e) {
                    log.error("❌ Error al sincronizar juego {} ({}): {}", game.getName(), appId, e.getMessage());
                    errores++;
                    juegosConError.add(game.getName() + " (" + appId + "): " + e.getMessage());
                }
            }

            String mensaje = String.format("Sincronización completada: %d de %d juegos actualizados", 
                                         sincronizados, total);
            if (errores > 0) {
                mensaje += String.format(" (%d error%s)", errores, errores > 1 ? "es" : "");
            }

            log.info(mensaje);

            return SyncBibliotecaResponseDto.builder()
                    .total(total)
                    .sincronizados(sincronizados)
                    .errores(errores)
                    .mensaje(mensaje)
                    .juegosConError(juegosConError)
                    .build();

        } catch (Exception e) {
            log.error("Error general en sincronización de biblioteca para {}: {}", steamId, e.getMessage(), e);
            return SyncBibliotecaResponseDto.builder()
                    .total(total)
                    .sincronizados(sincronizados)
                    .errores(errores)
                    .mensaje("Error al sincronizar biblioteca: " + e.getMessage())
                    .juegosConError(juegosConError)
                    .build();
        }
    }

    /**
     * Procesa y guarda un juego en la base de datos con información de appdetails y steamspy
     */
    private boolean procesarYGuardarJuego(Long appId, String gameName, String imgIconUrl,
                                          Map<String, Object> appDetails, 
                                          Map<String, Object> steamSpyDetails) {
        try {
            // Extraer información de appDetails
            String headerImage = null;
            String price = null;
            Boolean isFree = null;
            String name = gameName;

            if (appDetails != null && !appDetails.isEmpty()) {
                Map<String, Object> data = extractDataFromAppDetails(appDetails, appId);
                
                if (data != null) {
                    // Nombre
                    Object nameObj = data.get("name");
                    if (nameObj != null) {
                        name = String.valueOf(nameObj);
                    }

                    // Header image
                    Object headerObj = data.get("header_image");
                    if (headerObj != null) {
                        headerImage = String.valueOf(headerObj);
                    }

                    // Price overview
                    Object priceOverview = data.get("price_overview");
                    if (priceOverview instanceof Map) {
                        Map<?, ?> priceMap = (Map<?, ?>) priceOverview;
                        Object finalFormatted = priceMap.get("final_formatted");
                        if (finalFormatted != null) {
                            price = String.valueOf(finalFormatted);
                        }
                    }

                    // Is free
                    Object isFreeObj = data.get("is_free");
                    if (isFreeObj instanceof Boolean) {
                        isFree = (Boolean) isFreeObj;
                    } else if (isFreeObj != null) {
                        isFree = Boolean.valueOf(String.valueOf(isFreeObj));
                    }
                }
            }

            // Extraer tags de SteamSpy
            List<String> tags = new ArrayList<>();
            if (steamSpyDetails != null) {
                Object tagObj = steamSpyDetails.get("tags");
                if (tagObj instanceof Map) {
                    Map<?, ?> tagMap = (Map<?, ?>) tagObj;
                    for (Object key : tagMap.keySet()) {
                        tags.add(String.valueOf(key));
                    }
                }

                // Genre
                Object genreObj = steamSpyDetails.get("genre");
                if (genreObj != null) {
                    String genreStr = String.valueOf(genreObj);
                    String[] genres = genreStr.split(",");
                    for (String genre : genres) {
                        String trimmedGenre = genre.trim();
                        if (!trimmedGenre.isEmpty()) {
                            tags.add(trimmedGenre);
                        }
                    }
                }
            }

            // Normalizar precio
            if (isFree == null) {
                price = "N/A";
            } else if (isFree) {
                price = "$0 USD";
            } else if (price == null || price.trim().isEmpty()) {
                price = "N/A";
            } else if (!price.toUpperCase().contains("USD")) {
                price = price + " USD";
            }

            // Solo guardar si tiene header image
            if (headerImage != null && !headerImage.trim().isEmpty()) {
                BibliotecaJuegoDto juegoDto = new BibliotecaJuegoDto();
                juegoDto.setAppId(appId);
                juegoDto.setName(name);
                juegoDto.setHeaderImage(headerImage);
                juegoDto.setImgVertical("https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg");
                juegoDto.setImgIconUrl(imgIconUrl);
                juegoDto.setIsFree(isFree);
                juegoDto.setPrice(price);
                
                // Convertir tags a formato Tags
                if (!tags.isEmpty()) {
                    List<Tags> tagsList = tags.stream()
                            .map(tag -> {
                                Tags t = new Tags();
                                t.setTag(tag);
                                return t;
                            })
                            .collect(Collectors.toList());
                    juegoDto.setTags(tagsList);
                }

                // Guardar en backend
                apiBackendClient.upsertBibliotecaJuego(juegoDto);
                log.info("Juego {} guardado exitosamente en DB", appId);
                return true;
            } else {
                log.warn("Juego {} no guardado en DB (sin header_image)", appId);
                return false;
            }

        } catch (Exception e) {
            log.error("Error al procesar y guardar juego {}: {}", appId, e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el objeto 'data' de la respuesta de appdetails
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDataFromAppDetails(Map<String, Object> appDetails, Long appId) {
        Map<String, Object> data = null;
        
        if (appDetails.containsKey("data") && appDetails.get("data") instanceof Map) {
            data = (Map<String, Object>) appDetails.get("data");
        } else if (appDetails.containsKey(String.valueOf(appId)) && appDetails.get(String.valueOf(appId)) instanceof Map) {
            Map<String, Object> wrapper = (Map<String, Object>) appDetails.get(String.valueOf(appId));
            if (wrapper.get("data") instanceof Map) {
                data = (Map<String, Object>) wrapper.get("data");
            }
        } else {
            data = appDetails; // fallback
        }
        
        return data;
    }
    
    /**
     * Convierte el resultado del Backend (Map) a GameAchievementsDto
     */
    private GameAchievementsDto convertMapToAchievementsDto(Map<String, Object> dbResult) {
        String steamId = (String) dbResult.get("steamId");
        Object appIdObj = dbResult.get("appId");
        Long appId = appIdObj instanceof Integer ? ((Integer) appIdObj).longValue() : (Long) appIdObj;
        String gameName = (String) dbResult.get("gameName");
        Object totalObj = dbResult.get("totalAchievements");
        Integer totalAchievements = totalObj instanceof Integer ? (Integer) totalObj : null;
        Object unlockedObj = dbResult.get("unlockedAchievements");
        Integer unlockedAchievements = unlockedObj instanceof Integer ? (Integer) unlockedObj : null;
        
        List<Map<String, Object>> achievementsList = (List<Map<String, Object>>) dbResult.get("achievements");
        List<CombinedAchievementDto> combined = new ArrayList<>();
        
        if (achievementsList != null) {
            for (Map<String, Object> ach : achievementsList) {
                Object unlockTimeObj = ach.get("unlocktime");
                Long unlockTime = null;
                if (unlockTimeObj instanceof Integer) {
                    unlockTime = ((Integer) unlockTimeObj).longValue();
                } else if (unlockTimeObj instanceof Long) {
                    unlockTime = (Long) unlockTimeObj;
                }
                
                CombinedAchievementDto dto = CombinedAchievementDto.builder()
                    .apiname((String) ach.get("apiname"))
                    .displayName((String) ach.get("displayName"))
                    .description((String) ach.get("description"))
                    .icon((String) ach.get("icon"))
                    .icongray((String) ach.get("icongray"))
                    .achieved(Boolean.TRUE.equals(ach.get("achieved")))
                    .unlocktime(unlockTime)
                    .hidden(ach.get("hidden") != null && (Integer) ach.get("hidden") == 1)
                    .build();
                combined.add(dto);
            }
        }
        
        return GameAchievementsDto.builder()
            .steamId(steamId)
            .appId(appId)
            .gameName(gameName)
            .totalAchievements(totalAchievements)
            .unlockedAchievements(unlockedAchievements)
            .achievements(combined)
            .success(true)
            .build();
    }

    @Override
    public RefreshAllGamesResponseDto refreshAllGames(String steamId) {
        log.info("⚠️ Iniciando ACTUALIZACIÓN COMPLETA de biblioteca para usuario: {}", steamId);
        
        int total = 0;
        int actualizados = 0;
        int errores = 0;
        List<String> juegosConError = new ArrayList<>();

        try {
            // 1. Obtener todos los juegos del usuario desde Steam
            SteamOwnedGamesResponseDto ownedGames = apiConectorService.getUserOwnedGames(steamId, true, true);
            
            if (ownedGames == null || ownedGames.getResponse() == null || ownedGames.getResponse().getGames() == null) {
                log.warn("No se encontraron juegos para el usuario: {}", steamId);
                return RefreshAllGamesResponseDto.builder()
                        .total(0)
                        .actualizados(0)
                        .errores(0)
                        .mensaje("No se encontraron juegos en la biblioteca del usuario")
                        .juegosConError(new ArrayList<>())
                        .build();
            }

            List<SteamOwnedGamesResponseDto.OwnedGameDto> games = ownedGames.getResponse().getGames();
            total = games.size();
            log.info("ℹ️ Encontrados {} juegos en la biblioteca del usuario {} - SE ACTUALIZARÁN TODOS", total, steamId);

            // 2. Para CADA juego, llamar a refreshAndUpsert (actualizar o insertar)
            for (SteamOwnedGamesResponseDto.OwnedGameDto game : games) {
                Long appId = game.getAppId();
                if (appId == null) {
                    continue;
                }

                try {
                    log.info("🔄 Actualizando: {} ({}) [{}/{}]", game.getName(), appId, (actualizados + errores + 1), total);

                    // Llamar a appdetails, steamspy y obtener imagen
                    @SuppressWarnings("unchecked")
                    Map<String, Object> appDetails = null;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> steamSpyDetails = null;

                    try {
                        // Llamada a Steam App Details
                        appDetails = apiConectorClient.getGameDetailsFromStore(String.valueOf(appId));
                    } catch (Exception e) {
                        log.warn("Error al obtener appdetails para {}: {}", appId, e.getMessage());
                    }

                    try {
                        // Llamada a SteamSpy
                        steamSpyDetails = apiConectorClient.getSteamSpyAppDetails(String.valueOf(appId));
                    } catch (Exception e) {
                        log.warn("Error al obtener steamspy para {}: {}", appId, e.getMessage());
                    }

                    // Procesar y guardar/actualizar en la base de datos
                    boolean saved = procesarYGuardarJuego(appId, game.getName(), game.getImgIconUrl(), 
                                                         appDetails, steamSpyDetails);

                    if (saved) {
                        actualizados++;
                        log.info("✅ Juego {} ({}) actualizado exitosamente [{}/{}]", game.getName(), appId, actualizados, total);
                        
                        // Actualizar logros del juego inmediatamente después de actualizarlo
                        try {
                            log.info("🏆 Actualizando logros para {} ({})", game.getName(), appId);
                            boolean achievementsIngested = backendClient.ingestarLogrosJuegoIndividual(steamId, appId);
                            if (achievementsIngested) {
                                log.info("✅ Logros actualizados para {} ({})", game.getName(), appId);
                            } else {
                                log.warn("⚠️ No se pudieron actualizar logros para {} ({})", game.getName(), appId);
                            }
                        } catch (Exception eAch) {
                            log.warn("⚠️ Error al actualizar logros para {} ({}): {}", game.getName(), appId, eAch.getMessage());
                        }
                    } else {
                        errores++;
                        juegosConError.add(game.getName() + " (" + appId + ")");
                        log.warn("❌ No se pudo actualizar el juego {} ({}) [{}/{}]", game.getName(), appId, errores, total);
                    }

                    // Esperar 2 segundos antes de la siguiente llamada (3 APIs: Steam + SteamSpy + imagen)
                    Thread.sleep(2000);

                } catch (Exception e) {
                    log.error("❌ Error al actualizar juego {} ({}): {}", game.getName(), appId, e.getMessage());
                    errores++;
                    juegosConError.add(game.getName() + " (" + appId + "): " + e.getMessage());
                }
            }

            String mensaje = String.format("Actualización completa finalizada: %d de %d juegos actualizados", 
                                         actualizados, total);
            if (errores > 0) {
                mensaje += String.format(" (%d error%s)", errores, errores > 1 ? "es" : "");
            }

            log.info("🎉 " + mensaje);

            return RefreshAllGamesResponseDto.builder()
                    .total(total)
                    .actualizados(actualizados)
                    .errores(errores)
                    .mensaje(mensaje)
                    .juegosConError(juegosConError)
                    .build();

        } catch (Exception e) {
            log.error("Error general en actualización completa de biblioteca para {}: {}", steamId, e.getMessage(), e);
            return RefreshAllGamesResponseDto.builder()
                    .total(total)
                    .actualizados(actualizados)
                    .errores(errores)
                    .mensaje("Error al actualizar biblioteca: " + e.getMessage())
                    .juegosConError(juegosConError)
                    .build();
        }
    }
}

