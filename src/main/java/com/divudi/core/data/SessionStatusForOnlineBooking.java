
package com.divudi.core.data;

/**
 *
 * @author Chinthaka Prasad
 */
public enum SessionStatusForOnlineBooking {
    Available("Available for online bookings"),
    Started("Session is already started"),
    Ended("Session is ended"),
    Hold("Hold for online bookings"),
    Full("Session is alredy full"),
    Cancelled("Session is cancelled");
    
    private final String label;
    
    SessionStatusForOnlineBooking(String label){
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
}
