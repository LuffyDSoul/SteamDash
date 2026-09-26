package com.dacs.conector.dto.steamDTO;

import lombok.Data;

@Data
public class SteamResolveVanityResponseDto {
    private InnerResponse response;

    @Data
    public static class InnerResponse {
        private Integer success; // 1 success, 42 no match
        private String steamid;  // present when success == 1
        private String message;  // error message when not found
    }
}
