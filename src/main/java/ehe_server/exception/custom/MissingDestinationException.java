package ehe_server.exception.custom;

public class MissingDestinationException extends ValidationException {
    public MissingDestinationException() {
        super("error.message.missingDestination", "error.logDetail.missingDestination");
    }
}