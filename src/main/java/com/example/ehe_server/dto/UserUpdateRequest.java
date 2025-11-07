package com.example.ehe_server.dto;

import java.util.Objects;

public class UserUpdateRequest {
    private Integer userId;
    private String userName;
    private String email;
    private String password;
    private String accountStatus;
    private String registrationDate;

    public UserUpdateRequest(Integer userId, String userName, String email, String password, String accountStatus, String registrationDate) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.password = password;
        this.accountStatus = accountStatus;
        this.registrationDate = registrationDate;
    }

    public UserUpdateRequest() {
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserUpdateRequest that = (UserUpdateRequest) o;
        return Objects.equals(userId, that.userId) && Objects.equals(userName, that.userName) && Objects.equals(email, that.email) && Objects.equals(password, that.password) && Objects.equals(accountStatus, that.accountStatus) && Objects.equals(registrationDate, that.registrationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, userName, email, password, accountStatus, registrationDate);
    }

    @Override
    public String toString() {
        return "UserUpdateRequest{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", registrationDate='" + registrationDate + '\'' +
                '}';
    }
}
