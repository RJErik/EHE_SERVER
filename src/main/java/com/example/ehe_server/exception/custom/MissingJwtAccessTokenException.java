package com.example.ehe_server.exception.custom;

public class MissingJwtAccessTokenException extends ValidationException {
  public MissingJwtAccessTokenException() {
    super("error.message.missingJwtAccessToken", "error.logDetail.missingJwtAccessToken");
  }
}