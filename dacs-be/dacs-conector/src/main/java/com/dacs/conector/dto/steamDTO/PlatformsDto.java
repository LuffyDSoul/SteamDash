package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class PlatformsDto {
    @JsonProperty("windows")
    private Boolean windows;
    
    @JsonProperty("mac")
    private Boolean mac;
    
    @JsonProperty("linux")
    private Boolean linux;
}