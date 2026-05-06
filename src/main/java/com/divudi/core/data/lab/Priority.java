package com.divudi.core.data.lab;

/**
 * Enum representing different priority levels.
 *
 * Author: Dr M H B Ariyaratne
 */
public enum Priority {
    @Deprecated
    Stat("Stat","","","",0),
    @Deprecated
    Asap("Asap","","","",0),
    @Deprecated
    Routeine("Routeine","","","",0),
    @Deprecated
    DELAYED("Delayed","","","",0),
    @Deprecated
    Other("Other","","","",0),
    
    NORMAL("Normal", "", "#00ff25", "Normal processing.",1),
    HIGH("High", "H", "#f8ff00", "Faster than Normal.",2),
    URGENT("Urgent", "U", "#ff7700", "Immediate attention required.",3),
    CRITICAL("Critical", "C", "#ff0000", "Highest priority, stop all other work.",4);

    private final String displayName;
    private final String shortName;
    private final String priorityColor;
    private final String description;
    private final int level;
    
    Priority(String displayName, String shortName, String priorityColor, String description, int level ) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.priorityColor = priorityColor;
        this.description = description;
        this.level = level;
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
        return displayName;
    }

    public int getLevel() {
        return level;
    }

}
