package ehe_server.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "\"user\"")
public class User {

    public enum AccountStatus { ACTIVE, SUSPENDED, NONVERIFIED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @NotBlank
    @Size(min = 3)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @NotBlank
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 50)
    private AccountStatus accountStatus;

    @CreationTimestamp
    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
}
