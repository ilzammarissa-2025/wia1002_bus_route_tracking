package com.example.wia1002_bus_route_tracking;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;

@SpringBootApplication
@EnableScheduling
public class Wia1002BusRouteTrackingApplication {

	public static void main(String[] args) {
		SpringApplication.run(Wia1002BusRouteTrackingApplication.class, args);
	}

	//auto run 
	@Bean
	public CommandLineRunner testDatabaseConnection(BusStopRepository stopRepo) {
		return args -> {
			System.out.println("DB CONNECTION TEST");
			long stopCount = stopRepo.count();
			System.out.println("Currently, there are " + stopCount + " bus stops in the database.");
			System.out.println("If you see this message without errors, PostGIS and Spring Boot are successfully connected!");
		};
	}
}
