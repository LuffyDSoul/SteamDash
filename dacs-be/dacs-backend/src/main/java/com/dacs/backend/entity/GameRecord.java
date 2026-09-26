package com.dacs.backend.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

import lombok.*;

@Entity
@Table(name = "game_record")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long appId;
    private String name;
    private String headerImage;
    private String imgVertical;
    private String imgIconUrl;
    private Boolean isFree;
    private String price;
    private Boolean appdetailsFailed; // true si appdetails devolvió success: false
    private Instant createdAt;
    private Instant updatedAt;

    @ElementCollection
    @CollectionTable(name = "game_record_tags", joinColumns = @JoinColumn(name = "game_record_id"))
    @Column(name = "tag")
    private List<String> tags;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
