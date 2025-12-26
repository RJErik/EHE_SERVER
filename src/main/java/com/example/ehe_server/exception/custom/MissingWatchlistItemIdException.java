package com.example.ehe_server.exception.custom;

public class MissingWatchlistItemIdException extends ValidationException {
    public MissingWatchlistItemIdException() {
        super("error.message.missingWatchlistItemId", "error.logDetail.missingWatchlistItemId");
    }
}