package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la respuesta de sincronización de biblioteca
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncBibliotecaResponseDto {
    private Integer total;
    private Integer sincronizados;
    private Integer errores;
    private String mensaje;
    private List<String> juegosConError;  // Lista de juegos que fallaron
}
