package ehe_server.exception.custom;

public class MissingUserIdException extends ValidationException {
    public MissingUserIdException() {
        super("error.message.missingUserId", "error.logDetail.missingUserId");
    }
}