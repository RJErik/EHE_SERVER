package ehe_server.exception.custom;

import ehe_server.entity.VerificationToken;

public class TokenTypeMismatchException extends BusinessRuleException {
    public TokenTypeMismatchException(String token, VerificationToken.TokenType expected, VerificationToken.TokenType found) {
        super("error.message.tokenTypeMismatch", "error.logDetail.tokenTypeMismatch", token, expected, found);
    }
}