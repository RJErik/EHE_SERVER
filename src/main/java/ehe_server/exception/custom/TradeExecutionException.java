package ehe_server.exception.custom;

public class TradeExecutionException extends BusinessRuleException {
    public TradeExecutionException(String message) {
        super("error.message.tradeExecution", "error.logDetail.tradeExecution", message);
    }
}