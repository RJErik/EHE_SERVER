package com.example.ehe_server.exception.custom;

public class PlatformNotSupportedException extends ResourceNotFoundException {
    public PlatformNotSupportedException(String platformName) {
        super("error.message.platformNotSupported", "error.logDetail.platformNotSupported", platformName);
    }
}
