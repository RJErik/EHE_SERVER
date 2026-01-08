package ehe_server.dto;

import ehe_server.annotation.validation.MinValue;
import ehe_server.annotation.validation.NotNullField;
import ehe_server.entity.User;
import ehe_server.exception.custom.InvalidPageNumberException;
import ehe_server.exception.custom.InvalidPageSizeException;
import ehe_server.exception.custom.MissingPageNumberException;
import ehe_server.exception.custom.MissingPageSizeException;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserSearchRequest {

    private Integer userId;
    private String userName;
    private String email;
    private User.AccountStatus accountStatus;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime registrationDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime registrationDateFrom;

    @NotNullField(exception = MissingPageSizeException.class)
    @MinValue(exception = InvalidPageSizeException.class, min = 1)
    private Integer size;

    @NotNullField(exception = MissingPageNumberException.class)
    @MinValue(exception = InvalidPageNumberException.class, min = 0)
    private Integer page;

    public UserSearchRequest() {
    }

    public UserSearchRequest(Integer userId, String userName, String email,
                             User.AccountStatus accountStatus, LocalDateTime registrationDateTo,
                             LocalDateTime registrationDateFrom, Integer size, Integer page) {
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.accountStatus = accountStatus;
        this.registrationDateTo = registrationDateTo;
        this.registrationDateFrom = registrationDateFrom;
        this.size = size;
        this.page = page;
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

    public User.AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(User.AccountStatus accountStatus) {
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

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
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
                Objects.equals(registrationDateFrom, that.registrationDateFrom) &&
                Objects.equals(size, that.size) &&
                Objects.equals(page, that.page);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, userName, email, accountStatus,
                registrationDateTo, registrationDateFrom, size, page);
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
                ", size=" + size +
                ", page=" + page +
                '}';
    }
}