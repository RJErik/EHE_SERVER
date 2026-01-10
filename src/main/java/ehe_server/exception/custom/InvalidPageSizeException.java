package ehe_server.exception.custom;

public class InvalidPageSizeException extends ValidationException {
    public InvalidPageSizeException(Integer size) {
        super(
                "error.message.invalidPageSize",
                "error.logDetail.invalidPageSize",
                size
        );
    }

    public InvalidPageSizeException() {
        super("error.message.invalidPageSize", "error.logDetail.invalidPageSize");
    }
}