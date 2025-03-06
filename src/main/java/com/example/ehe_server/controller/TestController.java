package com.example.ehe_server.controller;

import com.example.ehe_server.entities.User;
import com.example.ehe_server.repository.TestUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TestUserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TestController(TestUserRepository userRepository, JdbcTemplate jdbcTemplate) {
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/db-connection")
    public String testConnection() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "Database connection successful! Test query result: " + result;
        } catch (Exception e) {
            return "Database connection failed: " + e.getMessage();
        }
    }
}
