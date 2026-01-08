package ehe_server.exception.custom;

public class MissingEndSequenceNumberException extends ValidationException {
    public MissingEndSequenceNumberException() {
        super("error.message.missingEndSequenceNumber", "error.logDetail.missingEndSequenceNumber");
    }
}