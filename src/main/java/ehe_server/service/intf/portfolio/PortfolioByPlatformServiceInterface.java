package ehe_server.service.intf.portfolio;

import ehe_server.dto.PortfolioByPlatformResponse;

import java.util.List;

public interface PortfolioByPlatformServiceInterface {
    List<PortfolioByPlatformResponse> getPortfoliosByPlatform(Integer userId, String platform);
}
