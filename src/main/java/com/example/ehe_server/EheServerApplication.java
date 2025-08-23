package com.example.ehe_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {"com.example.ehe_server.entity"})
@EnableJpaRepositories(basePackages = {"com.example.ehe_server.repository"})
@EnableScheduling // Added for WebSocket heartbeat and update checking
@EnableAsync  // Add this line
public class EheServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EheServerApplication.class, args);
    }
}
