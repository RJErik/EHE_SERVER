package ehe_server.service.binance;

import ehe_server.service.intf.binance.BinanceApiClientInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BinanceApiClient implements BinanceApiClientInterface {
    private final RestTemplate restTemplate;
    private static final String API_BASE_URL = "https://api.binance.com";
    private final LoggingServiceInterface loggingService;

    private final AtomicLong windowStart = new AtomicLong(0);
    private final AtomicInteger usedWeight = new AtomicInteger(0);
    private static final int WEIGHT_LIMIT_PER_MINUTE = 1200;
    private static final long MINUTE_IN_MS = 60_000;

    public BinanceApiClient(RestTemplate restTemplate, LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.loggingService = loggingService;
    }

    public ResponseEntity<String> getKlines(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        waitForRateLimitIfNeeded();

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(API_BASE_URL + "/api/v3/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval);

        if (startTime != null) builder.queryParam("startTime", startTime);
        if (endTime != null) builder.queryParam("endTime", endTime);
        if (limit != null) builder.queryParam("limit", limit);

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        loggingService.logAction("Requesting Binance klines: " + builder.toUriString());
        ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class
        );

        updateRateLimitFromHeaders(response.getHeaders());

        return response;
    }

    // Rate limit management
    private void waitForRateLimitIfNeeded() {
        long now = Instant.now().toEpochMilli();
        long currentWindowStart = windowStart.get();
        int currentWeight = usedWeight.get();

        if (now - currentWindowStart >= MINUTE_IN_MS) {
            windowStart.set(now);
            usedWeight.set(0);
            return;
        }

        if (currentWeight > WEIGHT_LIMIT_PER_MINUTE * 0.9) {
            long waitTime = MINUTE_IN_MS - (now - currentWindowStart);
            loggingService.logAction("Rate limit approaching, waiting " + waitTime + " ms");

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                loggingService.logError("Rate limit wait interrupted", e);
            }

            windowStart.set(Instant.now().toEpochMilli());
            usedWeight.set(0);
        }
    }

    private void updateRateLimitFromHeaders(HttpHeaders headers) {
        if (headers.containsKey("X-MBX-USED-WEIGHT-1M")) {
            String usedWeightHeader = headers.getFirst("X-MBX-USED-WEIGHT-1M");
            if (usedWeightHeader != null) {
                int weight = Integer.parseInt(usedWeightHeader);
                usedWeight.set(weight);
                loggingService.logAction("Current Binance API weight: " + weight + "/" + WEIGHT_LIMIT_PER_MINUTE);
            }
        }
    }

}
