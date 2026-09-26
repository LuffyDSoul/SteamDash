package com.dacs.backend.repository;

import com.dacs.backend.entity.UserNewsCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserNewsCacheRepository extends JpaRepository<UserNewsCache, Long> {
    
    /**
     * Buscar un registro de caché específico
     */
    Optional<UserNewsCache> findBySteamIdAndAppIdAndNewsGid(String steamId, Long appId, String newsGid);
    
    /**
     * Verificar si una noticia ya fue vista
     */
    boolean existsBySteamIdAndNewsGid(String steamId, String newsGid);
    
    /**
     * Buscar por steamId y newsGid
     */
    Optional<UserNewsCache> findBySteamIdAndNewsGid(String steamId, String newsGid);
    
    /**
     * Obtener todas las noticias vistas de un usuario para un juego
     */
    List<UserNewsCache> findBySteamIdAndAppIdOrderByNewsDateDesc(String steamId, Long appId);
    
    /**
     * Obtener la fecha de la última noticia vista para un juego
     */
    @Query("SELECT MAX(u.newsDate) FROM UserNewsCache u WHERE u.steamId = :steamId AND u.appId = :appId")
    Optional<Long> findLatestNewsDateBySteamIdAndAppId(@Param("steamId") String steamId, @Param("appId") Long appId);
    
    /**
     * Limpiar registros antiguos (más de X días)
     */
    void deleteByLastCheckedAtBefore(LocalDateTime cutoffDate);
    
    /**
     * Obtener todos los gids de noticias vistas por un usuario
     */
    @Query("SELECT u.newsGid FROM UserNewsCache u WHERE u.steamId = :steamId")
    List<String> findAllNewsGidsBySteamId(@Param("steamId") String steamId);
}
