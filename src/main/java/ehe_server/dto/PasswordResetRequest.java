package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.RegexPattern;
import ehe_server.exception.custom.InvalidEmailFormatException;
import ehe_server.exception.custom.MissingEmailException;

public class PasswordResetRequest {

    @NotEmptyString(exception = MissingEmailException.class)
    @RegexPattern(pattern = "^(?=.{1,255}$)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    exception = InvalidEmailFormatException.class)
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
