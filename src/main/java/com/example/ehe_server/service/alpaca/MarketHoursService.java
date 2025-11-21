package com.example.ehe_server.service.alpaca;

import com.example.ehe_server.properties.AlpacaProperties;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class MarketHoursService {

    private final RestTemplate restTemplate;
    private final AlpacaProperties alpacaProperties;
    private final ObjectMapper objectMapper;
    private final LoggingServiceInterface loggingService;

    private static final String CLOCK_ENDPOINT = "/v2/clock";
    private static final ZoneId EASTERN_TIME = ZoneId.of("America/New_York");

    // Cache market status for a short period to avoid excessive API calls
    private volatile Boolean cachedIsOpen = null;
    private volatile Instant cacheExpiry = null;
    private static final long CACHE_DURATION_MS = 60_000; // 1 minute

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

    /**
     * Gets the next market open time
     */
    public ZonedDateTime getNextOpen() {
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
            String nextOpenStr = clock.get("next_open").asText();

            return ZonedDateTime.parse(nextOpenStr);
        } catch (Exception e) {
            loggingService.logError("Error getting next open time: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Gets the next market close time
     */
    public ZonedDateTime getNextClose() {
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
            String nextCloseStr = clock.get("next_close").asText();

            return ZonedDateTime.parse(nextCloseStr);
        } catch (Exception e) {
            loggingService.logError("Error getting next close time: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Checks if a symbol requires market hours checking
     * Crypto symbols (containing "/") trade 24/7
     * Stock symbols only trade during market hours
     */
    public boolean requiresMarketHours(String symbol) {
        return !isCryptoSymbol(symbol);
    }

    /**
     * Determines if a symbol is crypto based on the presence of "/"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }

    /**
     * Checks if we should fetch data for this symbol right now
     * - Crypto: Always true (24/7)
     * - Stocks: Only during market hours
     */
    public boolean shouldFetchData(String symbol) {
        if (isCryptoSymbol(symbol)) {
            return true; // Crypto trades 24/7
        }
        return isMarketOpen(); // Stocks only during market hours
    }

    /**
     * Invalidates the market status cache
     * Useful for testing or forcing a refresh
     */
    public void invalidateCache() {
        cachedIsOpen = null;
        cacheExpiry = null;
        loggingService.logAction("Market hours cache invalidated");
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", alpacaProperties.getApikey());
        headers.set("APCA-API-SECRET-KEY", alpacaProperties.getSecret());
        return headers;
    }
}