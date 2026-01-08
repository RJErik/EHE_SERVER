package ehe_server.exception.custom;

public class SameEmailChangeException extends BusinessRuleException {
    public SameEmailChangeException() {
        super("error.message.sameEmailChange", "error.logDetail.sameEmailChange");
    }
}
