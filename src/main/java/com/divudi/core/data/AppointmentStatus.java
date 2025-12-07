package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum AppointmentStatus {
    PENDING("Pending"),
    COMPLETE("Complete"),
    CANCEL("Cancel");
    
    private final String name;
    
    AppointmentStatus(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }


}
