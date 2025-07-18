package com.example.ehe_server.service.binance;

import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class BinanceApiClient {
    private final RestTemplate restTemplate;
    private static final String API_BASE_URL = "https://api.binance.com";
    private final LoggingServiceInterface loggingService;

    // Rate limit tracking
    private final Map<String, Long> requestWindowStartMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCountMap = new ConcurrentHashMap<>();
    private static final int WEIGHT_LIMIT_PER_MINUTE = 1200;
    private static final long MINUTE_IN_MS = 60_000;

    public BinanceApiClient(RestTemplate restTemplate, LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.loggingService = loggingService;
    }

    public ResponseEntity<String> getKlines(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        String requestKey = "klines_" + symbol;
        waitForRateLimitIfNeeded(requestKey);

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(API_BASE_URL + "/api/v3/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval);

        if (startTime != null) builder.queryParam("startTime", startTime);
        if (endTime != null) builder.queryParam("endTime", endTime);
        if (limit != null) builder.queryParam("limit", limit);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        //SYSTEM SET HERE

        loggingService.logAction("Requesting Binance klines: " + builder.toUriString());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
        );

        // Track this request for rate limiting
        trackRequest(requestKey);

        // Handle rate limit headers
        updateRateLimitFromHeaders(response.getHeaders());

        return response;
    }

    // Rate limit management
    private synchronized void waitForRateLimitIfNeeded(String requestKey) {
        long now = Instant.now().toEpochMilli();
        long windowStart = requestWindowStartMap.getOrDefault(requestKey, 0L);
        int count = requestCountMap.getOrDefault(requestKey, 0);

        // If we're in the same minute window and approaching limits
        if (now - windowStart < MINUTE_IN_MS && count > WEIGHT_LIMIT_PER_MINUTE * 0.9) {
            // Calculate remaining time in the current window
            long waitTime = MINUTE_IN_MS - (now - windowStart);
            //SYSTEM SET HERE
            loggingService.logAction("Rate limit approaching for " + requestKey + ", waiting " + waitTime + " ms");

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //SYSTEM SET HERE
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

    private void updateRateLimitFromHeaders(HttpHeaders headers) {
        if (headers.containsKey("X-MBX-USED-WEIGHT-1M")) {
            String usedWeight = headers.getFirst("X-MBX-USED-WEIGHT-1M");
            //SYSTEM SET HERE
            loggingService.logAction("Current Binance API weight: " + usedWeight + "/" + WEIGHT_LIMIT_PER_MINUTE);
        }
    }
}
