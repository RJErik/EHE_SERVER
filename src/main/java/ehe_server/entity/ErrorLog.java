package ehe_server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "error_log")
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_log_id")
    private Integer errorLogId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "error_description", nullable = false, columnDefinition = "TEXT")
    private String errorDescription;

    @Column(name = "stack_trace", nullable = false, columnDefinition = "TEXT")
    private String stackTrace;

    @CreationTimestamp
    @Column(name = "error_date", nullable = false, updatable = false)
    private LocalDateTime errorDate;

    public Integer getErrorLogId() {
        return errorLogId;
    }

    public void setErrorLogId(Integer errorLogId) {
        this.errorLogId = errorLogId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public LocalDateTime getErrorDate() {
        return errorDate;
    }

    public void setErrorDate(LocalDateTime errorDate) {
        this.errorDate = errorDate;
    }
}
