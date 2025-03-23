package com.example.ehe_server.service.intf.audit;

/**
 * Interface for the Audit Context Service.
 * Provides methods to get and set audit context information stored in the database session.
 */
public interface AuditContextServiceInterface {

    /**
     * Get the current user identifier from the session context.
     *
     * @return the current user identifier
     */
    String getCurrentUser();

    /**
     * Get the current user role from the session context.
     *
     * @return the current user role
     */
    String getCurrentUserRole();

    /**
     * Get the current request path from the session context.
     *
     * @return the current request path
     */
    String getRequestPath();

    /**
     * Set the current user identifier in the session context.
     *
     * @param userId the user identifier to set
     */
    void setCurrentUser(String userId);

    /**
     * Set the current user role in the session context.
     *
     * @param roles the user role to set
     */
    void setCurrentUserRole(String roles);

    /**
     * Set the request path in the session context.
     *
     * @param path the request path to set
     */
    void setRequestPath(String path);

    /**
     * Set additional audit information in the session context.
     *
     * @param key the key for the additional information
     * @param value the value to store
     */
    void setAdditionalAuditInfo(String key, String value);
}
