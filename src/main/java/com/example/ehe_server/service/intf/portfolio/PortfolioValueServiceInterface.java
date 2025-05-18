package com.example.ehe_server.service.intf.portfolio;

import java.util.Map;

public interface PortfolioValueServiceInterface {
    Map<String, Object> calculatePortfolioValue(Integer portfolioId);
    Map<String, Object> updateHoldings(Integer portfolioId);
}
