package com.dacs.conector.dto.steamDTO;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@Getter
@Setter
public class SteamAppListResponseDto {
    @JsonProperty("applist")
    private AppListDto appList;
    
    @Getter
    @Setter
    public static class AppListDto {
        @JsonProperty("apps")
        private List<SteamAppDto> apps;
    }
    
    @Getter
    @Setter
    public static class SteamAppDto {
        @JsonProperty("appid")
        private Long appId;
        
        @JsonProperty("name")
        private String name;
    }
}