package com.example.ehe_server.service.intf.trade;

import com.example.ehe_server.dto.TradingCapacityResponse;

import java.util.Map;

public interface TradingCapacityServiceInterface {
    TradingCapacityResponse getTradingCapacity(Integer userId, Integer portfolioId, String stockSymbol);
}
