package com.example.ehe_server.exception.custom;

public class InvalidPageNumberException extends ValidationException {
    public InvalidPageNumberException(Integer page) {
        super(
                "error.message.invalidPageNumber",
                "error.logDetail.invalidPageNumber",
                page
        );
    }
}