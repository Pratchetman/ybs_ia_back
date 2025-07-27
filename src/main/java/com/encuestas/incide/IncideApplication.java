package com.encuestas.incide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class IncideApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncideApplication.class, args);
	}

}
