package com.divudi.core.data.clinical;

/**
 * Columns that can appear in the rows table of a covering letter. The
 * declaration order is the canonical render order.
 */
public enum CoveringLetterColumn {
    REF_NO("Ref No"),
    DATE("Date"),
    PATIENT("Patient"),
    POLICY_NO("Policy No"),
    AMOUNT("Bill Amount"),
    COMPANY_AMOUNT("Company Amount");

    private final String label;

    private CoveringLetterColumn(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
