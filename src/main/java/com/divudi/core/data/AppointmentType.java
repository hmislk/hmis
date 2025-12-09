package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */

public enum AppointmentType {
    IP_APPOINTMENT("Inpatient Appointment", "IPA" );
    
    private final String displayName;
    private final String code;
    
    AppointmentType(String displayName, String code) {
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
