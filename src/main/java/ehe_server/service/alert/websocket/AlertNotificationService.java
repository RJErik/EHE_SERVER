package ehe_server.service.alert.websocket;

import ehe_server.dto.websocket.AlertNotificationResponse;
import ehe_server.entity.Alert;
import ehe_server.entity.MarketCandle;
import ehe_server.service.intf.alert.websocket.AlertNotificationServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AlertNotificationService implements AlertNotificationServiceInterface {

    private static final String ALERTS_QUEUE = "/queue/alerts";

    private final SimpMessagingTemplate messagingTemplate;

    public AlertNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendTriggeredNotification(Alert alert, MarketCandle triggeringCandle, AlertSubscription subscription) {
        AlertNotificationResponse notification = buildNotification(alert, triggeringCandle, subscription);

        messagingTemplate.convertAndSendToUser(
                subscription.getUserId().toString(),
                ALERTS_QUEUE,
                notification
        );
    }

    private AlertNotificationResponse buildNotification(
            Alert alert,
            MarketCandle candle,
            AlertSubscription subscription) {

        BigDecimal triggerPrice = getRelevantPrice(alert, candle);
        String platformName = alert.getPlatformStock().getPlatform().getPlatformName();
        String stockSymbol = alert.getPlatformStock().getStock().getStockSymbol();

        AlertNotificationResponse notification = new AlertNotificationResponse();
        notification.setSuccess(true);
        notification.setAlertId(alert.getAlertId());
        notification.setPlatformName(platformName);
        notification.setStockSymbol(stockSymbol);
        notification.setConditionType(alert.getConditionType().toString());
        notification.setThresholdValue(alert.getThresholdValue());
        notification.setSubscriptionId(subscription.getId());
        notification.setTriggerTime(LocalDateTime.now());
        notification.setCurrentPrice(triggerPrice);
        notification.setMessage(formatMessage(alert, candle, triggerPrice));

        return notification;
    }

    private BigDecimal getRelevantPrice(Alert alert, MarketCandle candle) {
        return switch (alert.getConditionType()) {
            case PRICE_ABOVE -> candle.getHighPrice();
            case PRICE_BELOW -> candle.getLowPrice();
        };
    }

    private String formatMessage(Alert alert, MarketCandle candle, BigDecimal triggerPrice) {
        String action = alert.getConditionType() == Alert.ConditionType.PRICE_ABOVE
                ? "crossed above"
                : "dropped below";
        String symbol = alert.getPlatformStock().getStock().getStockSymbol();
        String platform = alert.getPlatformStock().getPlatform().getPlatformName();

        return String.format("ALERT TRIGGERED: %s %s price %s %s (actual: %s) at %s",
                platform,
                symbol,
                action,
                alert.getThresholdValue().stripTrailingZeros().toPlainString(),
                triggerPrice.stripTrailingZeros().toPlainString(),
                candle.getTimestamp());
    }
}