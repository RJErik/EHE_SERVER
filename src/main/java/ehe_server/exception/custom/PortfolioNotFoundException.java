package ehe_server.exception.custom;

public class PortfolioNotFoundException extends ResourceNotFoundException {
    public PortfolioNotFoundException(Integer portfolioId) {
        super("error.message.portfolioNotFound", "error.logDetail.portfolioNotFound", portfolioId);
    }
}