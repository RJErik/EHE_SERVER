package ehe_server.exception.custom;

public class MissingUsernameException extends ValidationException {
    public MissingUsernameException() {
        super("error.message.missingUsername", "error.logDetail.missingUsername");
    }
}