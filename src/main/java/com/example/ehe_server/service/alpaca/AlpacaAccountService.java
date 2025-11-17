package com.example.ehe_server.service.alpaca;

import com.example.ehe_server.securityConfig.AlpacaProperties;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AlpacaAccountService {

    private final RestTemplate restTemplate;
    private final AlpacaProperties alpacaProperties;
    private final LoggingServiceInterface loggingService;

    private static final String ACCOUNT_ENDPOINT = "/v2/account";
    private static final String ORDERS_ENDPOINT = "/v2/orders";

    public AlpacaAccountService(
            @Qualifier("alpacaRestTemplate") RestTemplate restTemplate,
            AlpacaProperties alpacaProperties,
            LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.alpacaProperties = alpacaProperties;
        this.loggingService = loggingService;
    }

    /**
     * Gets account information from Alpaca
     */
    public Map<String, Object> getAccountInfo() {
        try {
            String url = alpacaProperties.getBaseurl() + ACCOUNT_ENDPOINT;

            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            loggingService.logError("Error getting account info from Alpaca: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to connect to Alpaca API: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Places a market order on Alpaca
     *
     * @param symbol       The trading symbol (e.g., "AAPL" or "BTC/USD")
     * @param side         "buy" or "sell"
     * @param qty          The quantity to trade
     * @param timeInForce  "day", "gtc", "ioc", "fok"
     * @return Response from Alpaca API
     */
    public Map<String, Object> placeMarketOrder(String symbol, String side, BigDecimal qty, String timeInForce) {
        try {
            String url = alpacaProperties.getBaseurl() + ORDERS_ENDPOINT;

            // Build request body
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("symbol", symbol);
            orderRequest.put("qty", qty.toPlainString());
            orderRequest.put("side", side.toLowerCase());
            orderRequest.put("type", "market");
            orderRequest.put("time_in_force", timeInForce != null ? timeInForce : "day");

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            loggingService.logError("Error placing order on Alpaca: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to place order via Alpaca API: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Places a limit order on Alpaca
     *
     * @param symbol       The trading symbol (e.g., "AAPL" or "BTC/USD")
     * @param side         "buy" or "sell"
     * @param qty          The quantity to trade
     * @param limitPrice   The limit price
     * @param timeInForce  "day", "gtc", "ioc", "fok"
     * @return Response from Alpaca API
     */
    public Map<String, Object> placeLimitOrder(String symbol, String side, BigDecimal qty,
                                               BigDecimal limitPrice, String timeInForce) {
        try {
            String url = alpacaProperties.getBaseurl() + ORDERS_ENDPOINT;

            // Build request body
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("symbol", symbol);
            orderRequest.put("qty", qty.toPlainString());
            orderRequest.put("side", side.toLowerCase());
            orderRequest.put("type", "limit");
            orderRequest.put("limit_price", limitPrice.toPlainString());
            orderRequest.put("time_in_force", timeInForce != null ? timeInForce : "day");

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(orderRequest, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            loggingService.logError("Error placing limit order on Alpaca: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to place limit order via Alpaca API: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Creates HTTP headers with Alpaca authentication
     * Much simpler than Binance - no signature generation needed!
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", alpacaProperties.getApikey());
        headers.set("APCA-API-SECRET-KEY", alpacaProperties.getSecret());
        return headers;
    }
}