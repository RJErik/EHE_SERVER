package ehe_server.exception.custom;

public class PlatformNotFoundException extends ResourceNotFoundException {
    public PlatformNotFoundException(String platformName) {
        super("error.message.platformNotFound", "error.logDetail.platformNotFound", platformName);
    }
}