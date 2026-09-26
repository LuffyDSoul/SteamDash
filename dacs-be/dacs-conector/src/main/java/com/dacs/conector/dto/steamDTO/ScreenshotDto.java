package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class ScreenshotDto {
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("path_thumbnail")
    private String pathThumbnail;
    
    @JsonProperty("path_full")
    private String pathFull;
}
