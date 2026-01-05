package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioByPlatformResponse;

import java.util.List;
import java.util.Map;

public interface PortfolioByPlatformServiceInterface {
    List<PortfolioByPlatformResponse> getPortfoliosByPlatform(Integer userId, String platform);
}
