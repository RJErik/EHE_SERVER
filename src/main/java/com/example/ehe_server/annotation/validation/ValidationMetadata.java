package com.example.ehe_server.annotation.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ValidationMetadata {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String exceptionClass;
    private String[] params;
    private String actionLinkText;
    private String actionLinkTarget;
    private boolean showResendButton;

    public ValidationMetadata() {}

    public ValidationMetadata(
            String exceptionClass,
            String[] params,
            String actionLinkText,
            String actionLinkTarget,
            boolean showResendButton) {
        this.exceptionClass = exceptionClass;
        this.params = params;
        this.actionLinkText = actionLinkText;
        this.actionLinkTarget = actionLinkTarget;
        this.showResendButton = showResendButton;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(String exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String[] getParams() {
        return params;
    }

    public void setParams(String[] params) {
        this.params = params;
    }

    public String getActionLinkText() {
        return actionLinkText;
    }

    public void setActionLinkText(String actionLinkText) {
        this.actionLinkText = actionLinkText;
    }

    public String getActionLinkTarget() {
        return actionLinkTarget;
    }

    public void setActionLinkTarget(String actionLinkTarget) {
        this.actionLinkTarget = actionLinkTarget;
    }

    public boolean isShowResendButton() {
        return showResendButton;
    }

    public void setShowResendButton(boolean showResendButton) {
        this.showResendButton = showResendButton;
    }

    public boolean hasActionLink() {
        return actionLinkText != null && !actionLinkText.isEmpty()
                && actionLinkTarget != null && !actionLinkTarget.isEmpty();
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return exceptionClass;
        }
    }

    public static ValidationMetadata fromJson(String json) {
        try {
            return MAPPER.readValue(json, ValidationMetadata.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public static boolean isValidJson(String str) {
        return str != null && str.startsWith("{") && str.contains("exceptionClass");
    }
}