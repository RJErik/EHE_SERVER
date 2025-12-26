package com.example.ehe_server.service.stock;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.StocksByPlatformResponse;
import com.example.ehe_server.entity.PlatformStock;
import com.example.ehe_server.exception.custom.MissingPlatformNameException;
import com.example.ehe_server.exception.custom.PlatformNotFoundException;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.stock.StockServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StockService implements StockServiceInterface {

    private final PlatformStockRepository platformStockRepository;

    public StockService(PlatformStockRepository platformStockRepository) {
        this.platformStockRepository = platformStockRepository;
    }

    @LogMessage(
            messageKey = "log.message.stock.stock.get",
            params = {
                    "#platformName",
                    "#result.stocks.size()"
            }
    )
    @Override
    public StocksByPlatformResponse getStocksByPlatform(String platformName) {

        // Input validation checks
        if (platformName == null || platformName.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }

        // Database integrity checks
        if (!platformStockRepository.existsByPlatformPlatformName(platformName)) {
            throw new PlatformNotFoundException(platformName);
        }

        // Data retrieval and response mapping
        List<PlatformStock> platformStocks = platformStockRepository.findByPlatformPlatformNameOrderByStockStockNameAsc(platformName);

        List<String> stocks = platformStocks.stream()
                .map(ps -> ps.getStock().getStockName())
                .collect(Collectors.toList());

        return new StocksByPlatformResponse(platformName, stocks);
    }
}