package ehe_server.exception.custom;

public class NoBalancesFoundException extends ResourceNotFoundException {
    public NoBalancesFoundException(Integer portfolioId) {
        super("error.message.noBalancesFound", "error.logDetail.noBalancesFound", portfolioId);
    }
}