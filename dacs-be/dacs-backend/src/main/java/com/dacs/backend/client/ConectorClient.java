package com.dacs.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ConectorClient {

    private final RestTemplate restTemplate;

    @Value("${conector.url:http://localhost:9002/conector}")
    private String conectorBaseUrl;

    public ConectorClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Object getAppDetails(String appId) {
        String url = String.format("%s/steam/game/%s", conectorBaseUrl, appId);
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> resp = (java.util.Map<String, Object>) restTemplate.getForObject(url, java.util.Map.class);
        return resp;
    }

    @SuppressWarnings("unchecked")
    public java.util.Map<String, Object> getSteamSpyAppDetails(String appId) {
        String url = String.format("%s/steam/spy/appdetails/%s", conectorBaseUrl, appId);
        return (java.util.Map<String, Object>) restTemplate.getForObject(url, java.util.Map.class);
    }
}
