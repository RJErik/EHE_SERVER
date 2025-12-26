package com.example.ehe_server.exception.custom;

public class UnauthorizedWatchlistAccessException extends ResourceNotFoundException {
    public UnauthorizedWatchlistAccessException(Integer userId, Integer watchlistItemId) {
        super("error.message.watchlistItemNotFound", "error.logDetail.unauthorizedWatchlistAccess", userId, watchlistItemId);
    }
}