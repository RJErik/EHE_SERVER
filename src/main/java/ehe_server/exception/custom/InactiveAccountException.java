package ehe_server.exception.custom;

public class InactiveAccountException extends AuthorizationException {
    public InactiveAccountException(String userId, String status) {
        super("error.message.inactiveAccount", "error.logDetail.inactiveAccount", userId, status);
    }
}