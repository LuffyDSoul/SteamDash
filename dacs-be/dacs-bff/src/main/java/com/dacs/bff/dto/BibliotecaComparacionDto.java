package com.dacs.bff.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para el resultado de comparación de bibliotecas entre usuarios
 * Soporta comparación de 2 a 6 usuarios
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BibliotecaComparacionDto {
    
    private List<UsuarioComparacionDto> usuarios; // Lista de todos los usuarios comparados
    private List<JuegoComparacionDto> juegosComunes; // Juegos que tienen al menos 2 usuarios
    private Map<String, List<JuegoComparacionDto>> juegosUnicosPorUsuario; // Map<personaName, juegos únicos>
    private EstadisticasDto estadisticas;
    
    /**
     * DTO con información del usuario para la comparación
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UsuarioComparacionDto {
        private String steamId;
        private String personaName;
        private String avatarFull;
        private String profileUrl;
        private String localCountryCode;
        private Long timeCreated;
        private Integer totalHorasJugadas;
    }
    
    /**
     * DTO con información del juego para la comparación
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JuegoComparacionDto {
        private Long appId;
        private String name;
        private String headerImage;
    @JsonProperty("img-vertical")
    private String imgVertical;
        private String imgIconUrl;
        private Boolean isFree;
        private String price;
        private String storeUrl; // URL a la tienda de Steam
        private java.util.List<com.dacs.bff.dto.Tags> tags; // Tags futuros (SteamSpy)
        
        private Integer cantidadCopias; // Cantidad de usuarios que tienen este juego
        private java.util.Map<String, Integer> tiempoJugadoPorUsuario; // Map<personaName, minutos>
        
        @JsonProperty("appdetails-failed")
        private Boolean appdetailsFailed; // true si la llamada a appdetails devolvió success: false
    }

    /**
     * Estadísticas resumidas de la comparación
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstadisticasDto {
        private Integer juegosComunes; // cantidad de juegos en común (al menos 2 usuarios)
        private Integer totalUsuarios; // cantidad de usuarios comparados
        private Double porcentajeSimilitud; // porcentaje con 2 decimales
        private String categoriaComparacion; // Texto de categorización
    }
}
