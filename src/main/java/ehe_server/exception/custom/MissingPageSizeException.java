package ehe_server.exception.custom;

public class MissingPageSizeException extends ValidationException {
    public MissingPageSizeException() {
        super("error.message.missingPageSize", "error.logDetail.missingPageSize");
    }
}