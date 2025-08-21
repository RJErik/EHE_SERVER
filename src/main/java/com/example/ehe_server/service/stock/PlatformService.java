package com.example.ehe_server.service.stock;

import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    public List<String> getAllPlatforms() {
        // Get all platform stock records
        List<PlatformStock> platformStocks = platformStockRepository.findAll();

        // Extract unique platform names
        List<String> platforms = platformStocks.stream()
                .map(PlatformStock::getPlatformName)
                .distinct()
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Platforms retrieved successfully");

        return platforms;

    }
}
