package com.example.ehe_server.service.intf.portfolio;

import com.example.ehe_server.dto.HoldingsUpdateResponse;
import com.example.ehe_server.entity.Portfolio;

public interface HoldingsSyncServiceInterface {

    HoldingsUpdateResponse syncHoldings(Integer userId, Integer portfolioId);

    HoldingsUpdateResponse syncHoldings(Integer userId, Portfolio portfolio);
}