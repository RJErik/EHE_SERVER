package com.example.ehe_server.exception.custom;

/**
 * Thrown when an operation is attempted for a user that does not exist in the database.
 * Extends ResourceNotFoundException (HTTP 404).
 */

public class UserNotFoundException extends ResourceNotFoundException {
  public UserNotFoundException(Long userId) {
    super("error.message.userNotFound", "error.logDetail.userNotFound", userId);
  }
}