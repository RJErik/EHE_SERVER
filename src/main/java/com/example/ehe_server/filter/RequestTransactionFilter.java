package com.example.ehe_server.filter;

import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
@Order(1) // Execute this filter after Spring Security filters
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
            // Get the user ID from the JWT token via SecurityContext
            String auditUser = "system"; // Default if no authentication

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                // The principal should be the user ID from the JWT token
                if (principal != null) {
                    if (principal instanceof Long) {
                        // For authenticated users, use their user ID
                        auditUser = "user_" + principal;
                    } else {
                        auditUser = principal.toString();
                    }
                }
            } else {
                // For public endpoints like login, use something identifiable
                String requestPath = request.getRequestURI();
                String remoteAddr = request.getRemoteAddr();
                auditUser = "unauthenticated_" + remoteAddr + "_" + requestPath;
            }

            // Set PostgreSQL session variable that will be used by audit triggers
            jdbcTemplate.execute("SET LOCAL myapp.current_user = '" +
                    auditUser.replace("'", "''") + "'");

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
