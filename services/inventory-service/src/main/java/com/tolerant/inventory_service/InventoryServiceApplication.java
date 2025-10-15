package com.tolerant.inventory_service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

import com.tolerant.inventory_service.model.Inventory;
import com.tolerant.inventory_service.repo.InventoryRepository;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableDiscoveryClient
@Slf4j
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);

		System.out.println("Inventory service started at port: 8083");
	}

	@Bean
	CommandLineRunner initInventory(InventoryRepository inventoryRepository) {
		return args -> {
			log.info("initializing sample inventory data...");

			//sample products
			Inventory product1 = Inventory.builder()
				.productId("PROD-001")
				.productName("Laptop")
				.quantityAvailable(50)
				.quantityReserved(0)
				.build();

			Inventory product2 = Inventory.builder()
				.productId("PROD-002")
				.productName("Mouse")
				.quantityAvailable(200)
				.quantityReserved(0)
				.build();

			Inventory product3 = Inventory.builder()
				.productId("PROD-003")
				.productName("Keyboard")
				.quantityAvailable(100)
				.quantityReserved(0)
				.build();


			Inventory product4 = Inventory.builder()
				.productId("PROD-004")
				.productName("Monitor")
				.quantityAvailable(30)
				.quantityReserved(0)
				.build();

			Inventory product5 = Inventory.builder()
				.productId("PROD-005")
				.productName("Headphones")
				.quantityAvailable(75)
				.quantityReserved(0)
				.build();

			inventoryRepository.save(product1);
			inventoryRepository.save(product2);
			inventoryRepository.save(product3);
			inventoryRepository.save(product4);
			inventoryRepository.save(product5);

			log.info("sample inventory data initialized successfully!");
		};
	}

}
