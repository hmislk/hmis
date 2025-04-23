
package com.divudi.core.data;

/**
 *
 * @author Chinthaka Prasad
 */
public enum OnlineBookingStatus {
    PENDING("Temporary Booking"),
    COMPLETED("Complete the booking with payment"),
    CANCEL("Cancel the booking");
    
    private final String label;
    
    OnlineBookingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
