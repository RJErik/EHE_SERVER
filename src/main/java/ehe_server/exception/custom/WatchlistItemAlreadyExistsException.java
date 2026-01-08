package ehe_server.exception.custom;

public class WatchlistItemAlreadyExistsException extends BusinessRuleException {
    public WatchlistItemAlreadyExistsException(String platform, String symbol) {
        super("error.message.watchlistItemAlreadyExists", "error.logDetail.watchlistItemAlreadyExists", platform, symbol);
    }
}