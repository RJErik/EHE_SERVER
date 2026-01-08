package ehe_server.exception.custom;

public class MissingAmountException extends ValidationException {
    public MissingAmountException() {
        super("error.message.missingAmount", "error.logDetail.missingAmount");
    }
}