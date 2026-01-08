package ehe_server.exception.custom;

public class MissingQuantityException extends ValidationException {
    public MissingQuantityException() {
        super("error.message.missingQuantity", "error.logDetail.missingQuantity");
    }
}