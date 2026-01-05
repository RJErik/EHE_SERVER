package com.example.ehe_server.dto;

import com.example.ehe_server.annotation.validation.NotEmptyString;
import com.example.ehe_server.annotation.validation.RegexPattern;
import com.example.ehe_server.exception.custom.InvalidEmailFormatException;
import com.example.ehe_server.exception.custom.MissingEmailException;

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
