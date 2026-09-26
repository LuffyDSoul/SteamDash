package com.dacs.backend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar la información enriquecida de un único juego
 * Mantiene los mismos atributos relevantes que la comparación de bibliotecas
 * para que la UI pueda reutilizar componentes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BibliotecaJuegoDto {
    private Long appId;
    private String name;
    private String headerImage;
    private String imgVertical; // https://cdn.akamai.steamstatic.com/steam/apps/{appId}/library_600x900.jpg
    private String imgIconUrl;
    private Boolean isFree;
    private String price;       // precio formateado según reglas
    private String storeUrl;    // https://store.steampowered.com/app/{appId}
    private List<Tags> tags;    // tags/genres
}
