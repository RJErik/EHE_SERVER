package com.example.ehe_server.filter;

import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

@Component
@Order(2) // Execute after Spring Security filters
public class RequestTransactionFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;

    public RequestTransactionFilter(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Start a transaction for the entire request
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);

        try {
            // Default values
            String userId = "unknown";
            String userRole = "none";

            // Get authentication from security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !(authentication instanceof AnonymousAuthenticationToken)) {

                // Extract user ID
                Object principal = authentication.getPrincipal();
                if (principal != null) {
                    userId = principal.toString();
                }

                // Extract roles
                if (!authentication.getAuthorities().isEmpty()) {
                    userRole = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.joining(","));
                }
            }

            // Set PostgreSQL session variables using queryForObject instead of update
            jdbcTemplate.queryForObject(
                    "SELECT set_config(?, ?, true)",
                    String.class,
                    "myapp.current_user", userId
            );

            jdbcTemplate.queryForObject(
                    "SELECT set_config(?, ?, true)",
                    String.class,
                    "myapp.current_user_role", userRole
            );

            jdbcTemplate.queryForObject(
                    "SELECT set_config(?, ?, true)",
                    String.class,
                    "myapp.request_path", request.getRequestURI()
            );

            // Continue with request chain
            filterChain.doFilter(request, response);

            // Commit the transaction if all went well
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            // Roll back on error
            transactionManager.rollback(txStatus);
            throw e;
        }
    }
}
