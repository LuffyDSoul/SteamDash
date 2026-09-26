package com.dacs.backend.service;

import com.dacs.backend.dto.BibliotecaJuegoDto;

import java.util.List;

/**
 * Servicio de negocio para obtener/enriquecer un único juego de la biblioteca
 */
public interface BibliotecaJuegoService {
    /**
     * Obtiene un juego enriquecido y persistido. Si no existe en DB, lo
     * enriquece con datos del conector, lo guarda y devuelve el resultado.
     */
    BibliotecaJuegoDto obtenerJuego(Long appId);
    
    /**
     * Busca appIds de juegos que contengan al menos una de las tags especificadas.
     * Optimizado para filtrado a nivel de base de datos.
     * 
     * @param tags Lista de tags a buscar (case-insensitive)
     * @return Lista de appIds que contienen al menos una de las tags
     */
    List<Long> buscarAppIdsPorTags(List<String> tags);
}
