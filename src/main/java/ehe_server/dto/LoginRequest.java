package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.RegexPattern;
import ehe_server.exception.custom.InvalidEmailFormatException;
import ehe_server.exception.custom.InvalidPasswordFormatException;
import ehe_server.exception.custom.MissingEmailException;
import ehe_server.exception.custom.MissingPasswordException;

public class LoginRequest {

    @NotEmptyString(exception = MissingEmailException.class)
    @RegexPattern(pattern = "^(?=.{1,255}$)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    exception = InvalidEmailFormatException.class)
    private String email;

    @NotEmptyString(exception = MissingPasswordException.class)
    @RegexPattern(pattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$",
            exception = InvalidPasswordFormatException.class)
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
