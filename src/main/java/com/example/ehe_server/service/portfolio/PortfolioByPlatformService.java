package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.PortfolioByPlatformResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioByPlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioByPlatformService implements PortfolioByPlatformServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final UserRepository userRepository;

    public PortfolioByPlatformService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.userRepository = userRepository;
    }


    @Override
    public List<PortfolioByPlatformResponse> getPortfoliosByPlatform(Integer userId, String platform) {
        // Get all portfolios for the user
        User user;
        List<Portfolio> allPortfolios;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
            allPortfolios = portfolioRepository.findByUser(user);
        } else {
            return null;
        }

        // Transform to response format (only id and name as requested)
        // Filter portfolios by platform and map to DTOs
        List<PortfolioByPlatformResponse> filteredPortfolios = allPortfolios.stream()
                .filter(p -> p.getApiKey() != null && p.getApiKey().getPlatformName().equalsIgnoreCase(platform))
                .map(p -> new PortfolioByPlatformResponse(p.getPortfolioId(), p.getPortfolioName()))
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Retrieved " + filteredPortfolios.size() + " portfolios for platform: " + platform);

        return filteredPortfolios;
    }
}
