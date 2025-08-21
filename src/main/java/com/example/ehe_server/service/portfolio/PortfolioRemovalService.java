package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.entity.Holding;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.PortfolioNotFoundException;
import com.example.ehe_server.repository.HoldingRepository;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.portfolio.PortfolioRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class PortfolioRemovalService implements PortfolioRemovalServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;

    public PortfolioRemovalService(
            PortfolioRepository portfolioRepository,
            HoldingRepository holdingRepository,
            LoggingServiceInterface loggingService,
            UserContextService userContextService) {
        this.portfolioRepository = portfolioRepository;
        this.holdingRepository = holdingRepository;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
    }

    @Override
    @Transactional
    public void removePortfolio(Integer portfolioId) {
        // Get current user ID from user context
        Integer userId = Integer.parseInt(userContextService.getCurrentUserIdAsString());

        // Check if portfolio exists and belongs to the user
        Optional<Portfolio> portfolioOptional = portfolioRepository.findByPortfolioIdAndUser_UserId(portfolioId, userId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId, userId);
        }

        Portfolio portfolio = portfolioOptional.get();

        // First delete all holdings associated with this portfolio
        List<Holding> holdings = holdingRepository.findByPortfolio(portfolio);
        holdingRepository.deleteAll(holdings);

        // Now delete the portfolio
        portfolioRepository.delete(portfolio);

        // Log success
        loggingService.logAction("Deleted portfolio: " + portfolio.getPortfolioName() + " and " + holdings.size() + " holdings");
    }
}