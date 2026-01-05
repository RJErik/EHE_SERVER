package com.example.ehe_server.service.intf.stock.websocket;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.service.stock.websocket.StockCandleSubscription;

import java.time.LocalDateTime;
import java.util.List;

public interface StockCandleNotificationServiceInterface {

    void sendInitialCandle(StockCandleSubscription subscription, CandleData candle);

    void sendUpdate(StockCandleSubscription subscription, List<CandleData> candles, LocalDateTime timestamp);

    void sendHeartbeat(StockCandleSubscription subscription, LocalDateTime timestamp);
}