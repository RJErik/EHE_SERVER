package com.example.ehe_server.service.intf.portfolio;

import java.util.Map;

public interface PortfolioDetailsServiceInterface {
    Map<String, Object> getPortfolioDetails(Integer portfolioId);
}
