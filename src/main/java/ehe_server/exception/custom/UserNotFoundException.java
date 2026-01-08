package ehe_server.exception.custom;

public class UserNotFoundException extends ResourceNotFoundException {
  public UserNotFoundException(Integer userId) {
    super("error.message.userNotFound", "error.logDetail.userNotFound", userId);
  }
}