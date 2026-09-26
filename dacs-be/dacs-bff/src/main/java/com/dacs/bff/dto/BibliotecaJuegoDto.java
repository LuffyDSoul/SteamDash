package com.dacs.bff.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BibliotecaJuegoDto {
    private Long appId;
    private String name;
    private String headerImage;
    private String imgVertical; // https://cdn.akamai.steamstatic.com/steam/apps/{appId}/library_600x900.jpg
    private String imgIconUrl;
    private Boolean isFree;
    private String price;
    private String storeUrl;
    private List<Tags> tags;
}
