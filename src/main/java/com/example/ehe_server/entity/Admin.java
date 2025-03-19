package com.example.ehe_server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "\"admin\"") // Quotes are used because "admin" might be a reserved keyword
public class Admin {

    @Id
    private Integer adminId;

    @OneToOne
    @MapsId // Shares primary key with User entity
    @JoinColumn(name = "admin_id") // Foreign key column in this table
    private User user;

    // Getters and setters
    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
