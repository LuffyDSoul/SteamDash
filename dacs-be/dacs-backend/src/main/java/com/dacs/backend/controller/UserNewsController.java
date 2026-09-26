package com.dacs.backend.controller;

import com.dacs.backend.dto.UserNewsResponseDto;
import com.dacs.backend.service.UserNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para manejo de noticias de usuario
 */
@RestController
@RequestMapping("/api/user-news")
@RequiredArgsConstructor
@Slf4j
public class UserNewsController {
    
    private final UserNewsService userNewsService;
    
    /**
     * Procesar noticias de juegos de un usuario
     * POST /api/user-news/process
     */
    @PostMapping("/process")
    public ResponseEntity<UserNewsResponseDto> processUserNews(@RequestBody UserNewsRequestDto request) {
        log.info("Processing news for user: {}, page: {}, pageSize: {}", 
                 request.getSteamId(), request.getPage(), request.getPageSize());
        
        try {
            // Convertir DTO de entrada a formato del servicio
            List<UserNewsService.GameNewsInput> gameInputs = request.getGamesWithNews().stream()
                .map(this::mapToServiceInput)
                .collect(Collectors.toList());
            
            UserNewsResponseDto response = userNewsService.processUserNews(
                request.getSteamId(),
                gameInputs,
                request.getPage() != null ? request.getPage() : 0,
                request.getPageSize() != null ? request.getPageSize() : 25
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing user news", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Marcar noticias como vistas
     * POST /api/user-news/mark-seen
     */
    @PostMapping("/mark-seen")
    public ResponseEntity<Void> markNewsAsSeen(@RequestBody MarkNewsAsSeenRequest request) {
        log.info("Marking {} news as seen for user: {}", request.getNews().size(), request.getSteamId());
        
        try {
            List<UserNewsService.NewsToMark> newsToMark = request.getNews().stream()
                .map(n -> {
                    UserNewsService.NewsToMark ntm = new UserNewsService.NewsToMark();
                    ntm.gid = n.getGid();
                    ntm.appId = n.getAppId();
                    ntm.date = n.getDate();
                    return ntm;
                })
                .collect(Collectors.toList());
            
            userNewsService.markNewsAsSeen(request.getSteamId(), newsToMark);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking news as seen", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private UserNewsService.GameNewsInput mapToServiceInput(UserNewsRequestDto.GameNewsInput dto) {
        UserNewsService.GameNewsInput input = new UserNewsService.GameNewsInput();
        input.appId = dto.getAppId();
        input.gameName = dto.getGameName();
        input.news = dto.getNews().stream()
            .map(this::mapNewsItemToServiceInput)
            .collect(Collectors.toList());
        return input;
    }
    
    private UserNewsService.GameNewsInput.NewsItemInput mapNewsItemToServiceInput(
            UserNewsRequestDto.GameNewsInput.NewsItemInput dto) {
        UserNewsService.GameNewsInput.NewsItemInput input = new UserNewsService.GameNewsInput.NewsItemInput();
        input.gid = dto.getGid();
        input.title = dto.getTitle();
        input.url = dto.getUrl();
        input.isExternalUrl = dto.getIsExternalUrl();
        input.author = dto.getAuthor();
        input.contents = dto.getContents();
        input.feedLabel = dto.getFeedLabel();
        input.date = dto.getDate();
        input.feedName = dto.getFeedName();
        input.feedType = dto.getFeedType();
        input.appId = dto.getAppId();
        return input;
    }
    
    // DTOs internos
    @lombok.Getter
    @lombok.Setter
    public static class UserNewsRequestDto {
        private String steamId;
        private List<GameNewsInput> gamesWithNews;
        private Integer page;
        private Integer pageSize;
        
        @lombok.Getter
        @lombok.Setter
        public static class GameNewsInput {
            private Long appId;
            private String gameName;
            private List<NewsItemInput> news;
            
            @lombok.Getter
            @lombok.Setter
            public static class NewsItemInput {
                private String gid;
                private String title;
                private String url;
                private Boolean isExternalUrl;
                private String author;
                private String contents;
                private String feedLabel;
                private Long date;
                private String feedName;
                private Integer feedType;
                private Long appId;
            }
        }
    }
    
    @lombok.Getter
    @lombok.Setter
    public static class MarkNewsAsSeenRequest {
        private String steamId;
        private List<NewsToMarkDto> news;
        
        @lombok.Getter
        @lombok.Setter
        public static class NewsToMarkDto {
            private String gid;
            private Long appId;
            private Long date;
        }
    }
}
