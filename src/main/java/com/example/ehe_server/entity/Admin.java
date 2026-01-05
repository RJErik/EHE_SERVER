package com.example.ehe_server.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "\"admin\"")
public class Admin {

    @Id
    private Integer adminId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "admin_id")
    private User user;

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
