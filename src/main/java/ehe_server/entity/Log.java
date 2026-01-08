package ehe_server.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "log")
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Integer logId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "log_description", nullable = false, columnDefinition = "TEXT")
    private String logDescription;

    @CreationTimestamp
    @Column(name = "log_date", nullable = false, updatable = false)
    private LocalDateTime logDate;

    @PrePersist
    protected void onCreate() {
        if (logDate == null) {
            logDate = LocalDateTime.now();
        }
    }

    public Integer getLogId() {
        return logId;
    }

    public void setLogId(Integer logId) {
        this.logId = logId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getLogDescription() {
        return logDescription;
    }

    public void setLogDescription(String logDescription) {
        this.logDescription = logDescription;
    }

    public LocalDateTime getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDateTime logDate) {
        this.logDate = logDate;
    }
}
