package com.example.ehe_server.service.portfolio;

import com.example.ehe_server.annotation.LogMessage;
import com.example.ehe_server.dto.PortfolioByPlatformResponse;
import com.example.ehe_server.entity.Portfolio;
import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.PortfolioRepository;
import com.example.ehe_server.repository.UserRepository;
import com.example.ehe_server.service.intf.portfolio.PortfolioByPlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
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
                    "#result.platform",
                    "#result.size()",
            }
    )
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
        return allPortfolios.stream()
                .filter(p -> p.getApiKey() != null && p.getApiKey().getPlatformName().equalsIgnoreCase(platform))
                .map(p -> new PortfolioByPlatformResponse(p.getPortfolioId(), p.getPortfolioName()))
                .collect(Collectors.toList());
    }
}
