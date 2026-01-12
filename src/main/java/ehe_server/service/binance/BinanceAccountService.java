package ehe_server.service.binance;

import ehe_server.entity.AutomatedTradeRule;
import ehe_server.service.intf.binance.BinanceAccountServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class BinanceAccountService implements BinanceAccountServiceInterface {

    private final RestTemplate restTemplate;
    private final LoggingServiceInterface loggingService;
    private static final String BINANCE_API_URL = "https://testnet.binance.vision";
    private static final String ACCOUNT_ENDPOINT = "/api/v3/account";
    private static final String ORDER_ENDPOINT = "/api/v3/order";
    private static final String HMAC_SHA256 = "HmacSHA256";

    public BinanceAccountService(LoggingServiceInterface loggingService, RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.loggingService = loggingService;
    }

    public Map<String, Object> getAccountInfo(String apiKey, String secretKey) {
        try {
            // Build URL with timestamp parameter
            long timestamp = Instant.now().toEpochMilli();
            String queryParams = "timestamp=" + timestamp;

            // Generate signature
            String signature = generateSignature(queryParams, secretKey);

            // Build the complete URL
            String url = UriComponentsBuilder.fromUriString(BINANCE_API_URL + ACCOUNT_ENDPOINT)
                    .query(queryParams)
                    .queryParam("signature", signature)
                    .toUriString();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);

            // Make request
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            loggingService.logError("Error getting account info from Binance: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to connect to Binance API: " + e.getMessage());
            return errorResult;
        }
    }

    /**
     * Places a market order on Binance
     *
     * @param apiKey        The API key
     * @param secretKey     The secret key
     * @param symbol        The trading pair symbol (e.g., "BTCUSDT")
     * @param side          "BUY" or "SELL"
     * @param type          Order type (e.g., "MARKET")
     * @param quantity      The quantity to trade
     * @param quantityType  The quantity type (e.g., QUANTITY, QUOTE_ORDER_QTY)
     * @return Response from Binance API
     */
    public Map<String, Object> placeMarketOrder(String apiKey, String secretKey, String symbol,
                                                String side, String type, BigDecimal quantity, AutomatedTradeRule.QuantityType quantityType) {
        try {
            // Build URL with required parameters
            long timestamp = Instant.now().toEpochMilli();
            StringBuilder queryParamsBuilder = new StringBuilder();
            queryParamsBuilder.append("symbol=").append(symbol)
                    .append("&side=").append(side)
                    .append("&type=").append(type)
                    .append("&timestamp=").append(timestamp);

            // Add either quantity or quoteOrderQty based on which one is provided
            if (quantityType.equals(AutomatedTradeRule.QuantityType.QUANTITY)) {
                queryParamsBuilder.append("&quantity=").append(quantity.toPlainString());
            }
            if (quantityType.equals(AutomatedTradeRule.QuantityType.QUOTE_ORDER_QTY)) {
                queryParamsBuilder.append("&quoteOrderQty=").append(quantity.toPlainString());
            }

            String queryParams = queryParamsBuilder.toString();

            // Generate signature
            String signature = generateSignature(queryParams, secretKey);

            // Build the complete URL
            String url = UriComponentsBuilder.fromUriString(BINANCE_API_URL + ORDER_ENDPOINT)
                    .query(queryParams)
                    .queryParam("signature", signature)
                    .toUriString();

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-MBX-APIKEY", apiKey);

            // Create request entity with empty body (POST request)
            HttpEntity<String> requestEntity = new HttpEntity<>("", headers);

            // Make POST request
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            loggingService.logError("Error placing order on Binance: " + e.getMessage(), e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", "Failed to place order via Binance API: " + e.getMessage());
            return errorResult;
        }
    }

    private String generateSignature(String data, String key) {
        try {
            Mac sha256_HMAC = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), HMAC_SHA256);
            sha256_HMAC.init(secretKeySpec);

            byte[] hash = sha256_HMAC.doFinal(data.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating signature", e);
        }
    }
}
