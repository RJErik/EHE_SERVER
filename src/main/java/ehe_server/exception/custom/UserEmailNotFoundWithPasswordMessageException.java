package ehe_server.exception.custom;

public class UserEmailNotFoundWithPasswordMessageException extends ResourceNotFoundException {
    public UserEmailNotFoundWithPasswordMessageException(String email) {
        super("error.message.invalidCredentials", "error.logDetail.userEmailNotFound", email);

    }
}
