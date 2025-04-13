package com.example.ehe_server.service.intf.audit;

public interface UserContextServiceInterface {
    /**
     * Sets up the PostgreSQL context variables for the current user from the security context.
     * Must be called at the controller level to ensure it's in the same transaction as business logic.
     */
    void setupUserContext();
}
