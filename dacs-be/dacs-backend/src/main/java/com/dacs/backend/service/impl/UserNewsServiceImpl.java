package com.dacs.backend.service.impl;

import com.dacs.backend.dto.UserNewsResponseDto;
import com.dacs.backend.entity.UserNewsCache;
import com.dacs.backend.repository.UserNewsCacheRepository;
import com.dacs.backend.service.UserNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserNewsServiceImpl implements UserNewsService {
    
    private final UserNewsCacheRepository newsCacheRepository;
    
    @Override
    @Transactional
    public UserNewsResponseDto processUserNews(String steamId, List<GameNewsInput> gamesWithNews, int page, int pageSize) {
        log.info("Processing news for user {} - page {}, pageSize {}", steamId, page, pageSize);
        
        // Obtener todos los gids de noticias ya vistas por el usuario
        Set<String> seenNewsGids = new HashSet<>(newsCacheRepository.findAllNewsGidsBySteamId(steamId));
        log.debug("User {} has {} seen news", steamId, seenNewsGids.size());
        
        List<UserNewsResponseDto.GameNewsDto> processedGames = new ArrayList<>();
        
        for (GameNewsInput gameInput : gamesWithNews) {
            if (gameInput.news == null || gameInput.news.isEmpty()) {
                continue;
            }
            
            // Ordenar noticias por fecha descendente
            List<GameNewsInput.NewsItemInput> sortedNews = gameInput.news.stream()
                .sorted(Comparator.comparing(n -> n.date, Comparator.reverseOrder()))
                .collect(Collectors.toList());
            
            // Convertir a DTOs y marcar como nuevas si no están en caché
            List<UserNewsResponseDto.GameNewsDto.NewsItemDto> allNewsItems = sortedNews.stream()
                .map(newsItem -> {
                    boolean isNew = !seenNewsGids.contains(newsItem.gid);
                    
                    // Guardar en caché si es nueva
                    if (isNew) {
                        saveNewsToCache(steamId, gameInput.appId, newsItem);
                    } else {
                        updateLastChecked(steamId, newsItem.gid);
                    }
                    
                    return UserNewsResponseDto.GameNewsDto.NewsItemDto.builder()
                        .gid(newsItem.gid)
                        .title(newsItem.title)
                        .url(newsItem.url)
                        .isExternalUrl(newsItem.isExternalUrl)
                        .author(newsItem.author)
                        .contents(newsItem.contents)
                        .feedLabel(newsItem.feedLabel)
                        .date(newsItem.date)
                        .feedName(newsItem.feedName)
                        .feedType(newsItem.feedType)
                        .appId(newsItem.appId)
                        .isNew(isNew)
                        .build();
                })
                .collect(Collectors.toList());
            
            // Separar últimas 2 noticias del resto
            List<UserNewsResponseDto.GameNewsDto.NewsItemDto> latestNews = 
                allNewsItems.stream().limit(2).collect(Collectors.toList());
            
            List<UserNewsResponseDto.GameNewsDto.NewsItemDto> olderNews = 
                allNewsItems.stream().skip(2).collect(Collectors.toList());
            
            Long lastNewsDate = !allNewsItems.isEmpty() ? allNewsItems.get(0).getDate() : null;
            
            // Construir URL de imagen del juego
            String gameImageUrl = String.format("https://cdn.akamai.steamstatic.com/steam/apps/%d/library_600x900.jpg", 
                                                gameInput.appId);
            
            UserNewsResponseDto.GameNewsDto gameNewsDto = UserNewsResponseDto.GameNewsDto.builder()
                .appId(gameInput.appId)
                .gameName(gameInput.gameName)
                .gameImageUrl(gameImageUrl)
                .latestNews(latestNews)
                .olderNews(olderNews)
                .totalNewsCount(allNewsItems.size())
                .lastNewsDate(lastNewsDate)
                .build();
            
            processedGames.add(gameNewsDto);
        }
        
        // Ordenar juegos por fecha de última noticia (más recientes primero)
        processedGames.sort((g1, g2) -> {
            if (g1.getLastNewsDate() == null && g2.getLastNewsDate() == null) return 0;
            if (g1.getLastNewsDate() == null) return 1;
            if (g2.getLastNewsDate() == null) return -1;
            return g2.getLastNewsDate().compareTo(g1.getLastNewsDate());
        });
        
        // Aplicar paginación
        int start = page * pageSize;
        int end = Math.min(start + pageSize, processedGames.size());
        
        List<UserNewsResponseDto.GameNewsDto> paginatedGames = 
            start < processedGames.size() 
                ? processedGames.subList(start, end) 
                : new ArrayList<>();
        
        boolean hasMore = end < processedGames.size();
        
        return UserNewsResponseDto.builder()
            .steamId(steamId)
            .gamesNews(paginatedGames)
            .totalGames(processedGames.size())
            .page(page)
            .pageSize(pageSize)
            .hasMore(hasMore)
            .build();
    }
    
    @Override
    @Transactional
    public void markNewsAsSeen(String steamId, List<NewsToMark> newsToMark) {
        log.info("Marking {} news as seen for user {}", newsToMark.size(), steamId);
        
        for (NewsToMark news : newsToMark) {
            Optional<UserNewsCache> existing = newsCacheRepository
                .findBySteamIdAndAppIdAndNewsGid(steamId, news.appId, news.gid);
            
            if (existing.isEmpty()) {
                UserNewsCache cache = UserNewsCache.builder()
                    .steamId(steamId)
                    .appId(news.appId)
                    .newsGid(news.gid)
                    .newsDate(news.date)
                    .firstSeenAt(LocalDateTime.now())
                    .lastCheckedAt(LocalDateTime.now())
                    .build();
                
                newsCacheRepository.save(cache);
            }
        }
    }
    
    @Override
    @Transactional
    public void cleanOldCache() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        log.info("Cleaning news cache older than {}", cutoffDate);
        newsCacheRepository.deleteByLastCheckedAtBefore(cutoffDate);
    }
    
    private void saveNewsToCache(String steamId, Long appId, GameNewsInput.NewsItemInput newsItem) {
        try {
            UserNewsCache cache = UserNewsCache.builder()
                .steamId(steamId)
                .appId(appId)
                .newsGid(newsItem.gid)
                .newsDate(newsItem.date)
                .firstSeenAt(LocalDateTime.now())
                .lastCheckedAt(LocalDateTime.now())
                .build();
            
            newsCacheRepository.save(cache);
        } catch (Exception e) {
            // Ignorar errores de duplicados
            log.debug("News already in cache: {}", newsItem.gid);
        }
    }
    
    private void updateLastChecked(String steamId, String newsGid) {
        try {
            newsCacheRepository.findBySteamIdAndNewsGid(steamId, newsGid)
                .ifPresent(cache -> {
                    cache.setLastCheckedAt(LocalDateTime.now());
                    newsCacheRepository.save(cache);
                });
        } catch (Exception e) {
            log.debug("Error updating last checked for news {}: {}", newsGid, e.getMessage());
        }
    }
}
