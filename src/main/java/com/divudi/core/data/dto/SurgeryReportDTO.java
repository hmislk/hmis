package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class SurgeryReportDTO implements Serializable {

    private Long billId;
    private String mrn;
    private String patientName;
    private Date admissionDate;
    private String procedureName;
    private String otRoomName;      // c.department.name
    private String wardName;        // currentPatientRoom.roomFacilityCharge.name
    private String surgeonName;
    private String consultantName;

    // needed to join OT status in-memory after batch fetch
    private Long patientEncounterId;

    private transient String otStatus; // populated after the query, not via JPQL

    public SurgeryReportDTO(Long billId, String mrn, String patientName, Date admissionDate,
                            String procedureName, String otRoomName, String wardName,
                            String surgeonName, String consultantName, Long patientEncounterId) {
        this.billId = billId;
        this.mrn = mrn;
        this.patientName = patientName;
        this.admissionDate = admissionDate;
        this.procedureName = procedureName;
        this.otRoomName = otRoomName;
        this.wardName = wardName;
        this.surgeonName = surgeonName;
        this.consultantName = consultantName;
        this.patientEncounterId = patientEncounterId;
    }

    // getters/setters for all fields, including otStatus
    public Long getBillId() { return billId; }
    public String getMrn() { return mrn; }
    public String getPatientName() { return patientName; }
    public Date getAdmissionDate() { return admissionDate; }
    public String getProcedureName() { return procedureName; }
    public String getOtRoomName() { return otRoomName; }
    public String getWardName() { return wardName; }
    public String getSurgeonName() { return surgeonName; }
    public String getConsultantName() { return consultantName; }
    public Long getPatientEncounterId() { return patientEncounterId; }
    public String getOtStatus() { return otStatus; }
    public void setOtStatus(String otStatus) { this.otStatus = otStatus; }
}