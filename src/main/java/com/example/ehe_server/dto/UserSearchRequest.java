package com.example.ehe_server.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserSearchRequest {

    private Integer userId;
    private String userName;
    private String email;
    private String accountStatus;

    // Use LocalDateTime directly - Spring handles the conversion
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime registrationDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime registrationDateFrom;

    public UserSearchRequest() {
    }

    public UserSearchRequest(Integer userId, String userName, String email,
                             String accountStatus, LocalDateTime registrationDateTo,
                             LocalDateTime registrationDateFrom) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.accountStatus = accountStatus;
        this.registrationDateTo = registrationDateTo;
        this.registrationDateFrom = registrationDateFrom;
    }

    // Getters and Setters
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

    public LocalDateTime getRegistrationDateTo() {
        return registrationDateTo;
    }

    public void setRegistrationDateTo(LocalDateTime registrationDateTo) {
        this.registrationDateTo = registrationDateTo;
    }

    public LocalDateTime getRegistrationDateFrom() {
        return registrationDateFrom;
    }

    public void setRegistrationDateFrom(LocalDateTime registrationDateFrom) {
        this.registrationDateFrom = registrationDateFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserSearchRequest that = (UserSearchRequest) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(email, that.email) &&
                Objects.equals(accountStatus, that.accountStatus) &&
                Objects.equals(registrationDateTo, that.registrationDateTo) &&
                Objects.equals(registrationDateFrom, that.registrationDateFrom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, userName, email, accountStatus,
                registrationDateTo, registrationDateFrom);
    }

    @Override
    public String toString() {
        return "UserSearchRequest{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", registrationDateTo=" + registrationDateTo +
                ", registrationDateFrom=" + registrationDateFrom +
                '}';
    }
}