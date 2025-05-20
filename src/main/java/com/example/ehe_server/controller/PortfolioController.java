package com.example.ehe_server.controller;

import com.example.ehe_server.dto.PortfolioByPlatformRequest;
import com.example.ehe_server.dto.PortfolioCreateRequest;
import com.example.ehe_server.dto.PortfolioDeleteRequest;
import com.example.ehe_server.dto.PortfolioSearchRequest;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import com.example.ehe_server.service.intf.portfolio.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class PortfolioController {

    private final PortfolioServiceInterface portfolioService;
    private final PortfolioSearchServiceInterface portfolioSearchService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private final PortfolioDetailsServiceInterface portfolioDetailsService;
    private final UserContextServiceInterface userContextService;
    private final PortfolioByPlatformServiceInterface portfolioByPlatformServiceInterface;

    public PortfolioController(
            PortfolioServiceInterface portfolioService,
            PortfolioSearchServiceInterface portfolioSearchService,
            PortfolioValueServiceInterface portfolioValueService,
            PortfolioDetailsServiceInterface portfolioDetailsService,
            UserContextServiceInterface userContextService,
            PortfolioByPlatformServiceInterface portfolioByPlatformServiceInterface) {
        this.portfolioService = portfolioService;
        this.portfolioSearchService = portfolioSearchService;
        this.portfolioValueService = portfolioValueService;
        this.portfolioDetailsService = portfolioDetailsService;
        this.userContextService = userContextService;
        this.portfolioByPlatformServiceInterface = portfolioByPlatformServiceInterface;
    }

    /**
     * Endpoint to create a new portfolio
     *
     * @param request Contains portfolio name and API key ID
     * @return Success status and details of created portfolio
     */
    @PostMapping("/portfolio/create")
    public ResponseEntity<Map<String, Object>> createPortfolio(@RequestBody PortfolioCreateRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio service to create portfolio
        Map<String, Object> responseBody = portfolioService.createPortfolio(
                request.getPortfolioName(), request.getApiKeyId());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to retrieve all portfolios for the current user
     *
     * @return List of portfolios and success status
     */
    @GetMapping("/portfolio")
    public ResponseEntity<Map<String, Object>> getPortfolios() {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio service
        Map<String, Object> responseBody = portfolioService.getPortfolios();

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to delete a portfolio and its holdings
     *
     * @param request Contains the portfolio ID to delete
     * @return Success status and confirmation message
     */
    @DeleteMapping("/portfolio/delete")
    public ResponseEntity<Map<String, Object>> deletePortfolio(@RequestBody PortfolioDeleteRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio service to delete portfolio
        Map<String, Object> responseBody = portfolioService.deletePortfolio(request.getPortfolioId());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to search portfolios by various criteria
     *
     * @param request Contains search criteria (type, platform, min/max value)
     * @return Filtered list of portfolios and success status
     */
    @PostMapping("/portfolio/search")
    public ResponseEntity<Map<String, Object>> searchPortfolios(@RequestBody PortfolioSearchRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio search service
        Map<String, Object> responseBody = portfolioSearchService.searchPortfolios(
                request.getType(), request.getPlatform(), request.getMinValue(), request.getMaxValue());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to update portfolio holdings from the exchange
     *
     * @param portfolioId ID of the portfolio to update
     * @return Success status and details of updated holdings
     */
    @PostMapping("/portfolio/{portfolioId}/update-holdings")
    public ResponseEntity<Map<String, Object>> updateHoldings(@PathVariable Integer portfolioId) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio value service to update holdings
        Map<String, Object> responseBody = portfolioValueService.updateHoldings(portfolioId);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to get the current value of a portfolio
     *
     * @param portfolioId ID of the portfolio to value
     * @return Success status and portfolio value details
     */
    @GetMapping("/portfolio/{portfolioId}/value")
    public ResponseEntity<Map<String, Object>> getPortfolioValue(@PathVariable Integer portfolioId) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio value service to calculate value
        Map<String, Object> responseBody = portfolioValueService.calculatePortfolioValue(portfolioId);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    /**
     * Endpoint to get detailed information about a portfolio
     *
     * @param portfolioId ID of the portfolio to get details for
     * @return Success status and detailed portfolio information including holdings
     */
    @GetMapping("/portfolio/{portfolioId}/details")
    public ResponseEntity<Map<String, Object>> getPortfolioDetails(@PathVariable Integer portfolioId) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio details service
        Map<String, Object> responseBody = portfolioDetailsService.getPortfolioDetails(portfolioId);

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }

    @PostMapping("/portfolio/by-platform")
    public ResponseEntity<Map<String, Object>> getPortfoliosByPlatform(@RequestBody PortfolioByPlatformRequest request) {
        // Setup the user context from Spring Security
        userContextService.setupUserContext();

        // Call portfolio service
        Map<String, Object> responseBody = portfolioByPlatformServiceInterface.getPortfoliosByPlatform(request.getPlatform());

        // Return appropriate response
        boolean success = (boolean) responseBody.getOrDefault("success", false);
        return success ? ResponseEntity.ok(responseBody) : ResponseEntity.badRequest().body(responseBody);
    }
}
