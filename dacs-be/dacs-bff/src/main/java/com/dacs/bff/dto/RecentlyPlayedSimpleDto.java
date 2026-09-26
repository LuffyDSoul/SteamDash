package com.dacs.bff.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecentlyPlayedSimpleDto {
    @JsonProperty("appId")
    private Long appId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("playtime2Weeks")
    private Integer playtime2Weeks; // minutos
}
