package com.auito.automationtools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@SpringBootApplication
@EnableScheduling
public class RemoteAppiumManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemoteAppiumManagerApplication.class, args);
	}

}
