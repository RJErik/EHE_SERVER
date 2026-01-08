package ehe_server.exception.custom;

public class WatchlistItemNotFoundException extends ResourceNotFoundException {
    public WatchlistItemNotFoundException(Integer watchlistItemId) {
        super("error.message.watchlistItemNotFound", "error.logDetail.watchlistItemNotFound", watchlistItemId);
    }
}