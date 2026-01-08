package ehe_server.service.intf.portfolio;

import ehe_server.dto.PortfolioValueResponse;

public interface PortfolioValueServiceInterface {

    PortfolioValueResponse calculatePortfolioValue(Integer userId, Integer portfolioId);
}