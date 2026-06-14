package com.example.iotbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IotbackendApplication {
	
    public static void main(String[] args) {

		SpringApplication.run(IotbackendApplication.class, args);

	}

}
