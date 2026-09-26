package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class GenreDto {
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("description")
    private String description;
}