package com.cometchat.chat.exceptions;

import java.util.Map;

/** Error carrier used across the Kit; the hub client maps its error envelope into it. */
public class CometChatException extends Exception {
    private String code;
    private String details;
    private Map<String, Object> errorParams;

    public CometChatException(String code, String message) {
        super(message);
        this.code = code;
    }

    public CometChatException(String code, String message, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public CometChatException(String code, String message, String details, Map<String, Object> errorParams) {
        super(message);
        this.code = code;
        this.details = details;
        this.errorParams = errorParams;
    }

    public String getCode() { return code; }
    public void setCode(String v) { this.code = v; }
    public String getDetails() { return details; }
    public void setDetails(String v) { this.details = v; }
    public Map<String, Object> getErrorParams() { return errorParams; }
    public void setErrorParams(Map<String, Object> v) { this.errorParams = v; }
}
