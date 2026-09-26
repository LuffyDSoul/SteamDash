package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

@Getter
@Setter
public class SteamAppDetailsResponseDto {
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("data")
    private SteamGameDto data;
}