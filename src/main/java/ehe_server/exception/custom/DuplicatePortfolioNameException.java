package ehe_server.exception.custom;

public class DuplicatePortfolioNameException extends BusinessRuleException {
    public DuplicatePortfolioNameException(String portfolioName) {
        super("error.message.duplicatePortfolioName", "error.logDetail.duplicatePortfolioName", portfolioName);
    }
}