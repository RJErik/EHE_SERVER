package com.example.ehe_server.service.intf.trade;

import java.util.Map;

public interface TradingCapacityServiceInterface {
    Map<String, Object> getTradingCapacity(Integer portfolioId, String stockSymbol);
}
