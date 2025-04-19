package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface UserDeactivationServiceInterface {
    /**
     * Deactivates a user account by setting its status to inactive
     *
     * @param userId The ID of the user to deactivate
     * @return Map containing the result of the operation
     */
    Map<String, Object> deactivateUser(Long userId);
}
