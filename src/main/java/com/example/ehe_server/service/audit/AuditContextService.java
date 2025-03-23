package com.example.ehe_server.service.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditContextService {

    private final JdbcTemplate jdbcTemplate;

    public AuditContextService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Existing getters
    public String getCurrentUser() {
        return jdbcTemplate.queryForObject(
                "SELECT current_setting('myapp.current_user', true)", String.class);
    }

    public String getCurrentUserRole() {
        return jdbcTemplate.queryForObject(
                "SELECT current_setting('myapp.current_user_role', true)", String.class);
    }

    public String getRequestPath() {
        return jdbcTemplate.queryForObject(
                "SELECT current_setting('myapp.request_path', true)", String.class);
    }

    // New setters to directly set specific context values
    public void setCurrentUser(String userId) {
        jdbcTemplate.queryForObject(
                "SELECT set_config('myapp.current_user', ?, true)",
                String.class, userId);
    }

    public void setCurrentUserRole(String roles) {
        jdbcTemplate.queryForObject(
                "SELECT set_config('myapp.current_user_role', ?, true)",
                String.class, roles);
    }

    public void setRequestPath(String path) {
        jdbcTemplate.queryForObject(
                "SELECT set_config('myapp.request_path', ?, true)",
                String.class, path);
    }

    public void setAdditionalAuditInfo(String key, String value) {
        // For additional context if needed - using parameterized query
        jdbcTemplate.queryForObject(
                "SELECT set_config(?, ?, true)",
                String.class, "myapp." + key, value
        );
    }
}
