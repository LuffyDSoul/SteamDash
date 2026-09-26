package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para la biblioteca de un usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BibliotecaUsuarioDto {
    private String steamId;
    private String personaName;
    private String avatarUrl;
    private Integer totalJuegos;
    private List<JuegoUsuarioDto> juegos;
}
