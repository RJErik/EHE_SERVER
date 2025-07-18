package com.example.ehe_server.service.stock;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlatformService implements PlatformServiceInterface {

    private final PlatformStockRepository platformStockRepository;
    private final LoggingServiceInterface loggingService;

    public PlatformService(
            PlatformStockRepository platformStockRepository,
            LoggingServiceInterface loggingService) {
        this.platformStockRepository = platformStockRepository;
        this.loggingService = loggingService;
    }

    @Override
    public Map<String, Object> getAllPlatforms() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get all platform stock records
            List<PlatformStock> platformStocks = platformStockRepository.findAll();

            // Extract unique platform names
            List<String> platforms = platformStocks.stream()
                    .map(PlatformStock::getPlatformName)
                    .distinct()
                    .collect(Collectors.toList());

            // Prepare success response
            result.put("success", true);
            result.put("platforms", platforms);

            // Log success
            loggingService.logAction("Platforms retrieved successfully");

        } catch (Exception e) {
            // Log error
            loggingService.logError("Error retrieving platforms: " + e.getMessage(), e);

            // Return error response
            result.put("success", false);
            result.put("message", "An error occurred while retrieving platforms");
        }

        return result;
    }
}
