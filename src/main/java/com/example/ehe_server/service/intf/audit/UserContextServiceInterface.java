package com.example.ehe_server.service.intf.audit;

import com.example.ehe_server.entity.User;

/**
 * Interface for managing user context throughout the application
 */
public interface UserContextServiceInterface {

    /**
     *  Set the current user
     * @param userId identifies who uses the code (be it users or automatic system job), String role the role of who uses the code
     */
    void setUser(String userId, String role);

    /**
     * Get the current authenticated user ID
     * @return User ID if authenticated, null otherwise
     */
    Integer getCurrentUserId();

    /**
     * Get the current user's username for PostgreSQL audit context, as sting
     * @return Username string, "UNKNOWN" if not authenticated
     */
    String getCurrentUserIdAsString();

    /**
     * Check if there is an authenticated user
     * @return true if authenticated, false otherwise
     */
    boolean isAuthenticated();

    /**
     * Check if the current user is a human user (has numeric ID)
     * @return true if current user has numeric ID, false for system users or if not authenticated
     */
    boolean isHumanUser();

    /**
     * Gets the human user if the user is human (has an id)
     * @return with the user attribute, otherwise return null
     */
    User getCurrentHumanUser();

    /**
     * Get the current user's role
     * @return Role string if authenticated, null otherwise
     */
    String getCurrentUserRole();
}