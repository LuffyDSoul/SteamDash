package com.dacs.bff.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un juego aleatorio con información completa
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JuegoAleatorioDto {
    
    private Long appId;
    private String name;
    private String description;
    private String shortDescription;
    private String headerImage;
    private String libraryImage; // imagen vertical (library_600x900.jpg)
    private String backgroundImage;
    private String price;
    private Boolean isFree;
    private String storeUrl;
    private List<String> tags;
    private List<String> screenshots;
    private List<String> developers;
    private List<String> publishers;
    private String releaseDate;
    private List<String> genres;
    private List<String> categories;
    private Integer playtimeForever; // minutos jugados
}
