package com.dacs.bff.service.impl;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.dto.*;
import com.dacs.bff.service.UserNewsOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNewsOrchestrationServiceImpl implements UserNewsOrchestrationService {
    
    private final ApiConectorClient conectorClient;
    private static final int NEWS_PER_GAME = 5;
    
    // Clase auxiliar para procesar juegos
    private static class GameToProcess {
        Long appId;
        String name;
        
        GameToProcess(Long appId, String name) {
            this.appId = appId;
            this.name = name;
        }
    }
    
    /**
     * Obtener noticias de TODOS los juegos del usuario (owned games)
     */
    @Override
    public UserNewsResponseDto getAllUserGamesNews(String steamId, int page, int pageSize) {
        log.info("Getting ALL owned games news for user: {}", steamId);
        
        try {
            // Obtener TODOS los juegos del usuario (sin filtro de recientes)
            log.info("Fetching all owned games...");
            SteamOwnedGamesResponseDto ownedGames = conectorClient.getUserOwnedGames(steamId, true, false);
            
            if (ownedGames == null || ownedGames.getResponse() == null || 
                ownedGames.getResponse().getGames() == null || 
                ownedGames.getResponse().getGames().isEmpty()) {
                log.warn("No games found for user");
                return buildEmptyResponse(steamId, page, pageSize);
            }
            
            List<SteamOwnedGamesResponseDto.OwnedGameDto> allGames = ownedGames.getResponse().getGames();
            log.info("Found {} owned games, limiting to {}", allGames.size(), pageSize);
            
            List<GameToProcess> gamesToProcess = allGames.stream()
                .limit(pageSize)
                .map(g -> new GameToProcess(g.getAppId(), g.getName()))
                .collect(Collectors.toList());
            
            if (gamesToProcess.isEmpty()) {
                return buildEmptyResponse(steamId, page, pageSize);
            }
            
            return processGamesNews(steamId, gamesToProcess, page, pageSize);
            
        } catch (Exception e) {
            log.error("Error getting all games news: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching all games news: " + e.getMessage(), e);
        }
    }
    
    /**
     * Obtener noticias de juegos JUGADOS RECIENTEMENTE
     */
    @Override
    public UserNewsResponseDto getRecentlyPlayedGamesNews(String steamId, int page, int pageSize) {
        log.info("Getting recently played games news for user: {}", steamId);
        
        try {
            // Obtener solo juegos recientes (últimas 2 semanas)
            log.info("Fetching recently played games...");
            SteamRecentlyPlayedGamesResponseDto recentlyPlayed = conectorClient.getRecentlyPlayedGames(steamId);
            
            if (recentlyPlayed == null || recentlyPlayed.getResponse() == null || 
                recentlyPlayed.getResponse().getGames() == null || 
                recentlyPlayed.getResponse().getGames().isEmpty()) {
                log.info("No recently played games found");
                return buildEmptyResponse(steamId, page, pageSize);
            }
            
            log.info("Found {} recently played games", recentlyPlayed.getResponse().getGames().size());
            List<GameToProcess> gamesToProcess = recentlyPlayed.getResponse().getGames().stream()
                .map(g -> new GameToProcess(g.getAppId(), g.getName()))
                .collect(Collectors.toList());
            
            return processGamesNews(steamId, gamesToProcess, page, pageSize);
            
        } catch (Exception e) {
            log.error("Error getting recently played games news: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching recently played games news: " + e.getMessage(), e);
        }
    }
    
    /**
     * Método auxiliar para procesar noticias de una lista de juegos
     */
    private UserNewsResponseDto processGamesNews(String steamId, List<GameToProcess> gamesToProcess, int page, int pageSize) {
        log.info("Processing {} games", gamesToProcess.size());
        
        // Obtener noticias
        List<UserNewsResponseDto.GameNewsDto> gamesWithNews = new ArrayList<>();
        
        for (GameToProcess game : gamesToProcess) {
            try {
                String appId = String.valueOf(game.appId);
                SteamNewsResponseDto newsResponse = conectorClient.getNewsForApp(appId, NEWS_PER_GAME, 300);
                
                if (newsResponse != null && newsResponse.getAppNews() != null && 
                    newsResponse.getAppNews().getNewsItems() != null && 
                    !newsResponse.getAppNews().getNewsItems().isEmpty()) {
                    
                    List<UserNewsResponseDto.GameNewsDto.NewsItemDto> newsItems = 
                        newsResponse.getAppNews().getNewsItems().stream()
                            .map(this::mapNewsItem)
                            .collect(Collectors.toList());
                    
                    UserNewsResponseDto.GameNewsDto gameNews = UserNewsResponseDto.GameNewsDto.builder()
                        .appId(game.appId)
                        .gameName(game.name)
                        .gameImageUrl(getGameImageUrl(game.appId))
                        .latestNews(newsItems.subList(0, Math.min(2, newsItems.size())))
                        .olderNews(newsItems.size() > 2 ? newsItems.subList(2, newsItems.size()) : new ArrayList<>())
                        .totalNewsCount(newsItems.size())
                        .lastNewsDate(newsItems.get(0).getDate())
                        .build();
                    
                    gamesWithNews.add(gameNews);
                }
                
                Thread.sleep(100); // Rate limiting
                
            } catch (Exception e) {
                log.warn("Error fetching news for {}: {}", game.name, e.getMessage());
            }
        }
        
        log.info("Got news for {}/{} games", gamesWithNews.size(), gamesToProcess.size());
        
        return UserNewsResponseDto.builder()
            .steamId(steamId)
            .gamesNews(gamesWithNews)
            .totalGames(gamesWithNews.size())
            .page(0)
            .pageSize(pageSize)
            .hasMore(false)
            .build();
    }
    
    /**
     * @deprecated Use getAllUserGamesNews or getRecentlyPlayedGamesNews instead
     */
    @Deprecated
    public UserNewsResponseDto getUserGamesNews(String steamId, int page, int pageSize) {
        log.info("Getting news for user: {}", steamId);
        
        try {
            // 1. Primero intentar juegos recientes (últimas 2 semanas)
            log.info("Fetching recently played games...");
            SteamRecentlyPlayedGamesResponseDto recentlyPlayed = conectorClient.getRecentlyPlayedGames(steamId);
            
            List<GameToProcess> gamesToProcess = new ArrayList<>();
            
            // Si hay juegos recientes, usarlos
            if (recentlyPlayed != null && recentlyPlayed.getResponse() != null && 
                recentlyPlayed.getResponse().getGames() != null && 
                !recentlyPlayed.getResponse().getGames().isEmpty()) {
                
                log.info("Found {} recently played games", recentlyPlayed.getResponse().getGames().size());
                gamesToProcess = recentlyPlayed.getResponse().getGames().stream()
                    .map(g -> new GameToProcess(g.getAppId(), g.getName()))
                    .collect(Collectors.toList());
                    
            } else {
                // Fallback: obtener todos los juegos (limitados)
                log.info("No recently played games, fetching owned games...");
                SteamOwnedGamesResponseDto ownedGames = conectorClient.getUserOwnedGames(steamId, true, false);
                
                if (ownedGames == null || ownedGames.getResponse() == null || 
                    ownedGames.getResponse().getGames() == null || 
                    ownedGames.getResponse().getGames().isEmpty()) {
                    log.warn("No games found");
                    return buildEmptyResponse(steamId, page, pageSize);
                }
                
                List<SteamOwnedGamesResponseDto.OwnedGameDto> allGames = ownedGames.getResponse().getGames();
                log.info("Found {} owned games, limiting to {}", allGames.size(), pageSize);
                
                gamesToProcess = allGames.stream()
                    .limit(pageSize)
                    .map(g -> new GameToProcess(g.getAppId(), g.getName()))
                    .collect(Collectors.toList());
            }
            
            if (gamesToProcess.isEmpty()) {
                return buildEmptyResponse(steamId, page, pageSize);
            }
            
            log.info("Processing {} games", gamesToProcess.size());
            
            // 2. Obtener noticias
            List<UserNewsResponseDto.GameNewsDto> gamesWithNews = new ArrayList<>();
            
            for (GameToProcess game : gamesToProcess) {
                try {
                    String appId = String.valueOf(game.appId);
                    SteamNewsResponseDto newsResponse = conectorClient.getNewsForApp(appId, NEWS_PER_GAME, 300);
                    
                    if (newsResponse != null && newsResponse.getAppNews() != null && 
                        newsResponse.getAppNews().getNewsItems() != null && 
                        !newsResponse.getAppNews().getNewsItems().isEmpty()) {
                        
                        List<UserNewsResponseDto.GameNewsDto.NewsItemDto> newsItems = 
                            newsResponse.getAppNews().getNewsItems().stream()
                                .map(this::mapNewsItem)
                                .collect(Collectors.toList());
                        
                        UserNewsResponseDto.GameNewsDto gameNews = UserNewsResponseDto.GameNewsDto.builder()
                            .appId(game.appId)
                            .gameName(game.name)
                            .gameImageUrl(getGameImageUrl(game.appId))
                            .latestNews(newsItems.subList(0, Math.min(2, newsItems.size())))
                            .olderNews(newsItems.size() > 2 ? newsItems.subList(2, newsItems.size()) : new ArrayList<>())
                            .totalNewsCount(newsItems.size())
                            .lastNewsDate(newsItems.get(0).getDate())
                            .build();
                        
                        gamesWithNews.add(gameNews);
                    }
                    
                    Thread.sleep(100); // Rate limiting
                    
                } catch (Exception e) {
                    log.warn("Error fetching news for {}: {}", game.name, e.getMessage());
                }
            }
            
            log.info("Got news for {}/{} games", gamesWithNews.size(), gamesToProcess.size());
            
            return UserNewsResponseDto.builder()
                .steamId(steamId)
                .gamesNews(gamesWithNews)
                .totalGames(gamesWithNews.size())
                .page(0)
                .pageSize(pageSize)
                .hasMore(false)
                .build();
            
        } catch (Exception e) {
            log.error("Error getting news: {}", e.getMessage(), e);
            throw new RuntimeException("Error fetching news: " + e.getMessage(), e);
        }
    }
    
    private UserNewsResponseDto buildEmptyResponse(String steamId, int page, int pageSize) {
        return UserNewsResponseDto.builder()
            .steamId(steamId)
            .gamesNews(new ArrayList<>())
            .totalGames(0)
            .page(page)
            .pageSize(pageSize)
            .hasMore(false)
            .build();
    }
    
    @Override
    public GameDetailsDto searchGameDetails(String appId) {
        log.info("Searching game: {}", appId);
        
        try {
            SteamGameDto gameDto = conectorClient.getSteamGameDetails(appId);
            
            if (gameDto == null) {
                return null;
            }
            
            GameDetailsDto.GameDetailsDtoBuilder builder = GameDetailsDto.builder()
                .appId(gameDto.getSteamAppId())
                .name(gameDto.getName())
                .type(gameDto.getType())
                .isFree(gameDto.getIsFree())
                .headerImage(gameDto.getHeaderImage())
                .shortDescription(gameDto.getShortDescription())
                .detailedDescription(gameDto.getDetailedDescription())
                .website(gameDto.getWebsite());
            
            if (gameDto.getDevelopers() != null) {
                builder.developers(java.util.Arrays.asList(gameDto.getDevelopers()));
            }
            
            if (gameDto.getPublishers() != null) {
                builder.publishers(java.util.Arrays.asList(gameDto.getPublishers()));
            }
            
            if (gameDto.getPriceOverview() != null) {
                SteamGameDto.PriceOverviewDto po = gameDto.getPriceOverview();
                builder.priceOverview(GameDetailsDto.PriceOverview.builder()
                    .currency(po.getCurrency())
                    .initial(po.getInitial())
                    .finalPrice(po.getFinalPrice())
                    .discountPercent(po.getDiscountPercent())
                    .finalFormatted(po.getFinalFormatted())
                    .build());
            }
            
            if (gameDto.getCategories() != null) {
                List<String> categories = java.util.Arrays.stream(gameDto.getCategories())
                    .map(SteamGameDto.CategoryDto::getDescription)
                    .collect(Collectors.toList());
                builder.categories(categories);
            }
            
            if (gameDto.getGenres() != null) {
                List<String> genres = java.util.Arrays.stream(gameDto.getGenres())
                    .map(SteamGameDto.GenreDto::getDescription)
                    .collect(Collectors.toList());
                builder.genres(genres);
            }
            
            return builder.build();
            
        } catch (Exception e) {
            log.error("Error searching game: {}", e.getMessage(), e);
            throw new RuntimeException("Error searching game", e);
        }
    }
    
    private UserNewsResponseDto.GameNewsDto.NewsItemDto mapNewsItem(SteamNewsResponseDto.NewsItemDto newsItem) {
        return UserNewsResponseDto.GameNewsDto.NewsItemDto.builder()
            .gid(newsItem.getGid())
            .title(newsItem.getTitle())
            .url(newsItem.getUrl())
            .isExternalUrl(newsItem.getIsExternalUrl())
            .author(newsItem.getAuthor())
            .contents(newsItem.getContents())
            .feedLabel(newsItem.getFeedLabel())
            .date(newsItem.getDate())
            .feedName(newsItem.getFeedName())
            .feedType(newsItem.getFeedType())
            .appId(newsItem.getAppId())
            .isNew(false)
            .build();
    }
    
    private String getGameImageUrl(Long appId) {
        return String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/library_600x900.jpg", appId);
    }
}
