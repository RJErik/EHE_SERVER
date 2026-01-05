package com.example.ehe_server.service.intf.stock.websocket;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.service.stock.websocket.StockCandleSubscription;

import java.util.List;
import java.util.Optional;

public interface StockCandleProcessingServiceInterface {

    Optional<CandleData> getLatestCandle(StockCandleSubscription subscription);

    CandleUpdateResult checkForUpdates(StockCandleSubscription subscription);

    record CandleUpdateResult(
            boolean hasUpdates,
            List<CandleData> candlesToSend,
            CandleData latestCandle
    ) {}
}