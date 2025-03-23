package com.example.ehe_server.filter;

import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ehe_server.service.audit.AuditContextService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(1) // Run after security filters
public class RequestTransactionFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;
    private final AuditContextService auditContextService;

    public RequestTransactionFilter(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            AuditContextService auditContextService) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        this.auditContextService = auditContextService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Define transaction
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);

        // Start transaction for the request
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);

        try {
            // Set request path
            auditContextService.setRequestPath(request.getRequestURI());

            // Continue with request chain
            filterChain.doFilter(request, response);

            // Commit transaction if successful
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            // Roll back on error
            transactionManager.rollback(txStatus);
            throw e;
        }
    }
}
