package com.example.ehe_server.dto;

import java.util.Objects;

public class UserSearchResponse {
    private Integer id;
    private String userName;
    private String email;
    private String accountStatus;
    private String registrationDate;

    public UserSearchResponse(Integer id, String userName, String email, String accountStatus, String registrationDate) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.accountStatus = accountStatus;
        this.registrationDate = registrationDate;
    }

    public UserSearchResponse() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(String registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserSearchResponse that = (UserSearchResponse) o;
        return Objects.equals(id, that.id) && Objects.equals(userName, that.userName) && Objects.equals(email, that.email) && Objects.equals(accountStatus, that.accountStatus) && Objects.equals(registrationDate, that.registrationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userName, email, accountStatus, registrationDate);
    }

    @Override
    public String toString() {
        return "UserSearchResponse{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", accountStatus='" + accountStatus + '\'' +
                ", registrationDate='" + registrationDate + '\'' +
                '}';
    }
}
