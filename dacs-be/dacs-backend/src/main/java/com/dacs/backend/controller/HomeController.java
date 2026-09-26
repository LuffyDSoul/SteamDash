package com.dacs.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dacs.backend.ApplicationContextProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "")
public class HomeController {


	@GetMapping(value = "/")
	public Object root() {
		log.info("Ingrese a homecontroller 7");
		return "Hola desde MS Conector de DACS";
	}

	@GetMapping(value = "/ping")
	public String ping() {
		return "Hola desde MS Backend de DACS PONG";
	}

	@GetMapping(value = "/version")
	public Object version() {
		return ApplicationContextProvider.getApplicationContext().getBean("buildInfo");
	}

}
