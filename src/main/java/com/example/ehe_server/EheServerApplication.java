package com.example.ehe_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.example.ehe_server.entity"})
@EnableJpaRepositories(basePackages = {"com.example.ehe_server.repository"})
public class EheServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EheServerApplication.class, args);
    }
}
