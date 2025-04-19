package com.example.ehe_server.service.intf.user;

import java.util.Map;

public interface EmailChangeRequestServiceInterface {
    /**
     * Processes a request to change the user's email address
     * @param userId The ID of the user making the request
     * @param newEmail The new email address
     * @return A map containing the result of the operation
     */
    Map<String, Object> requestEmailChange(Long userId, String newEmail);
}
