package com.dacs.bff.dto;

import lombok.Data;

@Data
public class SteamResolveVanityResponseDto {
    private InnerResponse response;

    @Data
    public static class InnerResponse {
        private Integer success;
        private String steamid;
        private String message;
    }
}
