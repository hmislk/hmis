package com.divudi.core.data;

public enum InwardAppointmentCategory {
    ROOM_ADMISSION("Room Admission", "RA"),
    PROCEDURE("Procedure", "PR"),
    CONSULTANT("Consultant", "CN"),
    DEPARTMENT("Department", "DP"),
    CONSULTANT_AND_PROCEDURE("Consultant & Procedure", "CP"),
    CONSULTANT_AND_DEPARTMENT("Consultant & Department", "CD"),
    PROCEDURE_AND_DEPARTMENT("Procedure & Department", "PD"),
    CONSULTANT_PROCEDURE_AND_DEPARTMENT("Consultant, Procedure & Department", "CPD");

    private final String displayName;
    private final String code;

    InwardAppointmentCategory(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }

    public boolean needsRoom() {
        return this == ROOM_ADMISSION;
    }

    public boolean needsConsultant() {
        return this == CONSULTANT
                || this == CONSULTANT_AND_PROCEDURE
                || this == CONSULTANT_AND_DEPARTMENT
                || this == CONSULTANT_PROCEDURE_AND_DEPARTMENT;
    }

    public boolean needsProcedure() {
        return this == PROCEDURE
                || this == CONSULTANT_AND_PROCEDURE
                || this == PROCEDURE_AND_DEPARTMENT
                || this == CONSULTANT_PROCEDURE_AND_DEPARTMENT;
    }

    public boolean needsDepartment() {
        return this == DEPARTMENT
                || this == CONSULTANT_AND_DEPARTMENT
                || this == PROCEDURE_AND_DEPARTMENT
                || this == CONSULTANT_PROCEDURE_AND_DEPARTMENT;
    }
}
