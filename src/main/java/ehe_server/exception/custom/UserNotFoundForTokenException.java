package ehe_server.exception.custom;

public class UserNotFoundForTokenException extends ResourceNotFoundException {
    public UserNotFoundForTokenException(String token) {
        super("error.message.userNotFoundForToken", "error.logDetail.userNotFoundForToken", token);
    }
}