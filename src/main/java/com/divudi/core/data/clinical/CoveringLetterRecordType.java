package com.divudi.core.data.clinical;

public enum CoveringLetterRecordType {
    Admission("Admissions"),
    OpdBill("OPD Credit Bills");

    private final String label;

    private CoveringLetterRecordType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
