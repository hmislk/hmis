/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.hr;

/**
 *
 * @author buddhika
 */
public enum Relationship {

    NextOfKin("Next of Kin", "Next of Kin"),
    Guardian("Guardian", "Dependent"),
    Dependent("Dependent", "Guardian"),
    Spouse("Spouse", "Spouse"),
    Parent("Parent", "Child"),
    Child("Child", "Parent"),
    Sibling("Sibling", "Sibling"),
    EmergencyContact("Emergency Contact", "Emergency Contact"),
    Other("Other", "Other");

    private final String label;
    private final String inverseLabel;

    Relationship(String label, String inverseLabel) {
        this.label = label;
        this.inverseLabel = inverseLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getInverseLabel() {
        return inverseLabel;
    }
}
