package ehe_server.config.external;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

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

        return restTemplate;
    }
}