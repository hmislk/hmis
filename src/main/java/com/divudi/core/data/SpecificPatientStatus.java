
package com.divudi.core.data;

/**
 *
 * @author CHINTHAKA
 * This Enum class define for mark patient in the system as vip, vvip and member.
 */
public enum SpecificPatientStatus {
    
    NORMAL("Normal Person"),
    VIP("Very Important Person"),
    VVIP("Very Very Important Person"),
    STAFF("Staff Member");
    
    private final String label;

    SpecificPatientStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
    
    
 
    
}
