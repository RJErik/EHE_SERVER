package ehe_server.service.intf.auth;

public interface RegistrationVerificationServiceInterface {
    void verifyRegistrationToken(String token);
}