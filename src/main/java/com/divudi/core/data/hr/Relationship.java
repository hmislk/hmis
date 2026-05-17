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

    NextOfKin("Next of Kin"),
    Guardian("Guardian"),
    Dependent("Dependent"),
    Spouse("Spouse"),
    Parent("Parent"),
    Child("Child"),
    Sibling("Sibling"),
    EmergencyContact("Emergency Contact"),
    Other("Other");

    private final String label;

    Relationship(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
