package com.dacs.backend.repository;

import com.dacs.backend.entity.UserGameAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserGameAchievementRepository extends JpaRepository<UserGameAchievement, Long> {
    
    /**
     * Encuentra todos los logros de un usuario para un juego específico
     */
    List<UserGameAchievement> findBySteamIdAndAppId(String steamId, Long appId);
    
    /**
     * Verifica si existen logros almacenados para un usuario en un juego
     */
    boolean existsBySteamIdAndAppId(String steamId, Long appId);
    
    /**
     * Cuenta cuántos juegos distintos con logros tiene un usuario
     */
    @Query("SELECT COUNT(DISTINCT uga.appId) FROM UserGameAchievement uga WHERE uga.steamId = :steamId")
    long countDistinctAppIdBySteamId(@Param("steamId") String steamId);
    
    /**
     * Encuentra todos los logros de un usuario
     */
    List<UserGameAchievement> findBySteamId(String steamId);
    
    /**
     * Encuentra un logro específico de un usuario
     */
    Optional<UserGameAchievement> findBySteamIdAndAppIdAndAchievementName(
        String steamId, Long appId, String achievementName);
    
    /**
     * Cuenta cuántos logros ha desbloqueado un usuario en un juego
     */
    @Query("SELECT COUNT(uga) FROM UserGameAchievement uga " +
           "WHERE uga.steamId = :steamId AND uga.appId = :appId AND uga.achieved = true")
    Long countUnlockedAchievements(@Param("steamId") String steamId, @Param("appId") Long appId);
    
    /**
     * Cuenta cuántos logros tiene un usuario para un juego específico (total, incluyendo bloqueados y desbloqueados)
     */
    @Query("SELECT COUNT(uga) FROM UserGameAchievement uga WHERE uga.steamId = :steamId AND uga.appId = :appId")
    long countBySteamIdAndAppId(@Param("steamId") String steamId, @Param("appId") Long appId);
    
    /**
     * Obtiene estadísticas de logros para todos los juegos de un usuario en una sola consulta
     * Retorna una lista de Object[] con: [appId, totalAchievements, unlockedAchievements]
     */
    @Query("SELECT uga.appId, COUNT(uga), COALESCE(SUM(CASE WHEN uga.achieved = true THEN 1 ELSE 0 END), 0) " +
           "FROM UserGameAchievement uga " +
           "WHERE uga.steamId = :steamId " +
           "GROUP BY uga.appId")
    List<Object[]> getAchievementStatsByUser(@Param("steamId") String steamId);
    
    /**
     * Elimina todos los logros de un usuario para un juego (útil para refrescar datos)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserGameAchievement uga WHERE uga.steamId = :steamId AND uga.appId = :appId")
    void deleteBySteamIdAndAppId(@Param("steamId") String steamId, @Param("appId") Long appId);
}
