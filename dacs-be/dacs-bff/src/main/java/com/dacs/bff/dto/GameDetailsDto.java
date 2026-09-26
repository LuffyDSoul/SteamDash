package com.dacs.bff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO para detalles de un juego desde Steam Store API
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameDetailsDto {
    private Long appId;
    private String name;
    private String type;
    private Boolean isFree;
    private String headerImage;
    private String shortDescription;
    private String detailedDescription;
    private String website;
    private List<String> developers;
    private List<String> publishers;
    private PriceOverview priceOverview;
    private List<String> categories;
    private List<String> genres;
    private List<Screenshot> screenshots;
    private Map<String, Object> rawData; // Datos completos por si se necesita algo más
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceOverview {
        private String currency;
        private Integer initial;
        private Integer finalPrice;
        private Integer discountPercent;
        private String finalFormatted;
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Screenshot {
        private Integer id;
        private String pathThumbnail;
        private String pathFull;
    }
}
