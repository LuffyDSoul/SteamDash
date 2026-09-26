package com.dacs.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dacs.backend.entity.GameRecord;

import java.util.List;
import java.util.Optional;

public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {
    Optional<GameRecord> findByAppId(Long appId);

    List<GameRecord> findByAppIdIn(List<Long> appIds);
    
    /**
     * Buscar appIds de juegos que contengan al menos una de las tags especificadas
     * Optimizado para filtrar grandes bibliotecas antes de procesamiento en memoria
     */
    @Query("SELECT DISTINCT gr.appId FROM GameRecord gr JOIN gr.tags t WHERE LOWER(t) IN :tags")
    List<Long> findAppIdsByTagsIn(@Param("tags") List<String> tags);
}
