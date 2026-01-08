package ehe_server.exception.custom;

public class InvalidUsernameFormatException extends ValidationException {
    public InvalidUsernameFormatException(String username) {
        super("error.message.invalidUsernameFormat", "error.logDetail.invalidUsernameFormat", username);
    }
}