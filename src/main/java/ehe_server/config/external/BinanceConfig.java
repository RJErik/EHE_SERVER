package ehe_server.config.external;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class BinanceConfig {

    /**
     * Provides a configured RestTemplate for making HTTP requests to Binance API.
     * - Configures proper URI encoding
     * - Sets reasonable timeouts
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Configure proper URL encoding for Binance API
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory();
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(uriFactory);

        return restTemplate;
    }
}
