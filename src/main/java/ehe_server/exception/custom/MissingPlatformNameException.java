package ehe_server.exception.custom;

public class MissingPlatformNameException extends ValidationException {
    public MissingPlatformNameException() {
        super("error.message.missingPlatformName", "error.logDetail.missingPlatformName");
    }
}