package ehe_server.dto;

import ehe_server.annotation.validation.RegexPattern;
import ehe_server.entity.User;
import ehe_server.exception.custom.InvalidEmailFormatException;
import ehe_server.exception.custom.InvalidPasswordFormatException;
import ehe_server.exception.custom.InvalidUsernameFormatException;

import java.util.Objects;

public class UserUpdateRequest {

    @RegexPattern( pattern = "^[a-zA-Z0-9_]{3,100}$",
            exception = InvalidUsernameFormatException.class,
            params = {"$value"})
    private String userName;

    @RegexPattern(
            pattern = "^(?=.{1,255}$)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            exception = InvalidEmailFormatException.class,
            params = {"$value"}
    )
    private String email;

    @RegexPattern(
            pattern = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,72}$",
            exception = InvalidPasswordFormatException.class
    )
    private String password;

    private User.AccountStatus accountStatus;

    public UserUpdateRequest() {
    }

    public UserUpdateRequest(String userName, String email, String password, User.AccountStatus accountStatus) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.accountStatus = accountStatus;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public User.AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(User.AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserUpdateRequest that = (UserUpdateRequest) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(email, that.email) &&
                Objects.equals(password, that.password) &&
                Objects.equals(accountStatus, that.accountStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, email, password, accountStatus);
    }

    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}