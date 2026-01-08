package ehe_server.controller;

import ehe_server.dto.HomeLatestTransactionsResponse;
import ehe_server.dto.HomeStockResponse;
import ehe_server.service.intf.home.HomeBestStockServiceInterface;
import ehe_server.service.intf.home.HomeLatestTransactionsServiceInterface;
import ehe_server.service.intf.home.HomeWorstStockServiceInterface;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/home")
public class HomeInfoController {

    private final MessageSource messageSource;
    private final HomeBestStockServiceInterface homeBestStockService;
    private final HomeWorstStockServiceInterface homeWorstStockService;
    private final HomeLatestTransactionsServiceInterface homeLatestTransactionsService;

    public HomeInfoController(MessageSource messageSource,
                              HomeBestStockServiceInterface homeBestStockService,
                              HomeWorstStockServiceInterface homeWorstStockService,
                              HomeLatestTransactionsServiceInterface homeLatestTransactionsService) {
        this.messageSource = messageSource;
        this.homeBestStockService = homeBestStockService;
        this.homeWorstStockService = homeWorstStockService;
        this.homeLatestTransactionsService = homeLatestTransactionsService;
    }

    /**
     * GET /api/home/best-stocks
     * Retrieve top performing stocks for home page display
     */
    @GetMapping("/best-stocks")
    public ResponseEntity<Map<String, Object>> getBestStocks() {
        List<HomeStockResponse> bestStocks = homeBestStockService.getHomeBestStock();

        String successMessage = messageSource.getMessage(
                "success.message.home.bestStock",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("bestStocks", bestStocks);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/home/worst-stocks
     * Retrieve worst performing stocks for home page display
     */
    @GetMapping("/worst-stocks")
    public ResponseEntity<Map<String, Object>> getWorstStocks() {
        List<HomeStockResponse> worstStocks = homeWorstStockService.getHomeWorstStock();

        String successMessage = messageSource.getMessage(
                "success.message.home.worstStock",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("worstStocks", worstStocks);

        return ResponseEntity.ok(responseBody);
    }

    /**
     * GET /api/home/latest-transactions
     * Retrieve most recent transactions for home page display
     */
    @GetMapping("/latest-transactions")
    public ResponseEntity<Map<String, Object>> getLatestTransactions() {
        List<HomeLatestTransactionsResponse> latestTransactions =
                homeLatestTransactionsService.getLatestTransactions();

        String successMessage = messageSource.getMessage(
                "success.message.home.latestTransaction",
                null,
                LocaleContextHolder.getLocale()
        );

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("success", true);
        responseBody.put("message", successMessage);
        responseBody.put("latestTransactions", latestTransactions);

        return ResponseEntity.ok(responseBody);
    }
}