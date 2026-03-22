package com.divudi.core.data;

public enum AppointmentScheduleDateOverrideType {
    BLOCKED("Blocked", "BLK"),
    CUSTOM_LIMIT("Custom Limit", "CUS"),
    CUSTOM_TIME("Custom Time", "TIM");

    private final String displayName;
    private final String code;

    AppointmentScheduleDateOverrideType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}
