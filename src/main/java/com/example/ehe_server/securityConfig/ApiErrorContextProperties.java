package com.example.ehe_server.securityConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps the custom error context properties from application.properties
 * into a Map. This allows for easy injection and lookup of URL-based
 * error message prefixes.
 *
 * The properties should be in the format:
 * api.error-contexts.mapping[/api/some/url]=Error Prefix:
 * api.error-contexts.default-context=Default Prefix:
 */
@Configuration
@ConfigurationProperties(prefix = "api.error-contexts")
public class ApiErrorContextProperties {

    private final Map<String, String> mapping = new HashMap<>();
    private String defaultContext = "An error occurred: ";

    public Map<String, String> getMapping() {
        return mapping;
    }

    public String getDefaultContext() {
        return defaultContext;
    }

    public void setDefaultContext(String defaultContext) {
        this.defaultContext = defaultContext;
    }
}
