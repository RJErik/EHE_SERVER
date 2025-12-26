package com.example.ehe_server.controller;

import com.example.ehe_server.dto.*;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.portfolio.*;
import com.example.ehe_server.service.portfolio.PortfolioByPlatformService;
import com.example.ehe_server.service.portfolio.PortfolioDetailsService;
import com.example.ehe_server.service.portfolio.PortfolioValueService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/portfolios")
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
     * GET /api/user/portfolios
     * Retrieve all portfolios for the current user
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPortfolios() {
        List<PortfolioResponse> portfolioRetrievalResponses =
                portfolioRetrievalService.getPortfolios(userContextService.getCurrentUserId());

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolios", portfolioRetrievalResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * POST /api/user/portfolios
     * Create a new portfolio
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createPortfolio(
            @Valid @RequestBody PortfolioCreationRequest request) {

        PortfolioResponse portfolioCreationResponse = portfolioCreationService.createPortfolio(
                userContextService.getCurrentUserId(),
                request.getPortfolioName(),
                request.getApiKeyId());

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.add",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolio", portfolioCreationResponse);

        // 201 Created for resource creation
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    /**
     * DELETE /api/user/portfolios/{portfolioId}
     * Delete a portfolio by ID
     */
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Map<String, Object>> removePortfolio(@PathVariable Integer portfolioId) {
        portfolioRemovalService.removePortfolio(userContextService.getCurrentUserId(), portfolioId);

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.remove",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/portfolios/search?platform=X&minValue=100&maxValue=1000
     * Search portfolios with filters
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchPortfolio(@ModelAttribute PortfolioSearchRequest request) {
        List<PortfolioResponse> portfolioSearchResponses = portfolioSearchService.searchPortfolios(
                userContextService.getCurrentUserId(),
                request.getPlatform(),
                request.getMinValue(),
                request.getMaxValue());

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.search",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolios", portfolioSearchResponses);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * PUT /api/user/portfolios/{portfolioId}/holdings
     * Update holdings for a specific portfolio (sync with exchange)
     */
    @PutMapping("/{portfolioId}/holdings")
    public ResponseEntity<Map<String, Object>> updateHoldings(@PathVariable Integer portfolioId) {
        HoldingsUpdateResponse holdingsUpdateResponse =
                portfolioValueService.updateHoldings(userContextService.getCurrentUserId(), portfolioId);

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.holdings.updated",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("holdings", holdingsUpdateResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/portfolios/{portfolioId}/value
     * Get the calculated value of a portfolio
     */
    @GetMapping("/{portfolioId}/value")
    public ResponseEntity<Map<String, Object>> getPortfolioValue(@PathVariable Integer portfolioId) {
        PortfolioValueResponse portfolioValueResponse =
                portfolioValueService.calculatePortfolioValue(userContextService.getCurrentUserId(), portfolioId);

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.value.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolioValue", portfolioValueResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/portfolios/{portfolioId}/details
     * Get detailed information about a portfolio including holdings
     */
    @GetMapping("/{portfolioId}/details")
    public ResponseEntity<Map<String, Object>> getPortfolioDetails(@PathVariable Integer portfolioId) {
        PortfolioDetailsResponse portfolioDetailsResponse =
                portfolioDetailsService.getPortfolioDetails(userContextService.getCurrentUserId(), portfolioId);

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.details.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolio", portfolioDetailsResponse);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/user/portfolios/by-platform?platform=BINANCE
     * Get portfolios filtered by platform
     */
    @GetMapping("/by-platform")
    public ResponseEntity<Map<String, Object>> getPortfoliosByPlatform(
            @RequestParam String platform) {

        List<PortfolioByPlatformResponse> portfolioByPlatformResponses =
                portfolioByPlatformService.getPortfoliosByPlatform(
                        userContextService.getCurrentUserId(),
                        platform);

        String successMessage = messageSource.getMessage(
                "success.message.portfolio.platforms.get",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("portfolios", portfolioByPlatformResponses);

        return ResponseEntity.ok(responseBody);
    }
}