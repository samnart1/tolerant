package com.tolerant.order_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);

		String profile = System.getProperty("spring.profiles.active", "baseline");

		System.out.println("order service started successfully with profile: %s".formatted(profile));
	}

}
