package com.example.ehe_server.service.alpaca;

import com.example.ehe_server.properties.AlpacaProperties;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlpacaTradingApiClient {

    private final RestTemplate restTemplate;
    private final AlpacaProperties alpacaProperties;
    private final LoggingServiceInterface loggingService;

    private final Map<String, Long> requestWindowStartMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCountMap = new ConcurrentHashMap<>();
    private static final int REQUEST_LIMIT_PER_MINUTE = 200;
    private static final long MINUTE_IN_MS = 60_000;

    public AlpacaTradingApiClient(
            @Qualifier("alpacaRestTemplate") RestTemplate restTemplate,
            AlpacaProperties alpacaProperties,
            LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.alpacaProperties = alpacaProperties;
        this.loggingService = loggingService;
    }

    /**
     * Makes an authenticated request to Alpaca Trading API
     */
    public ResponseEntity<String> makeRequest(String endpoint, HttpMethod method) {
        return makeRequest(endpoint, method, null);
    }

    /**
     * Makes an authenticated request to Alpaca Trading API with body
     */
    public ResponseEntity<String> makeRequest(String endpoint, HttpMethod method, Object body) {
        String requestKey = "trading_" + endpoint;
        waitForRateLimitIfNeeded(requestKey);

        String url = alpacaProperties.getBaseurl() + endpoint;

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<?> entity = body != null ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);

        loggingService.logAction("Requesting Alpaca trading endpoint: " + url);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                method,
                entity,
                String.class
        );

        trackRequest(requestKey);
        return response;
    }

    /**
     * Creates authentication headers for Alpaca
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", alpacaProperties.getApikey());
        headers.set("APCA-API-SECRET-KEY", alpacaProperties.getSecret());
        return headers;
    }

    // Rate limit management
    private synchronized void waitForRateLimitIfNeeded(String requestKey) {
        long now = Instant.now().toEpochMilli();
        long windowStart = requestWindowStartMap.getOrDefault(requestKey, 0L);
        int count = requestCountMap.getOrDefault(requestKey, 0);

        // If we're in the same minute window and approaching limits
        if (now - windowStart < MINUTE_IN_MS && count > REQUEST_LIMIT_PER_MINUTE * 0.9) {
            // Calculate remaining time in the current window
            long waitTime = MINUTE_IN_MS - (now - windowStart);
            loggingService.logAction("Rate limit approaching for " + requestKey + ", waiting " + waitTime + " ms");

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loggingService.logError("Rate limit wait interrupted", e);
            }

            // Reset counters after waiting
            requestWindowStartMap.put(requestKey, Instant.now().toEpochMilli());
            requestCountMap.put(requestKey, 0);
        }

        // If window has expired, start a new one
        if (now - windowStart >= MINUTE_IN_MS) {
            requestWindowStartMap.put(requestKey, now);
            requestCountMap.put(requestKey, 0);
        }
    }

    private synchronized void trackRequest(String requestKey) {
        long now = Instant.now().toEpochMilli();
        long windowStart = requestWindowStartMap.getOrDefault(requestKey, now);
        int count = requestCountMap.getOrDefault(requestKey, 0);

        // Update counter
        requestCountMap.put(requestKey, count + 1);

        // If this is a new window, update the start time
        if (windowStart == 0) {
            requestWindowStartMap.put(requestKey, now);
        }
    }
}