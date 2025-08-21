package com.example.ehe_server.exception.custom;

import java.util.Map;

// The new and improved base exception
public abstract class CustomBaseException extends RuntimeException {

    private final String logDetailKey;
    private final Object[] logArgs; // For clean logging without manual string concatenation
    private Map<String, String> actionLink; // e.g., {"text": "log in", "target": "login"}
    private boolean showResendButton = false;

    // Constructor for simple exceptions
    public CustomBaseException(String messageKey, String logDetailKey, Object... logArgs) {
        super(messageKey); // The message key for the user
        this.logDetailKey = logDetailKey;
        this.logArgs = logArgs;
    }

    // --- Builder-style methods for special cases ---

    public CustomBaseException withActionLink(String text, String target) {
        this.actionLink = Map.of("text", text, "target", target);
        return this;
    }

    public CustomBaseException withResendButton() {
        this.showResendButton = true;
        return this;
    }

    // Getters...
    public String getLogDetailKey() { return logDetailKey; }
    public Object[] getLogArgs() { return logArgs; }
    public Map<String, String> getActionLink() { return actionLink; }
    public boolean isShowResendButton() { return showResendButton; }
}