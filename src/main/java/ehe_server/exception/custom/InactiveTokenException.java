package ehe_server.exception.custom;

public class InactiveTokenException extends BusinessRuleException {
    public InactiveTokenException(String token, String status) {
        super("error.message.inactiveToken", "error.logDetail.inactiveToken", token, status);
    }
}