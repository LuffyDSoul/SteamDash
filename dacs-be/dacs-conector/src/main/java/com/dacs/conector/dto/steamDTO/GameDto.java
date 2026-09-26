package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class GameDto {
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("steam_appid")
    private Long steamAppId;
    
    @JsonProperty("required_age")
    private Integer requiredAge;
    
    @JsonProperty("is_free")
    private Boolean isFree;
    
    @JsonProperty("short_description")
    private String shortDescription;
    
    @JsonProperty("detailed_description")
    private String detailedDescription;
    
    @JsonProperty("about_the_game")
    private String aboutTheGame;
    
    @JsonProperty("supported_languages")
    private String supportedLanguages;
    
    @JsonProperty("header_image")
    private String headerImage;
    
    @JsonProperty("capsule_image")
    private String capsuleImage;
    
    @JsonProperty("website")
    private String website;
    
    @JsonProperty("developers")
    private String[] developers;
    
    @JsonProperty("publishers")
    private String[] publishers;
    
    @JsonProperty("platforms")
    private PlatformsDto platforms;
    
    @JsonProperty("categories")
    private CategoryDto[] categories;
    
    @JsonProperty("genres")
    private GenreDto[] genres;
    
    @JsonProperty("release_date")
    private ReleaseDateDto releaseDate;
    
    @JsonProperty("price_overview")
    private PriceOverviewDto priceOverview;
}