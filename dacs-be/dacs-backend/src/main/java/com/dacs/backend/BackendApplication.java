package com.dacs.backend;

import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.TimeZone;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
        // Forzar timezone UTC al arrancar la aplicación para evitar problemas con PostgreSQL
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BackendApplication.class, args);
	}
    @Bean
    public ModelMapper getModelMapper() {
        return new ModelMapper();
    }
}
