package ehe_server.service.portfolio;

import ehe_server.annotation.LogMessage;
import ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.PortfolioNotFoundException;
import ehe_server.exception.custom.UnauthorizedPortfolioAccessException;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.portfolio.PortfolioRemovalServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PortfolioRemovalService implements PortfolioRemovalServiceInterface {

    private final PortfolioRepository portfolioRepository;

    public PortfolioRemovalService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.remove",
            params = {"#portfolioId"}
    )
    @Override
    public void removePortfolio(Integer userId, Integer portfolioId) {

        // Portfolio existence check
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

        // Ownership verification
        if (!portfolio.getUser().getUserId().equals(userId)) {
            throw new UnauthorizedPortfolioAccessException(userId, portfolioId);
        }

        // Execute removal
        portfolioRepository.delete(portfolio);
    }
}