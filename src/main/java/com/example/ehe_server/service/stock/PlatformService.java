package com.example.ehe_server.service.stock;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.repository.PlatformStockRepository;
import com.example.ehe_server.service.intf.stock.PlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PlatformService implements PlatformServiceInterface {

    private final PlatformStockRepository platformStockRepository;

    public PlatformService(PlatformStockRepository platformStockRepository) {
        this.platformStockRepository = platformStockRepository;
    }

    @LogMessage(
            messageKey = "log.message.stock.platform.get",
            params = {"#result.size()"}
    )
    @Override
    public List<String> getAllPlatforms() {
        // Extract unique platform names
        return platformStockRepository.findDistinctPlatformNames();

    }
}
