package ehe_server.exception.custom;

public class InvalidPasswordFormatException extends ValidationException {
    public InvalidPasswordFormatException() {
        super("error.message.invalidPasswordFormat", "error.logDetail.invalidPasswordFormat");
    }
}