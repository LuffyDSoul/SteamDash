package com.dacs.bff.config;

import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	// Bean para configurar CORS globalmente.
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		// Crea una nueva fuente de configuración basada en URL.
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		// Define las reglas de CORS.
		CorsConfiguration config = new CorsConfiguration();
		
		// Permite la credenciales, como cookies y encabezados de autenticación.
		config.setAllowCredentials(true);
		
		// Permite el origen de tu aplicación de Angular. Es crucial para resolver tu error.
		// En un entorno de producción, reemplaza "http://localhost:4200" con el dominio de tu frontend.
		config.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
		
		// Permite todos los encabezados HTTP.
		config.setAllowedHeaders(Arrays.asList("*"));
		
		// Permite los métodos HTTP que tu frontend usará.
		config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		
		// Registra esta configuración para todas las rutas ("/**").
		source.registerCorsConfiguration("/**", config);
		
		return source;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// Habilita la configuración de CORS definida en el bean de arriba.
		http.cors(); 
		
		// Deshabilita CSRF para simplificar, pero considera habilitarlo en producción.
		http.csrf().disable();
		
		// Autoriza todas las peticiones a cualquier URL.
		http.authorizeRequests(authorize -> authorize
			.anyRequest().permitAll()
		);

		return http.build();
	}

}

