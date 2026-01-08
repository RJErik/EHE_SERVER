package ehe_server.exception.custom;

public class MissingRoleException extends ValidationException {
    public MissingRoleException() {
        super("error.message.missingRole", "error.logDetail.missingRole");
    }
}