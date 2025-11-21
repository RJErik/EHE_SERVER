package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioSearchResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.InvalidUserIdentifierException;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
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
    private final PortfolioValueServiceInterface portfolioValueService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final UserRepository userRepository;

    public PortfolioSearchService(
            PortfolioRepository portfolioRepository,
            PortfolioValueServiceInterface portfolioValueService,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueService = portfolioValueService;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.search",
            params = {
                    "#platform",
                    "#minValue",
                    "#maxValue",
                    "#result.size()"}
    )
    @Override
    public List<PortfolioSearchResponse> searchPortfolios(Integer userId, String platform,
                                                          BigDecimal minValue, BigDecimal maxValue) {

        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new InvalidUserIdentifierException("TODO");
        }

        // Get filtered portfolios using single query
        List<Portfolio> portfolios = portfolioRepository.searchPortfolios(
                userId,
                (platform != null && !platform.trim().isEmpty()) ? platform : null
        );

        // Calculate values and filter by value range
        return portfolios.stream()
                .map(portfolio -> {
                    PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(
                            userId,
                            portfolio.getPortfolioId()
                    );
                    BigDecimal value = valueResult.getTotalValue();

                    return new PortfolioSearchResponse(
                            portfolio.getPortfolioId(),
                            portfolio.getPortfolioName(),
                            portfolio.getApiKey() != null ? portfolio.getApiKey().getPlatformName() : null,
                            portfolio.getCreationDate().format(DATE_FORMATTER),
                            value
                    );
                })
                .filter(p -> (minValue == null || p.getValue().compareTo(minValue) >= 0) &&
                        (maxValue == null || p.getValue().compareTo(maxValue) <= 0))
                .collect(Collectors.toList());
    }
}