package com.dacs.bff.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.bff.ApplicationContextProvider;
import com.dacs.bff.service.ApiBackendService;
import com.dacs.bff.service.ApiConectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "")
public class HomeController {

	@Autowired
	private ApiConectorService apiConectorService;
	
	@Autowired
	private ApiBackendService apiBackendService;
	
	@GetMapping(value = "/")
    public Object root() {
		log.info("Ingrese a homecontroller");
		return "Hola desde MS bff de DACS";
	}
	
	@GetMapping(value = "/ping")
    public Object ping() {
		log.info("Ingrese a homecontroller ping");
		return "Hola desde MS bff de DACS PONG";
	}
	

	@GetMapping(value = "/version")
    public Object version() {
        return ApplicationContextProvider.getApplicationContext().getBean("buildInfo");
    }
	
	
	@GetMapping(value = "/conectorping")
    public Object conectorPing() {
		log.info("Ingrese a homecontroller conector ping");
		return apiConectorService.ping();
	}
	
	@GetMapping(value = "/backendping")
    public Object backendPing() {
		log.info("Ingrese a homecontroller backend ping");
		return apiBackendService.ping();
	}
	
	@GetMapping(value = "/status")
    public Object status() {
		log.info("BFF - Verificando status de todos los servicios");
		try {
			return "BFF Status: OK" +
				   "\nBackend Status: " + apiBackendService.ping() +
				   "\nConector Status: " + apiConectorService.ping();
		} catch (Exception e) {
			return "BFF Status: OK, pero hay servicios con problemas: " + e.getMessage();
		}
	}

}
	
