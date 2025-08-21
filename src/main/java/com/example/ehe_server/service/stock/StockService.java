package com.example.ehe_server.service.stock;

import com.example.ehe_server.dto.StocksByPlatformResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.PlatformNotFoundException;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public StocksByPlatformResponse getStocksByPlatform(String platformName) {
        // Check if platform exists
        if (!platformStockRepository.existsByPlatformName(platformName)) {
            throw new PlatformNotFoundException(platformName);
        }

        // Get all stocks for the platform
        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformName(platformName);

        // Extract stock symbols
        List<String> stocks = platformStocks.stream()
                .map(PlatformStock::getStockSymbol)
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Stocks retrieved successfully for platform: " + platformName);

        return new StocksByPlatformResponse(platformName, stocks);
    }
}
