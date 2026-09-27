package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
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
    private Title title;

    // needed to join OT status in-memory after batch fetch
    private Long patientEncounterId;

    private transient String otStatus; // populated after the query, not via JPQL
    
    private Long procedureId;

    public SurgeryReportDTO(Long billId, String mrn, String patientName, Date admissionDate,
                            String procedureName, String otRoomName, String wardName,
                            String surgeonName, Title title, String consultantName, Long patientEncounterId) {
        this.billId = billId;
        this.mrn = mrn;
        this.patientName = patientName;
        this.admissionDate = admissionDate;
        this.procedureName = procedureName;
        this.otRoomName = otRoomName;
        this.wardName = wardName;
        this.surgeonName = surgeonName;
        this.title = title;
        this.consultantName = consultantName;
        this.patientEncounterId = patientEncounterId;
    }

    public SurgeryReportDTO(Long billId, String mrn, String patientName, Date admissionDate,
                            String procedureName, String otRoomName, String wardName,
                            String surgeonName, Title title, String consultantName, Long patientEncounterId, Long procedureId) {
        this(billId, mrn, patientName, admissionDate, procedureName, otRoomName, wardName, surgeonName, title, consultantName, patientEncounterId);
        this.procedureId = procedureId;
    }

    // Adds the surgeon's own title (b.staff's title), so the "Surgeon" column
    // still shows "Dr <name>" when no Performed_By/Assisted_by EncounterComponent
    // exists for this procedure and surgeonName is left at its raw bill-staff
    // fallback (the enrichment path in SurgeryReportController already bakes the
    // title into surgeonName itself when it finds EncounterComponent rows).
    public SurgeryReportDTO(Long billId, String mrn, String patientName, Date admissionDate,
                            String procedureName, String otRoomName, String wardName,
                            String surgeonName, Title title, String consultantName, Long patientEncounterId, Long procedureId,
                            Title surgeonTitle) {
        this(billId, mrn, patientName, admissionDate, procedureName, otRoomName, wardName, surgeonName, title, consultantName, patientEncounterId, procedureId);
        if (surgeonTitle != null && surgeonName != null && !surgeonName.trim().isEmpty()) {
            this.surgeonName = surgeonTitle.getLabel().trim() + " " + surgeonName.trim();
        }
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
    public void setSurgeonName(String surgeonName) { this.surgeonName = surgeonName; }
    public String getConsultantName() { return consultantName; }
    public Long getPatientEncounterId() { return patientEncounterId; }
    public String getOtStatus() { return otStatus; }
    public void setOtStatus(String otStatus) { this.otStatus = otStatus; }
    public Long getProcedureId() { return procedureId; }
    public void setProcedureId(Long procedureId) { this.procedureId = procedureId; }

    public void setOtRoomName(String otRoomName) {
        this.otRoomName = otRoomName;
    }

    public Title getTitle() {
        return title;
    }
}