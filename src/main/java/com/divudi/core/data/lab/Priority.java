package com.divudi.core.data.lab;

/**
 * Enum representing different priority levels.
 *
 * Author: Dr M H B Ariyaratne
 */
public enum Priority {
    @Deprecated
    Stat("Stat","","",""),
    @Deprecated
    Asap("Asap","","",""),
    @Deprecated
    Routeine("Routeine","","",""),
    @Deprecated
    DELAYED("Delayed","","",""),
    @Deprecated
    Other("Other","","",""),
    
    NORMAL("Normal", "", "#00ff25", "Normal processing."),
    HIGH("High", "H", "#f8ff00", "Faster than Normal."),
    URGENT("Urgent", "U", "#ff7700", "Immediate attention required."),
    CRITICAL("Critical", "C", "#ff0000", "Highest priority, stop all other work.");

    private final String displayName;
    private final String shortName;
    private final String priorityColor;
    private final String description;
    
    Priority(String displayName, String shortName, String priorityColor, String description) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.priorityColor = priorityColor;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }

    public String getShortName() {
        return shortName;
    }
    
    public String getPriorityColor() {
        return priorityColor;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }

}
