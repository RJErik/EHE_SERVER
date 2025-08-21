package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.dto.PortfolioSearchResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.Portfolio.PortfolioType;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioSearchServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PortfolioSearchService implements PortfolioSearchServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final LoggingServiceInterface loggingService;
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public PortfolioSearchService(
            PortfolioRepository portfolioRepository,
            LoggingServiceInterface loggingService,
            PortfolioValueServiceInterface portfolioValueService,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.loggingService = loggingService;
        this.portfolioValueService = portfolioValueService;
        this.userRepository = userRepository;
    }

    @Override
    public List<PortfolioSearchResponse> searchPortfolios(Integer userId, PortfolioType type, String platform,
                                                          BigDecimal minValue, BigDecimal maxValue) {

        // Get all user's portfolios
        User user;
        List<Portfolio> allPortfolios;
        if (userRepository.existsById(userId)) {
            user = userRepository.findById(userId).get();
            allPortfolios = portfolioRepository.findByUser(user);
        } else {
            return null;
        }

        // Filter, calculate value, and map to DTOs in a single stream pipeline
        List<PortfolioSearchResponse> portfoliosList = allPortfolios.stream()
                .filter(p -> type == null || p.getPortfolioType() == type)
                .filter(p -> platform == null || (p.getApiKey() != null && p.getApiKey().getPlatformName().equals(platform)))
                .map(portfolio -> {
                    // Assuming a refactored portfolioValueService returns a DTO or BigDecimal directly
                    // For now, we'll extract from the map, but this is a nested code smell
                    PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(userId, portfolio.getPortfolioId());
                    BigDecimal value = valueResult.getTotalValue();

                    return new PortfolioSearchResponse(
                            portfolio.getPortfolioId(),
                            portfolio.getPortfolioName(),
                            portfolio.getApiKey() != null ? portfolio.getApiKey().getPlatformName() : null,
                            portfolio.getPortfolioType().toString(),
                            portfolio.getCreationDate().format(DATE_FORMATTER),
                            value
                    );
                })
                .filter(p -> (minValue == null || p.getValue().compareTo(minValue) >= 0) &&
                        (maxValue == null || p.getValue().compareTo(maxValue) <= 0))
                .collect(Collectors.toList());

        // Log success
        loggingService.logAction("Searched portfolios with criteria - type: " + type +
                ", platform: " + platform +
                ", minValue: " + minValue +
                ", maxValue: " + maxValue +
                ". Found " + portfoliosList.size() + " results.");

        return portfoliosList;
    }
}
