package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.PortfolioValueResponse;

public interface PortfolioValueServiceInterface {

    PortfolioValueResponse calculatePortfolioValue(Integer userId, Integer portfolioId);
}