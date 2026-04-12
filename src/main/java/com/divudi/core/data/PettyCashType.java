package com.divudi.core.data;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */


public enum PettyCashType{

    STAFF("Staff"),
    DEPARTMENT("Department"),
    PERSON("Person"),
    NEWPERSON("New Person");
    
    private final String label;

    PettyCashType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
