package com.dacs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad para cachear IDs de noticias ya vistas por usuario
 */
@Entity
@Table(name = "user_news_cache", 
       indexes = {
           @Index(name = "idx_user_app", columnList = "steam_id,app_id"),
           @Index(name = "idx_news_gid", columnList = "news_gid")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNewsCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "steam_id", nullable = false, length = 50)
    private String steamId;
    
    @Column(name = "app_id", nullable = false)
    private Long appId;
    
    @Column(name = "news_gid", nullable = false, unique = true, length = 100)
    private String newsGid; // ID único de la noticia (gid)
    
    @Column(name = "news_date", nullable = false)
    private Long newsDate; // Timestamp de la noticia
    
    @Column(name = "first_seen_at", nullable = false)
    private LocalDateTime firstSeenAt; // Cuándo se vio por primera vez
    
    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt; // Última vez que se verificó
}
