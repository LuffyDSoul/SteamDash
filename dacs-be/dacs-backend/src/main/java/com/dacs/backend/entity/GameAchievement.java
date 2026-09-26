package com.dacs.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad que almacena los logros disponibles de un juego (schema de Steam)
 */
@Entity
@Table(name = "game_achievement",
    indexes = {
        @Index(name = "idx_game_achievement_appid", columnList = "app_id"),
        @Index(name = "idx_game_achievement_name", columnList = "app_id,achievement_name")
    },
    uniqueConstraints = @UniqueConstraint(columnNames = {"app_id", "achievement_name"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAchievement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "app_id", nullable = false)
    private Long appId;
    
    @Column(name = "achievement_name", nullable = false, length = 255)
    private String achievementName; // API name (ej: "NEW_ACHIEVEMENT_1_0")
    
    @Column(name = "display_name", length = 500)
    private String displayName; // Nombre mostrado al usuario
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "icon_url", length = 512)
    private String iconUrl; // URL del icono a color (desbloqueado)
    
    @Column(name = "icon_gray_url", length = 512)
    private String iconGrayUrl; // URL del icono en gris (bloqueado)
    
    @Column(name = "hidden")
    private Integer hidden; // 0 = visible, 1 = oculto
    
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
