package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioRetrievalServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioValueServiceInterface;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PortfolioRetrievalService implements PortfolioRetrievalServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final PortfolioValueServiceInterface portfolioValueService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public PortfolioRetrievalService(
            PortfolioRepository portfolioRepository,
            PortfolioValueServiceInterface portfolioValueService,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.portfolioValueService = portfolioValueService;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.get",
            params = {"#result.size()"}
    )
    @Override
    public List<PortfolioResponse> getPortfolios(Integer userId) {
        long startTime = System.currentTimeMillis();
        System.out.println("[TIMING] getPortfolios started at: " + startTime);

        // Input validation checks
        long validationStart = System.currentTimeMillis();
        if (userId == null) {
            throw new MissingUserIdException();
        }
        System.out.println("[TIMING] Validation completed in: " + (System.currentTimeMillis() - validationStart) + "ms");

        // Database integrity checks
        long userCheckStart = System.currentTimeMillis();
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        System.out.println("[TIMING] User existence check completed in: " + (System.currentTimeMillis() - userCheckStart) + "ms");

        // Data retrieval
        long dataRetrievalStart = System.currentTimeMillis();
        List<Portfolio> portfolios = portfolioRepository.findByUser_UserIdOrderByCreationDateDesc(userId);
        System.out.println("[TIMING] Portfolio data retrieval completed in: " + (System.currentTimeMillis() - dataRetrievalStart) + "ms");
        System.out.println("[TIMING] Found " + portfolios.size() + " portfolios");

        // Response mapping and value calculation
        long mappingStart = System.currentTimeMillis();
        System.out.println("[TIMING] Starting portfolio mapping and value calculation");

        List<PortfolioResponse> result = portfolios.parallelStream()
                .map(portfolio -> {
                    long portfolioStart = System.currentTimeMillis();
                    System.out.println("[TIMING] Processing portfolio: " + portfolio.getPortfolioId() + " - " + portfolio.getPortfolioName());

                    long valueCalcStart = System.currentTimeMillis();
                    PortfolioValueResponse valueResult = portfolioValueService.calculatePortfolioValue(
                            userId,
                            portfolio.getPortfolioId()
                    );
                    System.out.println("[TIMING] calculatePortfolioValue for portfolio " + portfolio.getPortfolioId() + " completed in: " + (System.currentTimeMillis() - valueCalcStart) + "ms");

                    BigDecimal totalValue = valueResult.getTotalValue();

                    PortfolioResponse response = new PortfolioResponse(
                            portfolio.getPortfolioId(),
                            portfolio.getPortfolioName(),
                            portfolio.getApiKey().getPlatform().getPlatformName(),
                            portfolio.getCreationDate().format(DATE_FORMATTER),
                            totalValue
                    );

                    System.out.println("[TIMING] Portfolio " + portfolio.getPortfolioId() + " total processing time: " + (System.currentTimeMillis() - portfolioStart) + "ms");
                    return response;
                })
                .collect(Collectors.toList());

        System.out.println("[TIMING] All portfolio mapping completed in: " + (System.currentTimeMillis() - mappingStart) + "ms");
        System.out.println("[TIMING] getPortfolios TOTAL TIME: " + (System.currentTimeMillis() - startTime) + "ms");

        return result;
    }
}