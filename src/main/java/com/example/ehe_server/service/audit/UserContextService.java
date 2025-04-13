package com.example.ehe_server.service.audit;

import com.example.ehe_server.service.audit.AuditContextService;
import com.example.ehe_server.service.intf.audit.UserContextServiceInterface;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class UserContextService implements UserContextServiceInterface {

    private final AuditContextService auditContextService;

    public UserContextService(AuditContextService auditContextService) {
        this.auditContextService = auditContextService;
    }

    @Override
    public void setupUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal())) {

            // Extract user ID from the principal
            String userId = authentication.getPrincipal().toString();

            // Extract role from authorities
            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","))
                    .replace("ROLE_", ""); // Strip ROLE_ prefix if present

            // Set PostgreSQL context variables
            auditContextService.setCurrentUser(userId);
            auditContextService.setCurrentUserRole(role);

//            System.out.println("User context set: userId=" + userId + ", role=" + role);
        } else {
            // Set default values for unauthenticated requests
            auditContextService.setCurrentUser("UNKNOWN");
            auditContextService.setCurrentUserRole("NONE");
//            System.out.println("Set default user context: UNKNOWN user");
        }
    }
}
