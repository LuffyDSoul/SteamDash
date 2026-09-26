package com.dacs.conector.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "steam.api")
@Getter
@Setter
public class SteamApiConfig {
    private String key;
    private String storeUrl;
    private String webUrl;
}