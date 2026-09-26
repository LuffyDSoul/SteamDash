package com.dacs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que almacena el progreso de logros de un usuario en un juego específico
 */
@Entity
@Table(name = "user_game_achievement",
    indexes = {
        @Index(name = "idx_user_game_ach_user", columnList = "steam_id"),
        @Index(name = "idx_user_game_ach_game", columnList = "app_id"),
        @Index(name = "idx_user_game_ach_combo", columnList = "steam_id,app_id")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"steam_id", "app_id", "achievement_name"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserGameAchievement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "steam_id", nullable = false, length = 50)
    private String steamId;
    
    @Column(name = "app_id", nullable = false)
    private Long appId;
    
    @Column(name = "achievement_name", nullable = false, length = 255)
    private String achievementName; // Nombre del logro (debe coincidir con GameAchievement.achievementName)
    
    @Column(name = "achieved", nullable = false)
    private Boolean achieved; // true = desbloqueado, false = bloqueado
    
    @Column(name = "unlock_time")
    private Long unlockTime; // Timestamp Unix cuando se desbloqueó (null si no está desbloqueado)
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
