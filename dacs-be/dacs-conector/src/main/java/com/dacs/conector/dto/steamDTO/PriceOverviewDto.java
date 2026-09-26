package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class PriceOverviewDto {
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("initial")
    private Integer initial;
    
    @JsonProperty("final")
    private Integer finalPrice;
    
    @JsonProperty("discount_percent")
    private Integer discountPercent;
    
    @JsonProperty("initial_formatted")
    private String initialFormatted;
    
    @JsonProperty("final_formatted")
    private String finalFormatted;
}