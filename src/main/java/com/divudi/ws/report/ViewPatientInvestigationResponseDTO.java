package com.divudi.ws.report;

import com.divudi.core.entity.lab.PatientInvestigation;

import java.util.Date;
import java.util.List;

public class ViewPatientInvestigationResponseDTO {
    private Long id;
    private Date createdAt;

    private Date sampleAcceptedAt;
    private Date sampleGeneratedAt;

    private Date sampledAt;
    private Boolean sampleOutside;
    private String sampleComments;
    private String sampleDepartment;
    private String sampleInstitution;

    private Date receivedAt;
    private String receivedDepartment;
    private String receivedInstitution;
    private String receivedComments;

    private Date performedAt;
    private String performedDepartment;
    private String performedInstitution;
    private String performedComments;

    private Date approvedAt;
    private String approvedDepartment;
    private String approvedInstitution;
    private String approvedComments;

    private Boolean outsourced;
    private String outsourcedInstitution;
    private String outsourcedComments;
    private String outsourcedDepartment;

    private ViewInvestigationResponseDTO investigation;

    private List<ViewReportResponseDTO> reports;

    public static ViewPatientInvestigationResponseDTO fromPatientInvestigation(
            final PatientInvestigation patientInvestigation) {

        ViewPatientInvestigationResponseDTO dto = new ViewPatientInvestigationResponseDTO();

        if (patientInvestigation == null) {
            return dto;
        }

        dto.id = patientInvestigation.getId();
        dto.createdAt = patientInvestigation.getCreatedAt();

        dto.sampleAcceptedAt = patientInvestigation.getSampleAcceptedAt();
        dto.sampleGeneratedAt = patientInvestigation.getSampleGeneratedAt();
        dto.sampledAt = patientInvestigation.getSampledAt();
        dto.sampleOutside = patientInvestigation.getSampleOutside();
        dto.sampleComments = patientInvestigation.getSampleComments();

        dto.sampleDepartment = patientInvestigation.getSampleDepartment() != null
                ? patientInvestigation.getSampleDepartment().getName()
                : null;
        dto.sampleInstitution = patientInvestigation.getSampleInstitution() != null
                ? patientInvestigation.getSampleInstitution().getName()
                : null;

        dto.receivedAt = patientInvestigation.getReceivedAt();
        dto.receivedDepartment = patientInvestigation.getReceiveDepartment() != null
                ? patientInvestigation.getReceiveDepartment().getName()
                : null;
        dto.receivedInstitution = patientInvestigation.getReceiveInstitution() != null
                ? patientInvestigation.getReceiveInstitution().getName()
                : null;
        dto.receivedComments = patientInvestigation.getReceiveComments();

        dto.performedAt = patientInvestigation.getPerformedAt();
        dto.performedDepartment = patientInvestigation.getPerformDepartment() != null
                ? patientInvestigation.getPerformDepartment().getName()
                : null;
        dto.performedInstitution = patientInvestigation.getPerformInstitution() != null
                ? patientInvestigation.getPerformInstitution().getName()
                : null;
        dto.performedComments = patientInvestigation.getPerformComments();

        dto.approvedAt = patientInvestigation.getApproveAt();
        dto.approvedDepartment = patientInvestigation.getApproveDepartment() != null
                ? patientInvestigation.getApproveDepartment().getName()
                : null;
        dto.approvedInstitution = patientInvestigation.getApproveInstitution() != null
                ? patientInvestigation.getApproveInstitution().getName()
                : null;
        dto.approvedComments = patientInvestigation.getApproveComments();

        dto.outsourced = patientInvestigation.getOutsourced();
        dto.outsourcedInstitution = patientInvestigation.getOutsourcedInstitution() != null
                ? patientInvestigation.getOutsourcedInstitution().getName()
                : null;
        dto.outsourcedComments = patientInvestigation.getOutsourcedComments();
        dto.outsourcedDepartment = patientInvestigation.getOutsourcedDepartment() != null
                ? patientInvestigation.getOutsourcedDepartment().getName()
                : null;

        dto.investigation = patientInvestigation.getInvestigation() != null
                ? new ViewInvestigationResponseDTO(patientInvestigation.getInvestigation())
                : null;

        dto.reports = ViewReportResponseDTO.fromPatientReports(patientInvestigation.getPatientReports());

        return dto;
    }

