package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioByPlatformResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.exception.custom.MissingPlatformNameException;
import com.example.ehe_server.exception.custom.MissingUserIdException;
import com.example.ehe_server.exception.custom.UserNotFoundException;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioByPlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PortfolioByPlatformService implements PortfolioByPlatformServiceInterface {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;

    public PortfolioByPlatformService(
            PortfolioRepository portfolioRepository,
            UserRepository userRepository) {
        this.portfolioRepository = portfolioRepository;
        this.userRepository = userRepository;
    }

    @LogMessage(
            messageKey = "log.message.portfolio.platforms.get",
            params = {
                    "#platform",
                    "#result.size()",
            }
    )
    @Override
    public List<PortfolioByPlatformResponse> getPortfoliosByPlatform(Integer userId, String platform) {

        // Input validation checks
        if (userId == null) {
            throw new MissingUserIdException();
        }

        if (platform == null || platform.trim().isEmpty()) {
            throw new MissingPlatformNameException();
        }

        // Database integrity checks
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        // Data retrieval
        List<Portfolio> portfolios = portfolioRepository
                .findByUser_UserIdAndApiKey_PlatformNameIgnoreCaseOrderByCreationDateDesc(userId, platform);

        // Response mapping
        return portfolios.stream()
                .map(p -> new PortfolioByPlatformResponse(p.getPortfolioId(), p.getPortfolioName()))
                .collect(Collectors.toList());
    }
}