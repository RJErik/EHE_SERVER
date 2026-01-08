package ehe_server.service.portfolio;

import ehe_server.annotation.LogMessage;
import ehe_server.dto.PortfolioByPlatformResponse;
import ehe_server.entity.Portfolio;
import ehe_server.repository.PortfolioRepository;
import ehe_server.service.intf.portfolio.PortfolioByPlatformServiceInterface;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PortfolioByPlatformService implements PortfolioByPlatformServiceInterface {

    private final PortfolioRepository portfolioRepository;

    public PortfolioByPlatformService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
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

        // Data retrieval
        List<Portfolio> portfolios = portfolioRepository
                .findByUser_UserIdAndApiKey_Platform_PlatformNameIgnoreCaseOrderByCreationDateDesc(userId, platform);

        // Response mapping
        return portfolios.stream()
                .map(p -> new PortfolioByPlatformResponse(p.getPortfolioId(), p.getPortfolioName()))
                .collect(Collectors.toList());
    }
}