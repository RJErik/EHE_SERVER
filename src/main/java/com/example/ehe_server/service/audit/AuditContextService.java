package com.example.ehe_server.service.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditContextService {

    private final JdbcTemplate jdbcTemplate;

    public AuditContextService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public void setAdditionalAuditInfo(String key, String value) {
        // For additional context if needed - using parameterized query
        jdbcTemplate.update(
                "SELECT set_config(?, ?, true)",
                "myapp." + key, value
        );
    }
}
