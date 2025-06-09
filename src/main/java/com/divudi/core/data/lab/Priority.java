package com.divudi.core.data.lab;

/**
 * Enum representing different priority levels.
 *
 * Author: Dr M H B Ariyaratne
 */
public enum Priority {
    Stat("Stat"),
    Asap("Asap"),
    Routeine("Routeine"),
    DELAYED("Delayed"),
    Other("Other");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
