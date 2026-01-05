package com.example.ehe_server.service.stock.websocket;

import com.example.ehe_server.dto.websocket.CandleDataResponse.CandleData;
import com.example.ehe_server.dto.websocket.CandleUpdateMessage;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import com.example.ehe_server.service.intf.stock.websocket.StockCandleNotificationServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockCandleNotificationService implements StockCandleNotificationServiceInterface {

    private final SimpMessagingTemplate messagingTemplate;
    private final LoggingServiceInterface loggingService;

    public StockCandleNotificationService(
            SimpMessagingTemplate messagingTemplate,
            LoggingServiceInterface loggingService) {
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
    }

    @Override
    public void sendInitialCandle(StockCandleSubscription subscription, CandleData candle) {
        CandleUpdateMessage message = buildMessage(
                subscription.getId(),
                "INITIAL",
                List.of(candle),
                LocalDateTime.now());

        messagingTemplate.convertAndSend(subscription.getDestination(), message);

        logCandleSent("initial", subscription, candle);
        loggingService.logAction(String.format(
                "Sent initial candle for subscription: %s (%s:%s %s)",
                subscription.getId(),
                subscription.getPlatformName(),
                subscription.getStockSymbol(),
                subscription.getTimeframe()));
    }

    @Override
    public void sendUpdate(
            StockCandleSubscription subscription,
            List<CandleData> candles,
            LocalDateTime timestamp) {

        CandleUpdateMessage message = buildMessage(
                subscription.getId(),
                "UPDATE",
                candles,
                timestamp);

        messagingTemplate.convertAndSend(subscription.getDestination(), message);

        System.out.println(">>> SENT " + candles.size() + " candle update(s) for subscription " +
                subscription.getId() + " <<<");
        candles.forEach(candle -> {
                    System.out.printf(
                            "  Candle: %s | Seq:%s | O:%s H:%s L:%s C:%s V:%s%n",
                            candle.getTimestamp(),
                            candle.getSequence(),
                            candle.getOpenPrice(),
                            candle.getHighPrice(),
                            candle.getLowPrice(),
                            candle.getClosePrice(),
                            candle.getVolume());
                });
        loggingService.logAction(String.format(
                "Sent %d candle update(s) for subscription %s",
                candles.size(),
                subscription.getId()));
    }

    @Override
    public void sendHeartbeat(StockCandleSubscription subscription, LocalDateTime timestamp) {
        CandleUpdateMessage message = buildMessage(
                subscription.getId(),
                "HEARTBEAT",
                new ArrayList<>(),
                timestamp);

        messagingTemplate.convertAndSend(subscription.getDestination(), message);

        System.out.println("Sent heartbeat for subscription: " + subscription.getId());
    }

    private CandleUpdateMessage buildMessage(
            String subscriptionId,
            String updateType,
            List<CandleData> candles,
            LocalDateTime timestamp) {

        CandleUpdateMessage message = new CandleUpdateMessage();
        message.setSubscriptionId(subscriptionId);
        message.setUpdateType(updateType);
        message.setUpdatedCandles(candles);
        message.setUpdateTimestamp(timestamp);
        return message;
    }

    private void logCandleSent(String type, StockCandleSubscription subscription, CandleData candle) {
        System.out.printf(
                "Sent %s candle: %s | Seq:%s | O:%s H:%s L:%s C:%s V:%s%n",
                type,
                candle.getTimestamp(),
                candle.getSequence(),
                candle.getOpenPrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getClosePrice(),
                candle.getVolume());
    }

    private void logUpdatesSent(StockCandleSubscription subscription, List<CandleData> candles) {
        System.out.println(">>> SENT " + candles.size() + " candle update(s) for subscription " +
                subscription.getId() + " <<<");
        candles.forEach(candle -> {
            System.out.printf(
                    "  Candle: %s | Seq:%s | O:%s H:%s L:%s C:%s V:%s%n",
                    candle.getTimestamp(),
                    candle.getSequence(),
                    candle.getOpenPrice(),
                    candle.getHighPrice(),
                    candle.getLowPrice(),
                    candle.getClosePrice(),
                    candle.getVolume());
        });
    }
}