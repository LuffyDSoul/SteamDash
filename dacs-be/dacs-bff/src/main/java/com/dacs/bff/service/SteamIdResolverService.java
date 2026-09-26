package com.dacs.bff.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dacs.bff.api.client.ApiConectorClient;
import com.dacs.bff.dto.SteamResolveVanityResponseDto;

@Service
public class SteamIdResolverService {

    @Autowired
    private ApiConectorClient apiConectorClient;

    private static final java.util.regex.Pattern STEAMID64_PATTERN = java.util.regex.Pattern.compile("^\\d{17}$");

    /**
     * Accepts either steamid64 or vanity string and returns steamid64.
     * Throws IllegalArgumentException when vanity cannot be resolved.
     */
    public String resolve(String idOrVanity) {
        if (idOrVanity == null || idOrVanity.isBlank()) {
            throw new IllegalArgumentException("steamId/vanity no provisto");
        }

        if (STEAMID64_PATTERN.matcher(idOrVanity).matches()) {
            return idOrVanity;
        }

        SteamResolveVanityResponseDto resp = apiConectorClient.resolveVanity(idOrVanity);
        if (resp != null && resp.getResponse() != null && Integer.valueOf(1).equals(resp.getResponse().getSuccess())
                && resp.getResponse().getSteamid() != null && !resp.getResponse().getSteamid().isBlank()) {
            return resp.getResponse().getSteamid();
        }

        String msg = (resp != null && resp.getResponse() != null && resp.getResponse().getMessage() != null)
                ? resp.getResponse().getMessage() : "No se pudo resolver vanity URL";
        throw new IllegalArgumentException(msg);
    }
}
