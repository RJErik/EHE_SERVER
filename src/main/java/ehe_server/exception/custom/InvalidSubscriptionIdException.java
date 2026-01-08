package ehe_server.exception.custom;

public class InvalidSubscriptionIdException extends ValidationException {
    public InvalidSubscriptionIdException() {
        super("error.message.invalidSubscriptionId", "error.logDetail.invalidSubscriptionId");
    }
}