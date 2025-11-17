package com.example.ehe_server.securityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.concurrent.Executor;

@Configuration
public class AlpacaConfig {

    /**
     * Provides a configured RestTemplate for making HTTP requests to Alpaca API.
     * - Configures proper URI encoding
     * - Sets reasonable timeouts
     */
    @Bean(name = "alpacaRestTemplate")
    public RestTemplate alpacaRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configure proper URL encoding for Alpaca API
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriFactory);

        // Additional configuration could go here
        // - Connection timeouts
        // - Request interceptors
        // - Error handlers

        return restTemplate;
    }

    /**
     * Provides a dedicated thread pool for processing Alpaca data.
     * This prevents Alpaca API operations from blocking the main application threads.
     */
    @Bean(name = "alpacaTaskExecutor")
    public Executor alpacaTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("alpaca-");
        executor.initialize();
        return executor;
    }
}