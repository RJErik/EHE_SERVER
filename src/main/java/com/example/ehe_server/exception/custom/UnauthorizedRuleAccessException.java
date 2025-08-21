package com.example.ehe_server.exception.custom;

public class UnauthorizedRuleAccessException extends AuthorizationException {
    public UnauthorizedRuleAccessException(Integer ruleId, Integer userId) {
        super("error.message.unauthorizedRuleAccess", "error.logDetail.unauthorizedRuleAccess", ruleId, userId);
    }
}