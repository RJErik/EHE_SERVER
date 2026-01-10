package ehe_server.service.intf.alpaca;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public interface AlpacaTradingApiClientInterface {

    /**
     * Makes an authenticated request to Alpaca Trading API
     *
     * @param endpoint API endpoint
     * @param method HTTP method
     * @return Response from Alpaca API
     */
    ResponseEntity<String> makeRequest(String endpoint, HttpMethod method);

    /**
     * Makes an authenticated request to Alpaca Trading API with body
     *
     * @param endpoint API endpoint
     * @param method HTTP method
     * @param body Request body
     * @return Response from Alpaca API
     */
    ResponseEntity<String> makeRequest(String endpoint, HttpMethod method, Object body);
}