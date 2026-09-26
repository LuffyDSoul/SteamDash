package com.dacs.backend.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.dacs.backend.dto.BibliotecaComparacionDto;
import com.dacs.backend.dto.SteamUserGamesInput;
import com.dacs.backend.service.BibliotecaComparacionService;
import com.dacs.backend.repository.GameRecordRepository;
import com.dacs.backend.entity.GameRecord;

/**
 * Implementación del servicio de comparación de bibliotecas
 * SOLO contiene lógica de negocio, NO hace llamadas externas
 * Soporta comparación de 2 a 6 usuarios
 */
@Service
public class BibliotecaComparacionServiceImpl implements BibliotecaComparacionService {

    @Autowired
    private com.dacs.backend.service.impl.GameRecordServiceImpl gameRecordService;

    @Autowired
    private GameRecordRepository gameRecordRepository;

    @Value("${comparison.enrichment.limit:5}")
    private int enrichmentLimit;

    @Value("${comparison.price.free-format:Gratuito}")
    private String freePriceFormat;

    @Value("${comparison.price.missing-format:No disponible}")
    private String missingPriceFormat;
    
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
    public BibliotecaComparacionDto compararBibliotecas(List<SteamUserGamesInput> usuarios) {
        if (usuarios == null || usuarios.size() < 2) {
            throw new IllegalArgumentException("Se requieren al menos 2 usuarios para comparar");
        }
        if (usuarios.size() > 6) {
            throw new IllegalArgumentException("El máximo de usuarios a comparar es 6");
        }
        
        // Filtrar juegos válidos para cada usuario
        List<List<SteamUserGamesInput.GameInfo>> gamesPerUser = new ArrayList<>();
        for (SteamUserGamesInput usuario : usuarios) {
            gamesPerUser.add(filtrarJuegosValidos(usuario.getGames()));
        }
        
        // Mapa auxiliar para tags provenientes de DB
        Map<Long, List<String>> dbTagsById = new HashMap<>();

        // PRIMERO: Cargar valores desde DB para TODOS los juegos (sin llamadas externas)
        try {
            // Recopilar todos los appIds únicos
            java.util.Set<Long> uniqueIds = new java.util.HashSet<>();
            for (List<SteamUserGamesInput.GameInfo> userGames : gamesPerUser) {
                for (SteamUserGamesInput.GameInfo g : userGames) {
                    if (g != null && g.getAppId() != null) uniqueIds.add(g.getAppId());
                }
            }

            if (!uniqueIds.isEmpty()) {
                java.util.List<GameRecord> records = gameRecordRepository.findByAppIdIn(new java.util.ArrayList<>(uniqueIds));
                java.util.Map<Long, GameRecord> byId = new java.util.HashMap<>();
                for (GameRecord r : records) {
                    byId.put(r.getAppId(), r);
                    if (r.getTags() != null && !r.getTags().isEmpty()) {
                        dbTagsById.put(r.getAppId(), r.getTags());
                    }
                }
                // Aplicar datos de DB (price, isFree, headerImage, imgIconUrl, appdetailsFailed) a cada instancia de GameInfo
                for (List<SteamUserGamesInput.GameInfo> userGames : gamesPerUser) {
                    for (SteamUserGamesInput.GameInfo g : userGames) {
                        GameRecord r = byId.get(g.getAppId());
                        if (r != null) {
                            if (r.getHeaderImage() != null) g.setHeaderImage(r.getHeaderImage());
                            if (r.getIsFree() != null) g.setIsFree(r.getIsFree());
                            if (r.getPrice() != null) g.setPrice(r.getPrice());
                            if (g.getImgIconUrl() == null && r.getImgIconUrl() != null) g.setImgIconUrl(r.getImgIconUrl());
                            if (g.getName() == null && r.getName() != null) g.setName(r.getName());
                            if (r.getAppdetailsFailed() != null) g.setAppdetailsFailed(r.getAppdetailsFailed());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: fallo al cargar datos desde DB: " + e.getMessage());
        }

        // SEGUNDO: Enriquecer con llamadas externas hasta el límite configurado (para juegos que no estaban en DB)
        java.util.Map<Long, java.util.List<String>> tagsMap = new java.util.HashMap<>();
        try {
            // Combinar todos los juegos únicos de todos los usuarios
            List<SteamUserGamesInput.GameInfo> allGames = new ArrayList<>();
            Set<Long> addedIds = new java.util.HashSet<>();
            
            for (List<SteamUserGamesInput.GameInfo> userGames : gamesPerUser) {
                for (SteamUserGamesInput.GameInfo game : userGames) {
                    if (!addedIds.contains(game.getAppId())) {
                        allGames.add(game);
                        addedIds.add(game.getAppId());
                    }
                }
            }
            
            // Enriquecer hasta el límite configurado
            tagsMap = gameRecordService.enrichAndStore(allGames, enrichmentLimit);
        } catch (Exception e) {
            System.err.println("Warning: fallo al enriquecer game records: " + e.getMessage());
        }
        
        // Crear mapas para búsqueda rápida: Map<appId, List<índice de usuario que lo tiene>>
        Map<Long, List<Integer>> gameOwnership = new HashMap<>();
        Map<Long, SteamUserGamesInput.GameInfo> gameInfoMap = new HashMap<>();
        
        for (int userIdx = 0; userIdx < gamesPerUser.size(); userIdx++) {
            for (SteamUserGamesInput.GameInfo game : gamesPerUser.get(userIdx)) {
                Long appId = game.getAppId();
                gameOwnership.computeIfAbsent(appId, k -> new ArrayList<>()).add(userIdx);
                gameInfoMap.putIfAbsent(appId, game); // Guardar info del primer usuario que lo tiene
            }
        }
        
        // Lista de juegos en común (al menos 2 usuarios lo tienen)
        List<BibliotecaComparacionDto.JuegoComparacionDto> juegosComunes = new ArrayList<>();
        
        // Mapa de juegos únicos por usuario (solo ese usuario lo tiene)
        Map<String, List<BibliotecaComparacionDto.JuegoComparacionDto>> juegosUnicosPorUsuario = new HashMap<>();
        for (SteamUserGamesInput usuario : usuarios) {
            String claveUsuario = obtenerClaveUsuario(usuario);
            juegosUnicosPorUsuario.put(claveUsuario, new ArrayList<>());
        }
        
        for (Map.Entry<Long, List<Integer>> entry : gameOwnership.entrySet()) {
            Long appId = entry.getKey();
            List<Integer> owners = entry.getValue();
            SteamUserGamesInput.GameInfo gameInfo = gameInfoMap.get(appId);
            
            // FILTRO CRÍTICO: Solo incluir juegos que tienen headerImage
            // Esto excluye juegos que:
            // - No están en la tienda de Steam (appdetails devuelve success: false)
            // - No fueron enriquecidos aún y no están en DB
            // - Fallaron al obtener detalles
            if (gameInfo.getHeaderImage() == null || gameInfo.getHeaderImage().trim().isEmpty()) {
                // Saltar este juego - no se incluirá en la respuesta
                continue;
            }
            
            // FILTRO: Excluir juegos de test/beta/privados/servers
            if (shouldExcludeGame(gameInfo.getName())) {
                continue;
            }
            
            String precio = calcularPrecioFormateado(gameInfo);
            
            if (owners.size() >= 2) {
                // Juego en común (al menos 2 usuarios lo tienen)
                
                // Crear mapa de tiempo jugado por usuario (personaName -> minutos)
                Map<String, Integer> tiemposPorUsuario = new HashMap<>();
                for (Integer userIdx : owners) {
                    SteamUserGamesInput usuario = usuarios.get(userIdx);
                    String claveUsuario = obtenerClaveUsuario(usuario);
                    
                    // Buscar el juego en la lista de ese usuario para obtener su playtime
                    for (SteamUserGamesInput.GameInfo game : gamesPerUser.get(userIdx)) {
                        if (game.getAppId().equals(appId)) {
                            tiemposPorUsuario.put(claveUsuario, game.getPlaytimeForever());
                            break;
                        }
                    }
                }
                
                // Solo establecer imgVertical si el juego tiene headerImage (existe en la tienda)
                String imgVertical = null;
                if (gameInfo.getHeaderImage() != null && !gameInfo.getHeaderImage().trim().isEmpty()) {
                    imgVertical = "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg";
                }
                
                juegosComunes.add(BibliotecaComparacionDto.JuegoComparacionDto.builder()
                    .appId(appId)
                    .name(gameInfo.getName())
                    .headerImage(gameInfo.getHeaderImage())
                    .imgVertical(imgVertical)
                    .imgIconUrl(gameInfo.getImgIconUrl())
                    .isFree(gameInfo.getIsFree())
                    .price(precio)
                    .storeUrl("https://store.steampowered.com/app/" + appId)
                    .cantidadCopias(owners.size())
                    .tiempoJugadoPorUsuario(tiemposPorUsuario)
                    .appdetailsFailed(gameInfo.getAppdetailsFailed())
                    .build());
            } else if (owners.size() == 1) {
                // Juego único (solo 1 usuario lo tiene)
                int ownerIdx = owners.get(0);
                SteamUserGamesInput usuario = usuarios.get(ownerIdx);
                String claveUsuario = obtenerClaveUsuario(usuario);
                
                // Obtener el playtime del usuario
                Integer playtime = null;
                for (SteamUserGamesInput.GameInfo game : gamesPerUser.get(ownerIdx)) {
                    if (game.getAppId().equals(appId)) {
                        playtime = game.getPlaytimeForever();
                        break;
                    }
                }
                
                Map<String, Integer> tiemposPorUsuario = new HashMap<>();
                tiemposPorUsuario.put(claveUsuario, playtime);
                
                // Solo establecer imgVertical si el juego tiene headerImage (existe en la tienda)
                String imgVerticalUnique = null;
                if (gameInfo.getHeaderImage() != null && !gameInfo.getHeaderImage().trim().isEmpty()) {
                    imgVerticalUnique = "https://cdn.akamai.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg";
                }
                
                juegosUnicosPorUsuario.get(claveUsuario).add(
                    BibliotecaComparacionDto.JuegoComparacionDto.builder()
                        .appId(appId)
                        .name(gameInfo.getName())
                        .headerImage(gameInfo.getHeaderImage())
                        .imgVertical(imgVerticalUnique)
                        .imgIconUrl(gameInfo.getImgIconUrl())
                        .isFree(gameInfo.getIsFree())
                        .price(precio)
                        .storeUrl("https://store.steampowered.com/app/" + appId)
                        .cantidadCopias(1)
                        .tiempoJugadoPorUsuario(tiemposPorUsuario)
                        .appdetailsFailed(gameInfo.getAppdetailsFailed())
                        .build()
                );
            }
        }
        
        // Construir DTOs de usuarios
        List<BibliotecaComparacionDto.UsuarioComparacionDto> usuariosDtos = new ArrayList<>();
        for (int i = 0; i < usuarios.size(); i++) {
            SteamUserGamesInput usuario = usuarios.get(i);
            Integer totalHoras = calcularTotalHoras(gamesPerUser.get(i));
            
            usuariosDtos.add(BibliotecaComparacionDto.UsuarioComparacionDto.builder()
                .steamId(obtenerSteamId(usuario))
                .personaName(usuario.getPlayerInfo() != null ? usuario.getPlayerInfo().getPersonaName() : null)
                .avatarFull(usuario.getPlayerInfo() != null ? usuario.getPlayerInfo().getAvatarFull() : null)
                .profileUrl(usuario.getPlayerInfo() != null ? usuario.getPlayerInfo().getProfileUrl() : null)
                .localCountryCode(usuario.getPlayerInfo() != null ? usuario.getPlayerInfo().getLocalCountryCode() : null)
                .timeCreated(usuario.getPlayerInfo() != null ? usuario.getPlayerInfo().getTimeCreated() : null)
                .totalHorasJugadas(totalHoras)
                .build());
        }
        
        // Calcular estadísticas
        int totalJuegosUnicos = gameOwnership.size();
        double porcentaje = 0.0;
        if (totalJuegosUnicos > 0) {
            porcentaje = ((double) juegosComunes.size() / (double) totalJuegosUnicos) * 100.0;
            porcentaje = Math.round(porcentaje * 100.0) / 100.0;
        }
        
        String categoria = calcularCategoria(porcentaje, usuarios.size());
        
        BibliotecaComparacionDto.EstadisticasDto estadisticas = BibliotecaComparacionDto.EstadisticasDto.builder()
            .juegosComunes(juegosComunes.size())
            .totalUsuarios(usuarios.size())
            .porcentajeSimilitud(porcentaje)
            .categoriaComparacion(categoria)
            .build();
        
        // Construir resultado
        BibliotecaComparacionDto result = BibliotecaComparacionDto.builder()
            .usuarios(usuariosDtos)
            .juegosComunes(juegosComunes)
            .juegosUnicosPorUsuario(juegosUnicosPorUsuario)
            .estadisticas(estadisticas)
            .build();
        
        // Adjuntar tags a los DTOs (mezclando los de enriquecimiento y los de DB)
        try {
            if (tagsMap != null && !tagsMap.isEmpty()) {
                // Agregar tags a juegos comunes
                for (BibliotecaComparacionDto.JuegoComparacionDto jc : result.getJuegosComunes()) {
                    if (jc.getTags() == null) jc.setTags(new java.util.ArrayList<>());
                    java.util.Set<String> merged = new java.util.LinkedHashSet<>();
                    java.util.List<String> t = tagsMap.get(jc.getAppId());
                    if (t != null) merged.addAll(t);
                    java.util.List<String> dbt = dbTagsById.get(jc.getAppId());
                    if (dbt != null) merged.addAll(dbt);
                    for (String s : merged) jc.getTags().add(com.dacs.backend.dto.Tags.builder().tag(s).build());
                }
                
                // Agregar tags a juegos únicos de cada usuario
                for (Map.Entry<String, List<BibliotecaComparacionDto.JuegoComparacionDto>> entry : result.getJuegosUnicosPorUsuario().entrySet()) {
                    for (BibliotecaComparacionDto.JuegoComparacionDto jc : entry.getValue()) {
                        if (jc.getTags() == null) jc.setTags(new java.util.ArrayList<>());
                        java.util.Set<String> merged = new java.util.LinkedHashSet<>();
                        java.util.List<String> t = tagsMap.get(jc.getAppId());
                        if (t != null) merged.addAll(t);
                        java.util.List<String> dbt = dbTagsById.get(jc.getAppId());
                        if (dbt != null) merged.addAll(dbt);
                        for (String s : merged) jc.getTags().add(com.dacs.backend.dto.Tags.builder().tag(s).build());
                    }
                }
            }
            // Caso: tagsMap vacío pero DB tiene tags
            if ((tagsMap == null || tagsMap.isEmpty()) && !dbTagsById.isEmpty()) {
                for (BibliotecaComparacionDto.JuegoComparacionDto jc : result.getJuegosComunes()) {
                    if (jc.getTags() == null) jc.setTags(new java.util.ArrayList<>());
                    java.util.List<String> dbt = dbTagsById.get(jc.getAppId());
                    if (dbt != null) {
                        for (String s : dbt) jc.getTags().add(com.dacs.backend.dto.Tags.builder().tag(s).build());
                    }
                }
                for (Map.Entry<String, List<BibliotecaComparacionDto.JuegoComparacionDto>> entry : result.getJuegosUnicosPorUsuario().entrySet()) {
                    for (BibliotecaComparacionDto.JuegoComparacionDto jc : entry.getValue()) {
                        if (jc.getTags() == null) jc.setTags(new java.util.ArrayList<>());
                        java.util.List<String> dbt = dbTagsById.get(jc.getAppId());
                        if (dbt != null) {
                            for (String s : dbt) jc.getTags().add(com.dacs.backend.dto.Tags.builder().tag(s).build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: fallo al adjuntar tags: " + e.getMessage());
        }

        return result;
    }
    
    /**
     * Filtra solo juegos de tipo "game" o "dlc"
     */
    private List<SteamUserGamesInput.GameInfo> filtrarJuegosValidos(List<SteamUserGamesInput.GameInfo> games) {
        if (games == null) {
            return new ArrayList<>();
        }
        
        // GetOwnedGames solo devuelve aplicaciones que el usuario posee (tipo 'game'),
        // por lo que no es necesario filtrar por tipo. Filtramos solo nulos.
        return games.stream()
            .filter(game -> game != null && game.getAppId() != null)
            .collect(Collectors.toList());
    }
    
    /**
     * Calcula el total de horas jugadas (suma de playtime_forever)
     * Convierte de minutos a horas
     */
    private Integer calcularTotalHoras(List<SteamUserGamesInput.GameInfo> games) {
        if (games == null || games.isEmpty()) {
            return 0;
        }
        
        int totalMinutos = games.stream()
            .mapToInt(game -> game.getPlaytimeForever() != null ? game.getPlaytimeForever() : 0)
            .sum();
        
        // Convertir de minutos a horas (redondeado)
        return (int) Math.round(totalMinutos / 60.0);
    }
    
    /**
     * Obtiene el Steam ID de un usuario
     */
    private String obtenerSteamId(SteamUserGamesInput usuario) {
        if (usuario.getPlayerInfo() != null && usuario.getPlayerInfo().getSteamId() != null) {
            return usuario.getPlayerInfo().getSteamId();
        }
        return usuario.getSteamId();
    }
    
    /**
     * Calcula el precio formateado de un juego
     */
    private String calcularPrecioFormateado(SteamUserGamesInput.GameInfo game) {
        // Priorizar el precio proveniente de la base de datos si está disponible
        if (game.getPrice() != null && !game.getPrice().trim().isEmpty()) {
            return game.getPrice();
        }
        // Si no hay precio, usar flags isFree para formatear
        if (game.getIsFree() == null) {
            return missingPriceFormat;
        }
        return game.getIsFree() ? freePriceFormat : missingPriceFormat;
    }
    
    /**
     * Calcula la categoría de similitud según el porcentaje y cantidad de usuarios
     */
    private String calcularCategoria(double porcentaje, int cantidadUsuarios) {
        if (cantidadUsuarios == 2) {
            // Misma lógica que antes para 2 usuarios
            if (porcentaje < 10.0) {
                return "Muy poco similar";
            } else if (porcentaje < 30.0) {
                return "Poco similar";
            } else if (porcentaje < 60.0) {
                return "Moderadamente similar";
            } else if (porcentaje < 85.0) {
                return "Muy similar";
            } else {
                return "Casi idénticos";
            }
        } else {
            // Para 3+ usuarios, ajustar las categorías
            if (porcentaje < 5.0) {
                return "Gustos muy diversos";
            } else if (porcentaje < 15.0) {
                return "Poca coincidencia";
            } else if (porcentaje < 30.0) {
                return "Algo en común";
            } else if (porcentaje < 50.0) {
                return "Buena coincidencia";
            } else {
                return "Gran coincidencia";
            }
        }
    }
    
    /**
     * Obtiene la clave para identificar al usuario en los Maps (personaName o steamId como fallback)
     */
    private String obtenerClaveUsuario(SteamUserGamesInput usuario) {
        if (usuario.getPlayerInfo() != null && 
            usuario.getPlayerInfo().getPersonaName() != null && 
            !usuario.getPlayerInfo().getPersonaName().trim().isEmpty()) {
            return usuario.getPlayerInfo().getPersonaName();
        }
        return obtenerSteamId(usuario);
    }
}
