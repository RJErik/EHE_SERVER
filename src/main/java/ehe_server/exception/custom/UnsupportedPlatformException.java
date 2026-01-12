package ehe_server.exception.custom;

public class UnsupportedPlatformException extends ValidationException {
    public UnsupportedPlatformException(String platformName) {
        super(
                "error.message.unsupportedPlatform",
                "error.logDetail.unsupportedPlatform",
                platformName
        );
    }
}