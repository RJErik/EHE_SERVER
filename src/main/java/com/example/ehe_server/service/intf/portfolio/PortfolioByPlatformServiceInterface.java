package com.example.ehe_server.service.intf.portfolio;

import java.util.Map;

public interface PortfolioByPlatformServiceInterface {

    /**
     * Retrieve portfolios by platform name for the current user
     *
     * @param platform The name of the platform to filter by
     * @return Map containing success status and list of portfolios from the specified platform
     */
    Map<String, Object> getPortfoliosByPlatform(String platform);
}
