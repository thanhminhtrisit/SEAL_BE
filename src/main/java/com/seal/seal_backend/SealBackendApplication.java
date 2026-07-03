package com.seal.seal_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SealBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SealBackendApplication.class, args);
	}

}
