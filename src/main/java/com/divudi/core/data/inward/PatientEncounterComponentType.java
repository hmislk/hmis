/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */

package com.divudi.core.data.inward;

/**
 *
 * @author Buddhika
 */
public enum PatientEncounterComponentType {
    // Stored by ordinal (no @Enumerated on EncounterComponent): never reorder,
    // rename or insert constants; only append.
    Performed_By("Performed By"),
    Assisted_by("Assisted By"),
    Ananesthesia_by("Anaesthesia By"),
    Item("Item"),
    Category("Category");

    private final String label;

    PatientEncounterComponentType(String label) {
        this.label = label;
    }

    /**
     * Human-readable name for screens and printed bills.
     */
    public String getLabel() {
        return label;
    }

}

