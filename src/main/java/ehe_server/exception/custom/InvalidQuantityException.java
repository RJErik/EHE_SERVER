package ehe_server.exception.custom;

import java.math.BigDecimal;

public class InvalidQuantityException extends ValidationException {
    public InvalidQuantityException(BigDecimal value) {
        super("error.message.invalidQuantity", "error.logDetail.invalidQuantity", value);
    }

    public InvalidQuantityException() {
        super("error.message.invalidQuantity", "error.logDetail.invalidQuantity");
    }
}