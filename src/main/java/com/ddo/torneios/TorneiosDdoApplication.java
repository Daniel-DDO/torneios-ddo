package com.ddo.torneios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableAsync
public class TorneiosDdoApplication {

	public static void main(String[] args) {
		SpringApplication.run(TorneiosDdoApplication.class, args);
	}

}
