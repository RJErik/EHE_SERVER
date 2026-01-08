package ehe_server.exception.custom;

public class MissingActionTypeException extends ValidationException {
    public MissingActionTypeException() {
        super("error.message.missingActionType", "error.logDetail.missingActionType");
    }
}