package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class CategoryDto {
    @JsonProperty("id")
    private Integer id;
    
    @JsonProperty("description")
    private String description;
}