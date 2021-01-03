package com.auito.automationtools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RemoteSlaveAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(RemoteSlaveAppApplication.class, args);
	}

}
