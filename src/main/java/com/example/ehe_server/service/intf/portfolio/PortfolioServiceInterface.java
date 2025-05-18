package com.example.ehe_server.service.intf.portfolio;

import java.util.Map;

public interface PortfolioServiceInterface {
    Map<String, Object> createPortfolio(String portfolioName, Integer apiKeyId);
    Map<String, Object> getPortfolios();
    Map<String, Object> deletePortfolio(Integer portfolioId);
}
