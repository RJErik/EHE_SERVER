package ehe_server.exception.custom;

public class PlatformStockNotFoundException extends ResourceNotFoundException {
    public PlatformStockNotFoundException(String platform, String symbol) {
        super("error.message.platformStockNotFound", "error.logDetail.platformStockNotFound", platform, symbol);
    }
}