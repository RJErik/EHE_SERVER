package com.example.ehe_server.service.alpaca;

import com.example.ehe_server.properties.AlpacaProperties;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlpacaDataApiClient {

    private final RestTemplate restTemplate;
    private final AlpacaProperties alpacaProperties;
    private final LoggingServiceInterface loggingService;

    private final Map<String, Long> requestWindowStartMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCountMap = new ConcurrentHashMap<>();
    private static final int REQUEST_LIMIT_PER_MINUTE = 200;
    private static final long MINUTE_IN_MS = 60_000;

    public AlpacaDataApiClient(
            @Qualifier("alpacaRestTemplate") RestTemplate restTemplate,
            AlpacaProperties alpacaProperties,
            LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.alpacaProperties = alpacaProperties;
        this.loggingService = loggingService;
    }

    /**
     * Gets historical bars for a stock symbol
     * Uses /v2/stocks/{symbol}/bars endpoint
     */
    public ResponseEntity<String> getStockBars(String symbol, String timeframe,
                                               ZonedDateTime start, ZonedDateTime end,
                                               String pageToken) {
        String requestKey = "stock_bars_" + symbol;
        waitForRateLimitIfNeeded(requestKey);

        String endpoint = "/v2/stocks/" + symbol + "/bars";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(alpacaProperties.getDataurl() + endpoint)
                .queryParam("timeframe", convertTimeframeToAlpaca(timeframe));

        if (start != null) {
            builder.queryParam("start", start.format(DateTimeFormatter.ISO_INSTANT));
        }
        if (end != null) {
            builder.queryParam("end", end.format(DateTimeFormatter.ISO_INSTANT));
        }
        if (pageToken != null) {
            builder.queryParam("page_token", pageToken);
        }

        // Add limit to control page size
        builder.queryParam("limit", 10000);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        loggingService.logAction("Requesting Alpaca stock bars: " + builder.toUriString());

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
        );

        trackRequest(requestKey);
        return response;
    }

    /**
     * Gets historical bars for a crypto symbol
     * Uses /v1beta3/crypto/us/bars endpoint
     *
     * Note: Crypto symbols should be in format "BTC/USD"
     */
    public ResponseEntity<String> getCryptoBars(String symbol, String timeframe,
                                                ZonedDateTime start, ZonedDateTime end,
                                                String pageToken) {
        String requestKey = "crypto_bars_" + symbol;
        waitForRateLimitIfNeeded(requestKey);

        String endpoint = "/v1beta3/crypto/us/bars";
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(alpacaProperties.getDataurl() + endpoint)
                .queryParam("symbols", symbol)
                .queryParam("timeframe", convertTimeframeToAlpaca(timeframe));

        if (start != null) {
            builder.queryParam("start", start.format(DateTimeFormatter.ISO_INSTANT));
        }
        if (end != null) {
            builder.queryParam("end", end.format(DateTimeFormatter.ISO_INSTANT));
        }
        if (pageToken != null) {
            builder.queryParam("page_token", pageToken);
        }

        // Add limit to control page size
        builder.queryParam("limit", 10000);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        loggingService.logAction("Requesting Alpaca crypto bars: " + builder.toUriString());

        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
        );

        trackRequest(requestKey);
        return response;
    }

    /**
     * Automatically detects if symbol is crypto (contains "/") and calls appropriate endpoint
     */
    public ResponseEntity<String> getBars(String symbol, String timeframe,
                                          ZonedDateTime start, ZonedDateTime end,
                                          String pageToken) {
        if (isCryptoSymbol(symbol)) {
            return getCryptoBars(symbol, timeframe, start, end, pageToken);
        } else {
            return getStockBars(symbol, timeframe, start, end, pageToken);
        }
    }

    /**
     * Determines if a symbol is crypto based on the presence of "/"
     * Crypto: "BTC/USD", "ETH/USD"
     * Stock: "AAPL", "GOOGL"
     */
    private boolean isCryptoSymbol(String symbol) {
        return symbol.contains("/");
    }

    /**
     * Converts our internal timeframe format to Alpaca format
     * M1 -> 1Min, H1 -> 1Hour, D1 -> 1Day
     */
    private String convertTimeframeToAlpaca(String timeframe) {
        return switch(timeframe.toUpperCase()) {
            case "M1", "1M" -> "1Min";
            case "M5", "5M" -> "5Min";
            case "M15", "15M" -> "15Min";
            case "H1", "1H" -> "1Hour";
            case "H4", "4H" -> "4Hour";
            case "D1", "1D" -> "1Day";
            default -> timeframe; // Pass through if already in Alpaca format
        };
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

        if (now - windowStart < MINUTE_IN_MS && count > REQUEST_LIMIT_PER_MINUTE * 0.9) {
            long waitTime = MINUTE_IN_MS - (now - windowStart);
            loggingService.logAction("Rate limit approaching for " + requestKey + ", waiting " + waitTime + " ms");

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loggingService.logError("Rate limit wait interrupted", e);
            }

            requestWindowStartMap.put(requestKey, Instant.now().toEpochMilli());
            requestCountMap.put(requestKey, 0);
        }

        if (now - windowStart >= MINUTE_IN_MS) {
            requestWindowStartMap.put(requestKey, now);
            requestCountMap.put(requestKey, 0);
        }
    }

    private synchronized void trackRequest(String requestKey) {
        long now = Instant.now().toEpochMilli();
        long windowStart = requestWindowStartMap.getOrDefault(requestKey, now);
        int count = requestCountMap.getOrDefault(requestKey, 0);

        requestCountMap.put(requestKey, count + 1);

        if (windowStart == 0) {
            requestWindowStartMap.put(requestKey, now);
        }
    }
}