    public static List<ViewPatientInvestigationResponseDTO> fromPatientInvestigations(final List<PatientInvestigation> patientInvestigations) {
        return patientInvestigations.stream()
                .map(ViewPatientInvestigationResponseDTO::fromPatientInvestigation)
                .collect(java.util.stream.Collectors.toList());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getSampleAcceptedAt() {
        return sampleAcceptedAt;
    }

    public void setSampleAcceptedAt(Date sampleAcceptedAt) {
        this.sampleAcceptedAt = sampleAcceptedAt;
    }

    public Date getSampleGeneratedAt() {
        return sampleGeneratedAt;
    }

    public void setSampleGeneratedAt(Date sampleGeneratedAt) {
        this.sampleGeneratedAt = sampleGeneratedAt;
    }

    public Date getSampledAt() {
        return sampledAt;
    }

    public void setSampledAt(Date sampledAt) {
        this.sampledAt = sampledAt;
    }

    public Boolean getSampleOutside() {
        return sampleOutside;
    }

    public void setSampleOutside(Boolean sampleOutside) {
        this.sampleOutside = sampleOutside;
    }

    public String getSampleComments() {
        return sampleComments;
    }

    public void setSampleComments(String sampleComments) {
        this.sampleComments = sampleComments;
    }

    public String getSampleDepartment() {
        return sampleDepartment;
    }

    public void setSampleDepartment(String sampleDepartment) {
        this.sampleDepartment = sampleDepartment;
    }

    public String getSampleInstitution() {
        return sampleInstitution;
    }

    public void setSampleInstitution(String sampleInstitution) {
        this.sampleInstitution = sampleInstitution;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getReceivedDepartment() {
        return receivedDepartment;
    }

    public void setReceivedDepartment(String receivedDepartment) {
        this.receivedDepartment = receivedDepartment;
    }

    public String getReceivedInstitution() {
        return receivedInstitution;
    }

    public void setReceivedInstitution(String receivedInstitution) {
        this.receivedInstitution = receivedInstitution;
    }

    public String getReceivedComments() {
        return receivedComments;
    }

    public void setReceivedComments(String receivedComments) {
        this.receivedComments = receivedComments;
    }

    public Date getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Date performedAt) {
        this.performedAt = performedAt;
    }

    public String getPerformedDepartment() {
        return performedDepartment;
    }

    public void setPerformedDepartment(String performedDepartment) {
        this.performedDepartment = performedDepartment;
    }

    public String getPerformedInstitution() {
        return performedInstitution;
    }

    public void setPerformedInstitution(String performedInstitution) {
        this.performedInstitution = performedInstitution;
    }

    public String getPerformedComments() {
        return performedComments;
    }

    public void setPerformedComments(String performedComments) {
        this.performedComments = performedComments;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApprovedDepartment() {
        return approvedDepartment;
    }

    public void setApprovedDepartment(String approvedDepartment) {
        this.approvedDepartment = approvedDepartment;
    }

    public String getApprovedInstitution() {
        return approvedInstitution;
    }

    public void setApprovedInstitution(String approvedInstitution) {
        this.approvedInstitution = approvedInstitution;
    }

    public String getApprovedComments() {
        return approvedComments;
    }

    public void setApprovedComments(String approvedComments) {
        this.approvedComments = approvedComments;
    }

    public Boolean getOutsourced() {
        return outsourced;
    }

    public void setOutsourced(Boolean outsourced) {
        this.outsourced = outsourced;
    }

    public String getOutsourcedInstitution() {
        return outsourcedInstitution;
    }

    public void setOutsourcedInstitution(String outsourcedInstitution) {
        this.outsourcedInstitution = outsourcedInstitution;
    }

    public String getOutsourcedComments() {
        return outsourcedComments;
    }

    public void setOutsourcedComments(String outsourcedComments) {
        this.outsourcedComments = outsourcedComments;
    }

    public String getOutsourcedDepartment() {
        return outsourcedDepartment;
    }

    public void setOutsourcedDepartment(String outsourcedDepartment) {
        this.outsourcedDepartment = outsourcedDepartment;
    }

    public ViewInvestigationResponseDTO getInvestigation() {
        return investigation;
    }

    public void setInvestigation(ViewInvestigationResponseDTO investigation) {
        this.investigation = investigation;
    }

    public List<ViewReportResponseDTO> getReports() {
        return reports;
    }

    public void setReports(List<ViewReportResponseDTO> reports) {
        this.reports = reports;
    }
}
