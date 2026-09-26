package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para tags (para futura integración con SteamSpy)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tags {
    private String tag; // Sólo almacenamos el nombre del tag para la comparativa
}
