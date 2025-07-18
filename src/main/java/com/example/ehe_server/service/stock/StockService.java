package com.example.ehe_server.service.stock;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockService implements StockServiceInterface {

    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;

    public StockService(
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService) {
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
    }

    @Override
    public Map<String, Object> getStocksByPlatform(String platformName) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Check if platform exists
            if (!platformStockRepository.existsByPlatformName(platformName)) {
                result.put("success", false);
                result.put("message", "Platform not found");
                loggingService.logAction("Stock retrieval failed: Platform not found, platform=" + platformName);
                return result;
            }

            // Get all stocks for the platform
            List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);

            // Extract stock symbols
            List<String> stocks = platformStocks.stream()
                    .map(PlatformStock::getStockSymbol)
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("platform", platformName);
            result.put("stocks", stocks);

            // Log success
            loggingService.logAction("Stocks retrieved successfully for platform: " + platformName);

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving stocks: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving stocks");
        }

        return result;
    }
}
