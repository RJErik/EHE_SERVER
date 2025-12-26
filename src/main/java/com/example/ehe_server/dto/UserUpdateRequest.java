package com.example.ehe_server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Objects;

/**
 * Request body for updating a user.
 * Note: userId is now passed via path parameter, not in the body.
 */
public class UserUpdateRequest {

    private String userName;

    private String email;

    private String password;

    private String accountStatus;

    public UserUpdateRequest() {
    }

    public UserUpdateRequest(String userName, String email, String password, String accountStatus) {
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.accountStatus = accountStatus;
    }

    // Getters and Setters
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

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
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