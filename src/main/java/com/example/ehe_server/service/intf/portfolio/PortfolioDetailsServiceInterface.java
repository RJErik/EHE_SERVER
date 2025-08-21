package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioDetailsResponse;

import java.util.Map;

public interface PortfolioDetailsServiceInterface {
    PortfolioDetailsResponse getPortfolioDetails(Integer userId, Integer portfolioId);
}
