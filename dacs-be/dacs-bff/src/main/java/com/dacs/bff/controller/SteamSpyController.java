package com.dacs.bff.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.api.client.ApiConectorClient;

@RestController
@RequestMapping("/steamspy")
public class SteamSpyController {

    @Autowired
    private ApiConectorClient apiConectorClient;

    @GetMapping("/appdetails/{appId}")
    public ResponseEntity<java.util.Map<String, Object>> getAppDetails(@PathVariable("appId") String appId) {
        try {
            java.util.Map<String, Object> details = apiConectorClient.getSteamSpyAppDetails(appId);
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
