package ehe_server.service.alpaca;

import ehe_server.entity.AutomatedTradeRule;
import ehe_server.properties.AlpacaProperties;
import ehe_server.service.intf.alpaca.AlpacaAccountServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
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
public class AlpacaAccountService implements AlpacaAccountServiceInterface {

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
    @Override
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
     * @param symbol        The trading symbol (e.g., "AAPL" or "BTC/USD")
     * @param side          "buy" or "sell"
     * @param amount        The amount to trade (either quantity or notional value)
     * @param timeInForce   "day", "gtc", "ioc", "fok"
     * @param quantityType  QUANTITY for number of shares/coins, QUOTE_ORDER_QTY for dollar amount (notional)
     * @return Response from Alpaca API
     */
    @Override
    public Map<String, Object> placeMarketOrder(String symbol, String side, BigDecimal amount,
                                                String timeInForce, AutomatedTradeRule.QuantityType quantityType) {
        try {
            String url = alpacaProperties.getBaseurl() + ORDERS_ENDPOINT;

            // Build request body
            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("symbol", symbol);
            orderRequest.put("side", side.toLowerCase());
            orderRequest.put("type", "market");
            orderRequest.put("time_in_force", timeInForce != null ? timeInForce : "day");

            // Add either qty or notional based on quantity type
            if (quantityType.equals(AutomatedTradeRule.QuantityType.QUANTITY)) {
                // qty: Specifies the number of shares/coins
                orderRequest.put("qty", amount.toPlainString());
            } else if (quantityType.equals(AutomatedTradeRule.QuantityType.QUOTE_ORDER_QTY)) {
                // notional: Specifies the dollar amount to trade
                orderRequest.put("notional", amount.toPlainString());
            }

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