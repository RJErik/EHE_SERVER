package com.example.ehe_server.exception.custom;

public class MissingSessionIdException extends ValidationException {
  public MissingSessionIdException() {
    super("error.message.missingSessionId", "error.logDetail.missingSessionId");
  }
}