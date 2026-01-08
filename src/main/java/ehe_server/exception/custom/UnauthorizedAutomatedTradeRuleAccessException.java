package ehe_server.exception.custom;

public class UnauthorizedAutomatedTradeRuleAccessException extends ResourceNotFoundException {
    public UnauthorizedAutomatedTradeRuleAccessException(Integer ruleId, Integer userId) {
        super("error.message.automatedTradeRuleNotFound", "error.logDetail.unauthorizedRuleAccess", ruleId, userId);
    }
}