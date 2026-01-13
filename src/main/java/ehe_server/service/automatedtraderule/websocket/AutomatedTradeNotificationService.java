package ehe_server.service.automatedtraderule.websocket;

import ehe_server.dto.TradeExecutionResponse;
import ehe_server.dto.websocket.AutomatedTradeNotificationResponse;
import ehe_server.entity.AutomatedTradeRule;
import ehe_server.entity.MarketCandle;
import ehe_server.service.intf.automatictrade.websocket.AutomatedTradeNotificationServiceInterface;
import ehe_server.service.intf.log.LoggingServiceInterface;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AutomatedTradeNotificationService implements AutomatedTradeNotificationServiceInterface {

    private static final String AUTOMATED_TRADES_QUEUE = "/queue/automated-trades";

    private final SimpMessagingTemplate messagingTemplate;
    private final LoggingServiceInterface loggingService;

    public AutomatedTradeNotificationService(
            SimpMessagingTemplate messagingTemplate,
            LoggingServiceInterface loggingService) {
        this.messagingTemplate = messagingTemplate;
        this.loggingService = loggingService;
    }

    @Override
    public void sendTradeNotification(
            AutomatedTradeRule rule,
            MarketCandle triggeringCandle,
            boolean success,
            TradeExecutionResponse tradeResult,
            Integer transactionId,
            AutomatedTradeSubscription subscription) {

        AutomatedTradeNotificationResponse notification = buildNotification(
                rule, triggeringCandle, success, tradeResult, transactionId, subscription);

        messagingTemplate.convertAndSendToUser(
                subscription.getUserId().toString(),
                AUTOMATED_TRADES_QUEUE,
                notification
        );

        loggingService.logAction("Sent automated trade notification for rule #" + rule.getAutomatedTradeRuleId());
    }

    private AutomatedTradeNotificationResponse buildNotification(
            AutomatedTradeRule rule,
            MarketCandle triggeringCandle,
            boolean success,
            TradeExecutionResponse tradeResult,
            Integer transactionId,
            AutomatedTradeSubscription subscription) {

        AutomatedTradeNotificationResponse notification = new AutomatedTradeNotificationResponse();

        notification.setSuccess(success);
        notification.setAutomatedTradeRuleId(rule.getAutomatedTradeRuleId());
        notification.setPlatformName(rule.getPlatformStock().getPlatform().getPlatformName());
        notification.setStockSymbol(rule.getPlatformStock().getStock().getStockSymbol());
        notification.setConditionType(rule.getConditionType().toString());
        notification.setThresholdValue(rule.getThresholdValue());
        notification.setActionType(rule.getActionType().toString());
        notification.setQuantityType(rule.getQuantityType().toString());
        notification.setQuantity(rule.getQuantity());
        notification.setCurrentPrice(triggeringCandle.getClosePrice());
        notification.setSubscriptionId(subscription.getId());
        notification.setTriggerTime(LocalDateTime.now());
        notification.setTransactionId(transactionId);
        notification.setTransactionStatus(
                tradeResult != null && tradeResult.getStatus() != null
                        ? tradeResult.getStatus().toString()
                        : (success ? "COMPLETED" : "FAILED"));
        notification.setMessage(formatMessage(rule, triggeringCandle, success, tradeResult));

        return notification;
    }

    private String formatMessage(
            AutomatedTradeRule rule,
            MarketCandle candle,
            boolean success,
            TradeExecutionResponse tradeResult) {

        StringBuilder message = new StringBuilder();
        message.append("AUTOMATED TRADE ");
        message.append(success ? "EXECUTED: " : "FAILED: ");

        appendTradeDetails(message, rule, candle);
        appendExecutionResult(message, success, tradeResult);

        return message.toString();
    }

    private void appendTradeDetails(StringBuilder message, AutomatedTradeRule rule, MarketCandle candle) {
        String conditionStr = formatConditionType(rule.getConditionType());
        String actionStr = formatActionType(rule.getActionType());
        String quantityTypeStr = formatQuantityType(rule.getQuantityType());

        message.append(rule.getPlatformStock().getPlatform().getPlatformName())
                .append(" ")
                .append(rule.getPlatformStock().getStock().getStockSymbol())
                .append(" price ")
                .append(conditionStr)
                .append(" ")
                .append(formatPrice(rule.getThresholdValue()))
                .append(" (actual: ")
                .append(formatPrice(candle.getClosePrice()))
                .append(") triggered ")
                .append(actionStr)
                .append(" order for ")
                .append(formatPrice(rule.getQuantity()))
                .append(" (")
                .append(quantityTypeStr)
                .append(")");
    }

    private void appendExecutionResult(StringBuilder message, boolean success, TradeExecutionResponse tradeResult) {
        if (success && tradeResult != null) {
            message.append(" - Order executed successfully");
            appendExecutionDetails(message, tradeResult);
        } else {
            message.append(" - Failed to execute order");
            if (tradeResult != null && tradeResult.getStatus() != null) {
                message.append(" (Status: ").append(tradeResult.getStatus()).append(")");
            }
        }
    }

    private void appendExecutionDetails(StringBuilder message, TradeExecutionResponse tradeResult) {
        if (tradeResult.getQuantity() != null) {
            String formattedQty = tradeResult.getQuantity()
                    .stripTrailingZeros()
                    .toPlainString();
            message.append(", executed quantity: ").append(formattedQty);
        }
    }

    private String formatPrice(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }

    private String formatConditionType(AutomatedTradeRule.ConditionType conditionType) {
        return switch (conditionType) {
            case PRICE_ABOVE -> "above";
            case PRICE_BELOW -> "below";
        };
    }

    private String formatActionType(AutomatedTradeRule.ActionType actionType) {
        return switch (actionType) {
            case BUY -> "Buy";
            case SELL -> "Sell";
        };
    }

    private String formatQuantityType(AutomatedTradeRule.QuantityType quantityType) {
        return switch (quantityType) {
            case QUANTITY -> "Quantity";
            case QUOTE_ORDER_QTY -> "Quote Order Qty";
        };
    }
}