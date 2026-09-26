package com.dacs.backend.repository;

import com.dacs.backend.entity.GameAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameAchievementRepository extends JpaRepository<GameAchievement, Long> {
    
    /**
     * Encuentra todos los logros de un juego específico
     */
    List<GameAchievement> findByAppId(Long appId);
    
    /**
     * Verifica si existen logros almacenados para un juego
     */
    boolean existsByAppId(Long appId);
    
    /**
     * Encuentra un logro específico por appId y nombre del logro
     */
    Optional<GameAchievement> findByAppIdAndAchievementName(Long appId, String achievementName);
    
    /**
     * Cuenta cuántos logros tiene un juego
     */
    @Query("SELECT COUNT(ga) FROM GameAchievement ga WHERE ga.appId = :appId")
    Long countByAppId(@Param("appId") Long appId);
    
    /**
     * Elimina todos los logros de un juego (útil para actualizar el schema)
     */
    void deleteByAppId(Long appId);
}
