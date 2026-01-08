package ehe_server.service.stock.websocket;

import ehe_server.dto.websocket.CandleDataResponse.CandleData;
import ehe_server.dto.websocket.CandleUpdateMessage;
import ehe_server.service.intf.log.LoggingServiceInterface;
import ehe_server.service.intf.stock.websocket.StockCandleNotificationServiceInterface;
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

        logCandlesSent("INITIAL", subscription, List.of(candle));
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

        logCandlesSent("UPDATE", subscription, candles);
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

    private void logCandlesSent(String type, StockCandleSubscription subscription, List<CandleData> candles) {
        System.out.printf(">>> SENT %d %s candle(s) for subscription %s (%s:%s %s) <<<%n",
                candles.size(),
                type,
                subscription.getId(),
                subscription.getPlatformName(),
                subscription.getStockSymbol(),
                subscription.getTimeframe());

        candles.forEach(candle -> System.out.printf(
                "  Candle: %s | Seq:%s | O:%s H:%s L:%s C:%s V:%s%n",
                candle.getTimestamp(),
                candle.getSequence(),
                candle.getOpenPrice(),
                candle.getHighPrice(),
                candle.getLowPrice(),
                candle.getClosePrice(),
                candle.getVolume()));

        loggingService.logAction(String.format(
                "Sent %d %s candle(s) for subscription %s",
                candles.size(),
                type,
                subscription.getId()));
    }
}