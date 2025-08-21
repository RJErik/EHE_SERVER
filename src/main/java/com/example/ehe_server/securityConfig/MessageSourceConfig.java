package com.example.ehe_server.securityConfig;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() { // CORRECTED: Removed the 'String s' parameter
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages"); // looks for messages.properties in resources
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }
}