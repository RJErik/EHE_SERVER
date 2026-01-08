package ehe_server.exception.custom;

public class MissingPasswordException extends ValidationException {
    public MissingPasswordException() {
        super("error.message.missingPassword", "error.logDetail.missingPassword");
    }
}