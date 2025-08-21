package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioByPlatformResponse;

import java.util.List;
import java.util.Map;

public interface PortfolioByPlatformServiceInterface {

    /**
     * Retrieve portfolios by platform name for the current user
     * @param userId The user that has initiated the service
     * @param platform The name of the platform to filter by
     * @return Map containing success status and list of portfolios from the specified platform
     */
    List<PortfolioByPlatformResponse> getPortfoliosByPlatform(Integer userId, String platform);
}
