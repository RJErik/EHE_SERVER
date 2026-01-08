package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.RegexPattern;
import ehe_server.exception.custom.InvalidPasswordFormatException;
import ehe_server.exception.custom.MissingPasswordException;
import ehe_server.exception.custom.MissingVerificationTokenException;

public class ResetPasswordRequest {

    @NotEmptyString(exception = MissingVerificationTokenException.class)
    private String token;

    @NotEmptyString(exception = MissingPasswordException.class)
    @RegexPattern(pattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$",
            exception = InvalidPasswordFormatException.class)
    private String password;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
