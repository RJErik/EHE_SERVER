package com.example.ehe_server;

import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long validateUser(String email, String password) throws AuthException {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT validate_login(?, ?)",
                    Long.class,
                    email,
                    password
            );
        } catch (EmptyResultDataAccessException e) {
            throw new AuthException("Invalid credentials");
        } catch (Exception e) {
            throw new AuthException("Database error: " + e.getMessage());
        }
    }

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}
