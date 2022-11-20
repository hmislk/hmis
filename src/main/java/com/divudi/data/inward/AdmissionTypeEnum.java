/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.data.inward;

/**
 *
 * @author safrin
 */
public enum AdmissionTypeEnum {
    Admission("Inward Stay"),
    DayCase("Day Case");
    
    private final String label;

    public String getLabel() {
        return label;
    }

    AdmissionTypeEnum(String label) {
        this.label = label;
    }
    
    
}
