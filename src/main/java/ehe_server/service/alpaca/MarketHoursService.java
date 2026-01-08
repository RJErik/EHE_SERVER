package ehe_server.service.alpaca;

import ehe_server.properties.AlpacaProperties;
import ehe_server.service.intf.log.LoggingServiceInterface;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class MarketHoursService {

    private final RestTemplate restTemplate;
    private final AlpacaProperties alpacaProperties;
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;

    private static final String CLOCK_ENDPOINT = "/v2/clock";

    private volatile Boolean cachedIsOpen = null;
    private volatile Instant cacheExpiry = null;
    private static final long CACHE_DURATION_MS = 60_000;

    public MarketHoursService(
            @Qualifier("alpacaRestTemplate") RestTemplate restTemplate,
            AlpacaProperties alpacaProperties,
            ObjectMapper objectMapper,
            LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.alpacaProperties = alpacaProperties;
        this.objectMapper = objectMapper;
        this.loggingService = loggingService;
    }

    /**
     * Checks if the stock market is currently open
     * Note: Crypto markets are always open (24/7)
     */
    public boolean isMarketOpen() {
        // Check cache first
        if (cachedIsOpen != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry)) {
            return cachedIsOpen;
        }

        try {
            String url = alpacaProperties.getBaseurl() + CLOCK_ENDPOINT;

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            JsonNode clock = objectMapper.readTree(response.getBody());
            boolean isOpen = clock.get("is_open").asBoolean();

            // Update cache
            cachedIsOpen = isOpen;
            cacheExpiry = Instant.now().plusMillis(CACHE_DURATION_MS);

            loggingService.logAction("Market status: " + (isOpen ? "OPEN" : "CLOSED"));

            return isOpen;
        } catch (Exception e) {
            loggingService.logError("Error checking market hours: " + e.getMessage(), e);
            // Default to assuming market is closed on error
            return false;
        }
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", alpacaProperties.getApikey());
        headers.set("APCA-API-SECRET-KEY", alpacaProperties.getSecret());
        return headers;
    }
}