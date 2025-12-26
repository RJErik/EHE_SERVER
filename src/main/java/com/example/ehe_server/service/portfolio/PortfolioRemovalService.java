package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.*;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class PortfolioRemovalService implements PortfolioRemovalServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    public PortfolioRemovalService(PortfolioRepository portfolioRepository,
                                   UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.remove",
            params = {"#portfolioId"}
    )
    @Override
    public void removePortfolio(Integer userId, Integer portfolioId) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (portfolioId == null) {
            throw new MissingPortfolioIdException();
        }

        // User validation
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Portfolio existence check
        Optional<Portfolio> portfolioOptional = portfolioRepository.findById(portfolioId);
        if (portfolioOptional.isEmpty()) {
            throw new PortfolioNotFoundException(portfolioId); // Fixed: logic required portfolioId here
        }

        Portfolio portfolio = portfolioOptional.get();

        // Ownership verification
        if (!portfolio.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedPortfolioAccessException(userId, portfolioId);
        }

        // Execute removal
        portfolioRepository.delete(portfolio);
    }
}