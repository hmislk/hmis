package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for Patient Deposit Balance Report.
 * Shows the last deposit balance for each patient/department combination
 * as of a specific date.
 *
 * @author Dr M H B Ariyaratne
 */
public class PatientDepositBalanceDto implements Serializable {

    private Long patientDepositHistoryId;

    // Patient info
    private Long patientId;
    private String patientName;
    private String patientPhn;

    // Location info
    private Long departmentId;
    private String departmentName;
    private Long siteId;
    private String siteName;
    private Long institutionId;
    private String institutionName;

    // Balance (from the last history record's balanceAfterTransaction)
    private Double balance;

    public PatientDepositBalanceDto() {
    }

    // Constructor for JPQL projection
    public PatientDepositBalanceDto(Long patientDepositHistoryId,
            Long patientId, String patientName, String patientPhn,
            Long departmentId, String departmentName,
            Long siteId, String siteName,
            Long institutionId, String institutionName,
            Double balance) {
        this.patientDepositHistoryId = patientDepositHistoryId;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.balance = balance;
    }

    // Getters and Setters
    public Long getPatientDepositHistoryId() {
        return patientDepositHistoryId;
    }

    public void setPatientDepositHistoryId(Long patientDepositHistoryId) {
        this.patientDepositHistoryId = patientDepositHistoryId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public void setPatientPhn(String patientPhn) {
        this.patientPhn = patientPhn;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
