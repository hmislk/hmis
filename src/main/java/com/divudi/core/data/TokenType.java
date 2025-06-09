/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data;

public enum TokenType {

    APPOINTMENT_TOKEN("Appointment Token"),
    WALK_IN_TOKEN("Walk-in Token"),
    PHARMACY_TOKEN("Pharmacy Token"),
    DIAGNOSTIC_TOKEN("Diagnostic Token"),
    PRIORITY_TOKEN("Priority Token"),
    OPD_TOKEN("OPD Token"),
    SPECIAL_CLINIC_TOKEN("Special Clinic Token"),
    COUNTER_TOKEN("Counter Token"),
    PHARMACY_TOKEN_SALE_FOR_CASHIER("Pharmacy Token to sale for cashier"); //add another type to serve sale for cashier token system separately
    

    private final String label;

    TokenType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
