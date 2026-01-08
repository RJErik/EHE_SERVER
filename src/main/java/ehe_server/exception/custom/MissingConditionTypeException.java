package ehe_server.exception.custom;

public class MissingConditionTypeException extends ValidationException {
    public MissingConditionTypeException() {
        super("error.message.missingConditionType", "error.logDetail.missingConditionType");
    }
}