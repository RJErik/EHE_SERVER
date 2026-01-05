package com.example.ehe_server.dto;

import com.example.ehe_server.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserResponse {
    private Integer id;
    private String userName;
    private String email;
    private User.AccountStatus accountStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime registrationDate;

    public UserResponse() {}

    public UserResponse(Integer id, String userName, String email, User.AccountStatus accountStatus, LocalDateTime registrationDate) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.accountStatus = accountStatus;
        this.registrationDate = registrationDate;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public User.AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(User.AccountStatus accountStatus) { this.accountStatus = accountStatus; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserResponse that = (UserResponse) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(userName, that.userName) &&
                Objects.equals(email, that.email) &&
                Objects.equals(accountStatus, that.accountStatus) &&
                Objects.equals(registrationDate, that.registrationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userName, email, accountStatus, registrationDate);
    }

    @Override
    public String toString() {
        return "UserResponse{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", registrationDate='" + registrationDate + '\'' +
                '}';
    }
}