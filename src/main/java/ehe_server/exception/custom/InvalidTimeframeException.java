package ehe_server.exception.custom;

public class InvalidTimeframeException extends ValidationException {
    public InvalidTimeframeException(String timeframe) {
        super("error.message.invalidTimeframe", "error.logDetail.invalidTimeframe", timeframe);
    }
}