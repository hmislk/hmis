
package com.divudi.core.data;

/**
 *
 * @author CHINTHAKA
 * This Enum class define for mark patient in the system as vip, vvip and member.
 */
public enum SpecificPatientStatus {
    
    VIP("Very Important Person"),
    VVIP("Very Very Important Person"),
    NORMAL("Normal person");
    
    private final String label;

    private SpecificPatientStatus(String label) {
        this.label = label;
    }
    
    
    
    
}
