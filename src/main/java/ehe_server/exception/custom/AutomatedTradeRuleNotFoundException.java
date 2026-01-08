package ehe_server.exception.custom;

public class AutomatedTradeRuleNotFoundException extends ResourceNotFoundException {
    public AutomatedTradeRuleNotFoundException(Integer ruleId) {
        super("error.message.automatedTradeRuleNotFound", "error.logDetail.automatedTradeRuleNotFound", ruleId);
    }
}