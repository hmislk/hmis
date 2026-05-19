package com.divudi.core.data;

public enum AppointmentScheduleType {
    TIME_BASED("Time Based", "TB"),
    DATE_BASED("Date Based", "DB");

    private final String displayName;
    private final String code;

    AppointmentScheduleType(String displayName, String code) {
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
