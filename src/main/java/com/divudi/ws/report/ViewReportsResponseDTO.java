package com.divudi.ws.report;

import com.divudi.core.entity.lab.PatientInvestigation;

import java.util.Date;
import java.util.List;

public class ViewReportsResponseDTO {
    private String nic;
    private String mobile;
    private String name;
    private String phn;
    private Date dateOfBirth;
    private String title;
    private String sex;
    private String bloodGroup;
    private String fromDate;
    private String toDate;
    private List<ViewPatientInvestigationResponseDTO> patientInvestigations;

    public ViewReportsResponseDTO(String mobile, String fromDate, String toDate, List<PatientInvestigation> patientInvestigations) {
        this.nic = !patientInvestigations.isEmpty() ? patientInvestigations.get(0).getPatient().getPerson().getNic() : null;
        this.mobile = mobile;
        this.name = !patientInvestigations.isEmpty() ? patientInvestigations.get(0).getPatient().getPerson().getName() : null;
        this.phn = !patientInvestigations.isEmpty() ? patientInvestigations.get(0).getPatient().getPhn() : null;
        this.dateOfBirth = !patientInvestigations.isEmpty() ? patientInvestigations.get(0).getPatient().getPerson().getDob() : null;
        this.title = !patientInvestigations.isEmpty() ? patientInvestigations.get(0).getPatient().getPerson().getTitle().name() : null;
        this.sex = !patientInvestigations.isEmpty() ? patientInvestigations.get(0).getPatient().getPerson().getSex().name() : null;
        this.bloodGroup = !patientInvestigations.isEmpty() && patientInvestigations.get(0).getPatient().getPerson().getBloodGroup() != null
                ? patientInvestigations.get(0).getPatient().getPerson().getBloodGroup().getName() : null;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.patientInvestigations = ViewPatientInvestigationResponseDTO.fromPatientInvestigations(patientInvestigations);
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public List<ViewPatientInvestigationResponseDTO> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<ViewPatientInvestigationResponseDTO> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }
}
