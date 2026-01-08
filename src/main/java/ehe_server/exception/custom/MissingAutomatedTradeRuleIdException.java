package ehe_server.exception.custom;

public class MissingAutomatedTradeRuleIdException extends ValidationException {
    public MissingAutomatedTradeRuleIdException() {
        super("error.message.missingAutomatedTradeRuleId", "error.logDetail.missingAutomatedTradeRuleId");
    }
}