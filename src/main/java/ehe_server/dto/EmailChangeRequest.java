package ehe_server.dto;

import ehe_server.annotation.validation.NotEmptyString;
import ehe_server.annotation.validation.RegexPattern;
import ehe_server.exception.custom.InvalidEmailFormatException;
import ehe_server.exception.custom.MissingEmailException;

public class EmailChangeRequest {

    @NotEmptyString(exception = MissingEmailException.class)
    @RegexPattern(
            pattern = "^(?=.{1,255}$)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            exception = InvalidEmailFormatException.class,
            params = {"$value"}
    )
    private String newEmail;

    public String getNewEmail() {
        return newEmail;
    }

    public void setNewEmail(String newEmail) {
        this.newEmail = newEmail;
    }
}
