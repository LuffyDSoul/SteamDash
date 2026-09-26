package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class ReleaseDateDto {
    @JsonProperty("coming_soon")
    private Boolean comingSoon;
    
    @JsonProperty("date")
    private String date;
}