package ehe_server.exception.custom;

public class PriceDataNotFoundException extends ResourceNotFoundException {
    public PriceDataNotFoundException(String stockSymbol) {
        super("error.message.priceDataNotFound", "error.logDetail.priceDataNotFound", stockSymbol);
    }
}