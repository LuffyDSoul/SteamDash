package com.dacs.bff.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para SteamGameDto desde el conector
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SteamGameDto {
    
    private String type;
    
    private String name;
    
    private Long steamAppId;
    
    private Integer requiredAge;
    
    private Boolean isFree;
    
    private String shortDescription;
    
    private String detailedDescription;
    
    private String aboutTheGame;
    
    private String supportedLanguages;
    
    private String headerImage;
    
    private String capsuleImage;
    
    private String website;
    
    private String[] developers;
    
    private String[] publishers;
    
    private PlatformsDto platforms;
    
    private CategoryDto[] categories;
    
    private GenreDto[] genres;
    
    private ReleaseDateDto releaseDate;
    
    private PriceOverviewDto priceOverview;
    
    private ScreenshotDto[] screenshots;
    
    // Inner classes para estructuras anidadas
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScreenshotDto {
        private Long id;
        private String pathThumbnail;
        private String pathFull;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlatformsDto {
        private Boolean windows;
        private Boolean mac;
        private Boolean linux;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryDto {
        private Integer id;
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreDto {
        private String id;
        private String description;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReleaseDateDto {
        private Boolean comingSoon;
        private String date;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceOverviewDto {
        private String currency;
        private Integer initial;
        private Integer finalPrice;
        private Integer discountPercent;
        private String initialFormatted;
        private String finalFormatted;
    }
}