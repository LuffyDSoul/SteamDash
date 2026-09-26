package com.dacs.conector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ConectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConectorApplication.class, args);
	}

}
