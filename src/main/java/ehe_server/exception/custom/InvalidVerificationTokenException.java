package ehe_server.exception.custom;

public class InvalidVerificationTokenException extends ResourceNotFoundException {
    public InvalidVerificationTokenException(String token) {
        super("error.message.invalidVerificationToken", "error.logDetail.invalidVerificationToken", token);
    }
}