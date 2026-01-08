package ehe_server.exception.custom;

public class MissingStockSubscriptionParametersException extends ValidationException {
    public MissingStockSubscriptionParametersException(String missingParameters) {
        super("error.message.missingStockSubscriptionParameters", "error.logDetail.missingStockSubscriptionParameters", missingParameters);
    }
}