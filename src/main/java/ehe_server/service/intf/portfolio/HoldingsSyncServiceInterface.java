package ehe_server.service.intf.portfolio;

import ehe_server.dto.HoldingsUpdateResponse;
import ehe_server.entity.Portfolio;

public interface HoldingsSyncServiceInterface {

    HoldingsUpdateResponse syncHoldings(Integer userId, Integer portfolioId);

    HoldingsUpdateResponse syncHoldings(Integer userId, Portfolio portfolio);
}