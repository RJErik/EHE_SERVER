// src/main/java/com/example/ehe_server/dto/RegistrationRequest.java
package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.RegexPattern;
import com.example.ehe_server.exception.custom.*;
import ehe_server.exception.custom.*;

public class RegistrationRequest {

    @NotEmptyString(exception = MissingUsernameException.class)
    @RegexPattern(pattern = "^[a-zA-Z0-9_]{3,100}$",
    exception = InvalidUsernameFormatException.class)
    private String username;

    @NotEmptyString(exception = MissingEmailException.class)
    @RegexPattern(pattern = "^(?=.{1,255}$)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    exception = InvalidEmailFormatException.class)
    private String email;

    @NotEmptyString(exception = MissingPasswordException.class)
    @RegexPattern(pattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$",
    exception = InvalidPasswordFormatException.class)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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
