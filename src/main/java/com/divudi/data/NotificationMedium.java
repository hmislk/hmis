package com.divudi.data;

/**
 * Enum to categorize trigger types into system notification, SMS, and email.
 * 
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public enum NotificationMedium {
    SYSTEM_NOTIFICATION("System Notification"),
    SMS("SMS"),
    EMAIL("Email");

    private final String description;

    NotificationMedium(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
