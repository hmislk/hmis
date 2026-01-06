package com.divudi.core.data;

/**
- *
- * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
- *
*/

public enum ReportType {
    GENARATE("Genarate"),
    INTERFACE("Interface"),
    UPLOAD("Upload"),
    OTHER("Other");

    private final String label;

    ReportType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
