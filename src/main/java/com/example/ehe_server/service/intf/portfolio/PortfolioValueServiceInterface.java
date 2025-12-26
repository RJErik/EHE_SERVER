package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.HoldingsUpdateResponse;
import com.example.ehe_server.dto.PortfolioValueResponse;
import com.example.ehe_server.entity.Portfolio;

public interface PortfolioValueServiceInterface {
    PortfolioValueResponse calculatePortfolioValue(Integer userId, Integer portfolioId);
    HoldingsUpdateResponse updateHoldings(Integer userId, Portfolio portfolio);
    HoldingsUpdateResponse updateHoldings(Integer userId, Integer portfolioId);
}
