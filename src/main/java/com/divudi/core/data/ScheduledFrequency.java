package com.divudi.core.data;

/**
 * Enum representing the frequency at which a scheduled process should run.
 */
public enum ScheduledFrequency {
    Midnight("Midnight"),
    Hourly("Hourly"),
    WeekEnd("Week End"),
    MonthEnd("Month End"),
    YearEnd("Year End");

    private final String label;

    ScheduledFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
