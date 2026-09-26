package com.dacs.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para tags obtenidos (o por obtener) desde SteamSpy u otras fuentes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tags {
    private String tag;       // Nombre del tag (ej. "Multiplayer", "RPG")
    // Sólo almacenamos el nombre del tag para la comparativa
}
