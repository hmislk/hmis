
package com.divudi.core.data;

/**
 *
 * @author Chinthaka Prasad
 */
public enum OnlineBookingStatus {
    PENDING("Temporary Booking"),
    ACTIVE("Completed the booking with Payment"),
    ABSENT("Patient absent to the booking. Completed Payment"),
    COMPLETED("Patient visited and completed the appoinment"),
    PATIENT_CANCELED("Patient canceled appoinment from API"),
    DOCTOR_CANCELED("Doctor cancelled the session");
    
    private final String label;
    
    OnlineBookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
