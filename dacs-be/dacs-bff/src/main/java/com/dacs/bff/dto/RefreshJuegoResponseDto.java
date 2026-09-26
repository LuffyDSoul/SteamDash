package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la respuesta de refresh de un juego
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshJuegoResponseDto {
    private Integer appId;
    private Boolean success;
    private String message;
    private JuegoUsuarioDto juegoActualizado;
}
