package ehe_server.service.alpaca;

import ehe_server.entity.AutomatedTradeRule;
import ehe_server.service.intf.alpaca.AlpacaAccountServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlpacaAccountService implements AlpacaAccountServiceInterface {

    private final RestTemplate restTemplate;
    private final LoggingServiceInterface loggingService;

    private static final String ALPACA_API_URL = "https://paper-api.alpaca.markets";
    private static final String ACCOUNT_ENDPOINT = "/v2/account";
    private static final String POSITIONS_ENDPOINT = "/v2/positions";
    private static final String ORDERS_ENDPOINT = "/v2/orders";
    private static final String TIME_IN_FORCE = "day";

    public AlpacaAccountService(
            @Qualifier("alpacaRestTemplate") RestTemplate restTemplate,
            LoggingServiceInterface loggingService) {
        this.restTemplate = restTemplate;
        this.loggingService = loggingService;
    }

    /**
     * Fetches both Account Data (Cash) and Positions (Holdings)
     * and merges them into a single Map.
     */
    @Override
    public Map<String, Object> getAccountInfo(String apiKey, String secretKey) {
        try {
            HttpHeaders headers = createAuthHeaders(apiKey, secretKey);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 1. Fetch Account Data (for Cash)
            String accountUrl = ALPACA_API_URL + ACCOUNT_ENDPOINT;
            ResponseEntity<Map> accountResponse = restTemplate.exchange(
                    accountUrl,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );
            Map<String, Object> accountData = accountResponse.getBody();

            // 2. Fetch Positions (for Holdings)
            String positionsUrl = ALPACA_API_URL + POSITIONS_ENDPOINT;
            ResponseEntity<List<Map<String, Object>>> positionsResponse = restTemplate.exchange(
                    positionsUrl,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );
            List<Map<String, Object>> positionsData = positionsResponse.getBody();

            // 3. Merge them
            // We start with accountData so "cash" is at the top level
            Map<String, Object> mergedResult = new HashMap<>();
            if (accountData != null) {
                mergedResult.putAll(accountData);
            }

            // Add positions under the key "positions" so HoldingsSyncService can find it
            if (positionsData != null) {
                mergedResult.put("positions", positionsData);
            }

            return mergedResult;

        } catch (Exception e) {
            loggingService.logError("Error getting full account info from Alpaca: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to connect to Alpaca API: " + e.getMessage());
            return errorResult;
        }
    }

    @Override
    public Map<String, Object> placeMarketOrder(
            String apiKey,
            String secretKey,
            String symbol,
            String side,
            String type,
            BigDecimal amount,
            AutomatedTradeRule.QuantityType quantityType) {
        try {
            String url = ALPACA_API_URL + ORDERS_ENDPOINT;

            // Prepare the POST request
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("symbol", symbol);
            orderRequest.put("side", side.toLowerCase());
            orderRequest.put("type", type.toLowerCase());
            orderRequest.put("time_in_force", TIME_IN_FORCE);

            if (quantityType.equals(AutomatedTradeRule.QuantityType.QUANTITY)) {
                orderRequest.put("qty", amount.toPlainString());
            } else if (quantityType.equals(AutomatedTradeRule.QuantityType.QUOTE_ORDER_QTY)) {
                orderRequest.put("notional", amount.toPlainString());
            }

            HttpHeaders headers = createAuthHeaders(apiKey, secretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> postRequestEntity = new HttpEntity<>(orderRequest, headers);

            // Execute POST (Place Order)
            ResponseEntity<Map> postResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    postRequestEntity,
                    Map.class
            );

            Map<String, Object> initialOrderData = postResponse.getBody();

            //  Check on the order (GET) using the ID
            if (initialOrderData != null && initialOrderData.containsKey("id")) {
                String orderId = (String) initialOrderData.get("id");

                String getOrderUrl = ALPACA_API_URL + ORDERS_ENDPOINT + "/" + orderId;

                // We can reuse the headers, but we don't need the JSON body for a GET
                HttpEntity<Void> getRequestEntity = new HttpEntity<>(headers);

                ResponseEntity<Map> getResponse = restTemplate.exchange(
                        getOrderUrl,
                        HttpMethod.GET,
                        getRequestEntity,
                        Map.class
                );

                // Return the UPDATED status
                return getResponse.getBody();
            }

            // Fallback: If we couldn't find an ID, return the initial response
            return initialOrderData;

        } catch (Exception e) {
            loggingService.logError("Error placing/checking order on Alpaca: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to place order via Alpaca API: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Creates HTTP headers with Alpaca authentication
     *
     * @param apiKey    The API key
     * @param secretKey The secret key
     * @return HttpHeaders with authentication set
     */
    private HttpHeaders createAuthHeaders(String apiKey, String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("APCA-API-KEY-ID", apiKey);
        headers.set("APCA-API-SECRET-KEY", secretKey);
        return headers;
    }
}