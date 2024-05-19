/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data;

public enum TokenType {

    APPOINTMENT_TOKEN("Appointment Token"),
    WALK_IN_TOKEN("Walk-in Token"),
    PHARMACY_TOKEN("Pharmacy Token"),
    DIAGNOSTIC_TOKEN("Diagnostic Token"),
    PRIORITY_TOKEN("Priority Token"),
    OPD_TOKEN("OPD Token"),
    SPECIAL_CLINIC_TOKEN("Special Clinic Token"),
    COUNTER_TOKEN("Counter Token");

    private final String label;

    TokenType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
