package com.example.ehe_server.service.context;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ContextPropagationService {
    private final JdbcTemplate jdbcTemplate;

    public ContextPropagationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void propagateCurrentContext() {
        // Extract from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = "unknown";
        String roles = "none";

        if (auth != null && auth.isAuthenticated() &&
                !(auth instanceof AnonymousAuthenticationToken)) {
            userId = auth.getPrincipal().toString();
            roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
        }

        // Use set_config with local=true instead of SET
        jdbcTemplate.queryForObject("SELECT set_config('myapp.current_user', ?, true)",
                String.class, userId);

        jdbcTemplate.queryForObject("SELECT set_config('myapp.current_user_role', ?, true)",
                String.class, roles);
    }

    public void setRequestPath(String path) {
        if (path != null) {
            jdbcTemplate.queryForObject("SELECT set_config('myapp.request_path', ?, true)",
                    String.class, path);
        }
    }
}
