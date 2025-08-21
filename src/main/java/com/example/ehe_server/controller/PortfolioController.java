package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.portfolio.*;
import com.example.ehe_server.service.portfolio.PortfolioByPlatformService;
import com.example.ehe_server.service.portfolio.PortfolioDetailsService;
import com.example.ehe_server.service.portfolio.PortfolioValueService;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class PortfolioController {

    private final PortfolioRetrievalServiceInterface portfolioRetrievalService;
    private final PortfolioCreationServiceInterface portfolioCreationService;
    private final PortfolioRemovalServiceInterface portfolioRemovalService;
    private final PortfolioSearchServiceInterface portfolioSearchService;
    private final UserContextService userContextService;
    private final MessageSource messageSource;
    private final PortfolioValueServiceInterface portfolioValueService;
    private final PortfolioDetailsServiceInterface portfolioDetailsService;
    private final PortfolioByPlatformServiceInterface portfolioByPlatformService;

    public PortfolioController(
            PortfolioRetrievalServiceInterface portfolioRetrievalService,
            PortfolioCreationServiceInterface portfolioCreationService,
            PortfolioRemovalServiceInterface portfolioRemovalService,
            PortfolioSearchServiceInterface portfolioSearchService,
            UserContextService userContextService,
            MessageSource messageSource,
            PortfolioValueService portfolioValueService,
            PortfolioDetailsService portfolioDetailsService,
            PortfolioByPlatformService portfolioByPlatformService) {
        this.portfolioRetrievalService = portfolioRetrievalService;
        this.portfolioCreationService = portfolioCreationService;
        this.portfolioRemovalService = portfolioRemovalService;
        this.portfolioSearchService = portfolioSearchService;
        this.userContextService = userContextService;
        this.messageSource = messageSource;
        this.portfolioValueService = portfolioValueService;
        this.portfolioDetailsService = portfolioDetailsService;
        this.portfolioByPlatformService = portfolioByPlatformService;
    }

    /**
     * Endpoint to retrieve all portfolios for the current user with their calculated values
     *
     * @return List of portfolios with values and success status
     */
    @GetMapping("/portfolios")
    public ResponseEntity<Map<String, Object>> getPortfolios() {
        // Call automated trade rule retrieval service
        List<PortfolioRetrievalResponse> portfolioRetrievalResponses = portfolioRetrievalService.getPortfolios(userContextService.getCurrentUserId().intValue());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolios", portfolioRetrievalResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to create a new portfolio for the user
     *
     * @param request Contains portfolio name and API key ID
     * @return Success status and details of created portfolio
     */
    @PostMapping("/portfolios")
    public ResponseEntity<Map<String, Object>> createPortfolio(@RequestBody PortfolioCreationRequest request) {
        // Call automated trade rule retrieval service
        PortfolioCreationResponse portfolioCreationResponse = portfolioCreationService.createPortfolio(
                request.getPortfolioName(),
                request.getApiKeyId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.add", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolio", portfolioCreationResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Endpoint to delete a portfolio and all its holdings
     *
     * @param request Contains the portfolio ID to delete
     * @return Success status and confirmation message
     */
    @DeleteMapping("/portfolios")
    public ResponseEntity<Map<String, Object>> removePortfolio(@RequestBody PortfolioRemovalRequest request) {
        // Call automated trade rule retrieval service
        portfolioRemovalService.removePortfolio(request.getPortfolioId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.remove", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/portfolios/search")
    public ResponseEntity<Map<String, Object>> searchPortfolio(@RequestBody PortfolioSearchRequest request) {
        // Call automated trade rule retrieval service
        List<PortfolioSearchResponse> portfolioSearchResponses = portfolioSearchService.searchPortfolios(userContextService.getCurrentUserId().intValue(), request.getType(), request.getPlatform(), request.getMinValue(), request.getMaxValue());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.search", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolios", portfolioSearchResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OVER PATH, RESPONSE

    @PostMapping("/portfolios/update-holdings")
    public ResponseEntity<Map<String, Object>> updateHoldings(@RequestBody PortfolioUpdateHoldingsRequest request) {
        // Call portfolio value service to update holdings
        HoldingsUpdateResponse holdingsUpdateResponse = portfolioValueService.updateHoldings(userContextService.getCurrentUserId().intValue(), request.getPortfolioId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.holdings.updated", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("holdings", holdingsUpdateResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OVER PATH, RESPONSE, TECHNICALLY NOT USED In THE FRONTEND

    /**
     * Endpoint to get the current value of a portfolio
     *
     * @return Success status and portfolio value details
     */
    @PostMapping("/portfolios/value")
    public ResponseEntity<Map<String, Object>> getPortfolioValue(@RequestBody PortfolioValueRequest request) {
        // Call portfolio value service to calculate value
        PortfolioValueResponse portfolioValueResponse = portfolioValueService.calculatePortfolioValue(userContextService.getCurrentUserId().intValue(), request.getPortfolioId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.value.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolioValue", portfolioValueResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    //LOOK OVER PATH, RESPONSE

    /**
     * Endpoint to get detailed information about a portfolios
     *
     * @return Success status and detailed portfolio information including holdings
     */
    @PostMapping("/portfolios/details")
    public ResponseEntity<Map<String, Object>> getPortfolioDetails(@RequestBody PortfolioDetailsRequest request) {
        // Call portfolio details service
        PortfolioDetailsResponse portfolioDetailsResponse = portfolioDetailsService.getPortfolioDetails(userContextService.getCurrentUserId().intValue(), request.getPortfolioId());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.details.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolio", portfolioDetailsResponse);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/portfolios/by-platform")
    public ResponseEntity<Map<String, Object>> getPortfoliosByPlatform(@RequestBody PortfolioByPlatformRequest request) {
        // Call portfolio service
        List<PortfolioByPlatformResponse> portfolioByPlatformResponses = portfolioByPlatformService.getPortfoliosByPlatform(userContextService.getCurrentUserId().intValue(), request.getPlatform());

        // 2. Fetch the success message from messages.properties
        String successMessage = messageSource.getMessage(
                "success.message.portfolio.platforms.get", // The key from your properties file
                null,                // Arguments for the message (none in this case)
                LocaleContextHolder.getLocale() // Gets the current request's locale
        );

        // 3. Build the final response body
        Map<String, Object> responseBody = new HashMap<>(); // Use LinkedHashMap to preserve order
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolios", portfolioByPlatformResponses);

        // 4. Return the successful response
        return ResponseEntity.ok(responseBody);
    }
}