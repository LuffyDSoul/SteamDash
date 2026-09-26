package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de actualización completa de biblioteca
 * Usado cuando se actualizan TODOS los juegos (refresh-all)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshAllGamesResponseDto {
    private Integer total;          // Total de juegos en la biblioteca
    private Integer actualizados;   // Juegos actualizados exitosamente
    private Integer errores;        // Juegos que fallaron al actualizar
    private String mensaje;         // Mensaje descriptivo del resultado
    private List<String> juegosConError;  // Lista de juegos que fallaron con detalles
}
