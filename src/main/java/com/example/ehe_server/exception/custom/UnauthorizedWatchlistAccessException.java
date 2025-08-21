package com.example.ehe_server.exception.custom;

public class UnauthorizedWatchlistAccessException extends AuthorizationException {
    public UnauthorizedWatchlistAccessException(Integer userId, Integer watchlistItemId) {
        super("error.message.unauthorizedWatchlistAccess", "error.logDetail.unauthorizedWatchlistAccess", userId, watchlistItemId);
    }
}