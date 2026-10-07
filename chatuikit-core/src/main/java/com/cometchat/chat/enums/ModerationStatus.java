package com.cometchat.chat.enums;

public enum ModerationStatus {
    UNMODERATED("unmoderated"), PENDING("pending"), APPROVED("approved"), DISAPPROVED("disapproved");

    private final String value;

    ModerationStatus(String value) { this.value = value; }

    public String getValue() { return value; }

    public static ModerationStatus get(String value) {
        for (ModerationStatus s : values()) if (s.value.equalsIgnoreCase(value)) return s;
        return null;
    }
}
