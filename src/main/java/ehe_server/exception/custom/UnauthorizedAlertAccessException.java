package ehe_server.exception.custom;

public class UnauthorizedAlertAccessException extends ResourceNotFoundException {
    public UnauthorizedAlertAccessException(Integer userId, Integer alertId) {
        super("error.message.alertNotFound", "error.logDetail.unauthorizedAlertAccess", userId, alertId);
    }
}