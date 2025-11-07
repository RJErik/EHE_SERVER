package com.example.ehe_server.dto;

import java.math.BigDecimal;
import java.util.Objects;

public class UserSearchRequest {
    private Integer userId;
    private String userName;
    private String email;
    private String accountStatus;
    private String registrationDateToTime;
    private String registrationDateFromTime;

    public UserSearchRequest(Integer userId, String userName, String email, String accountStatus, String registrationDateToTime, String registrationDateFromTime) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.accountStatus = accountStatus;
        this.registrationDateToTime = registrationDateToTime;
        this.registrationDateFromTime = registrationDateFromTime;
    }

    public UserSearchRequest() {
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

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getRegistrationDateToTime() {
        return registrationDateToTime;
    }

    public void setRegistrationDateToTime(String registrationDateToTime) {
        this.registrationDateToTime = registrationDateToTime;
    }

    public String getRegistrationDateFromTime() {
        return registrationDateFromTime;
    }

    public void setRegistrationDateFromTime(String registrationDateFromTime) {
        this.registrationDateFromTime = registrationDateFromTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserSearchRequest that = (UserSearchRequest) o;
        return Objects.equals(userId, that.userId) && Objects.equals(userName, that.userName) && Objects.equals(email, that.email) && Objects.equals(accountStatus, that.accountStatus) && Objects.equals(registrationDateToTime, that.registrationDateToTime) && Objects.equals(registrationDateFromTime, that.registrationDateFromTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, userName, email, accountStatus, registrationDateToTime, registrationDateFromTime);
    }

    @Override
    public String toString() {
        return "UserSearchRequest{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", registrationDateToTime='" + registrationDateToTime + '\'' +
                ", registrationDateFromTime='" + registrationDateFromTime + '\'' +
                '}';
    }
}
