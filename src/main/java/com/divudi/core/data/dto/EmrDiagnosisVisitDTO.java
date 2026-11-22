package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * One row of the EMR visits by diagnosis drill-down: an OPD visit with one
 * diagnosis recorded on it and the presenting complaints of the visit.
 */
public class EmrDiagnosisVisitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long visitId;
    private Date visitDate;
    private String patientName;
    private String phn;
    private String departmentName;
    private String doctorName;
    private String diagnosisName;
    private String presentingComplaints;

    public EmrDiagnosisVisitDTO() {
    }

    public EmrDiagnosisVisitDTO(Long visitId, Date visitDate, String patientName, String phn,
            String departmentName, String doctorName, String diagnosisName,
            String presentingComplaints) {
        this.visitId = visitId;
        this.visitDate = visitDate;
        this.patientName = patientName;
        this.phn = phn;
        this.departmentName = departmentName;
        this.doctorName = doctorName;
        this.diagnosisName = diagnosisName;
        this.presentingComplaints = presentingComplaints;
    }

    public Long getVisitId() {
        return visitId;
    }

    public void setVisitId(Long visitId) {
        this.visitId = visitId;
    }

    public Date getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }

    public String getPresentingComplaints() {
        return presentingComplaints;
    }

    public void setPresentingComplaints(String presentingComplaints) {
        this.presentingComplaints = presentingComplaints;
    }
}
