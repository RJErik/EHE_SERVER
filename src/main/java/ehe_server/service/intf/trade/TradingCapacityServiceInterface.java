package ehe_server.service.intf.trade;

import ehe_server.dto.TradingCapacityResponse;

public interface TradingCapacityServiceInterface {
    TradingCapacityResponse getTradingCapacity(Integer userId, Integer portfolioId, String stockSymbol);
}
