package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Patient Deposit Bill search results.
 * Used for optimized queries without loading full entity graphs.
 *
 * @author Dr M H B Ariyaratne
 */
public class PatientDepositBillDto implements Serializable {

    private Long id;
    private String deptId;
    private Date createdAt;
    private Double netTotal;
    private String comments;

    // Patient info
    private Long patientId;
    private String patientName;
    private String patientPhn;

    // Creator info
    private Long createrId;
    private String createrName;

    // Location info
    private Long departmentId;
    private String departmentName;
    private Long siteId;
    private String siteName;
    private Long institutionId;
    private String institutionName;

    // Bill status
    private boolean cancelled;
    private boolean refunded;

    // Cancelled bill info
    private Long cancelledBillId;
    private String cancelledBillDeptId;
    private Date cancelledBillCreatedAt;
    private String cancelledBillCreaterName;
    private String cancelledBillComments;

    // Refunded bill info
    private String refundedBillComments;

    public PatientDepositBillDto() {
    }

    // Constructor for JPQL projection - basic fields without cancelled/refunded details
    public PatientDepositBillDto(Long id, String deptId, Date createdAt, Double netTotal, String comments,
            Long patientId, String patientName, String patientPhn,
            Long createrId, String createrName,
            Long departmentId, String departmentName,
            Long siteId, String siteName,
            Long institutionId, String institutionName,
            boolean cancelled, boolean refunded) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.comments = comments;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.createrId = createrId;
        this.createrName = createrName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.cancelled = cancelled;
        this.refunded = refunded;
    }

    // Constructor with cancelled bill details
    public PatientDepositBillDto(Long id, String deptId, Date createdAt, Double netTotal, String comments,
            Long patientId, String patientName, String patientPhn,
            Long createrId, String createrName,
            Long departmentId, String departmentName,
            Long siteId, String siteName,
            Long institutionId, String institutionName,
            boolean cancelled, boolean refunded,
            Long cancelledBillId, String cancelledBillDeptId, Date cancelledBillCreatedAt,
            String cancelledBillCreaterName, String cancelledBillComments,
            String refundedBillComments) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.comments = comments;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.createrId = createrId;
        this.createrName = createrName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.cancelledBillId = cancelledBillId;
        this.cancelledBillDeptId = cancelledBillDeptId;
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
        this.cancelledBillCreaterName = cancelledBillCreaterName;
        this.cancelledBillComments = cancelledBillComments;
        this.refundedBillComments = refundedBillComments;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
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

    public Long getCreaterId() {
        return createrId;
    }

    public void setCreaterId(Long createrId) {
        this.createrId = createrId;
    }

    public String getCreaterName() {
        return createrName;
    }

    public void setCreaterName(String createrName) {
        this.createrName = createrName;
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

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isRefunded() {
        return refunded;
    }

    public void setRefunded(boolean refunded) {
        this.refunded = refunded;
    }

    public Long getCancelledBillId() {
        return cancelledBillId;
    }

    public void setCancelledBillId(Long cancelledBillId) {
        this.cancelledBillId = cancelledBillId;
    }

    public String getCancelledBillDeptId() {
        return cancelledBillDeptId;
    }

    public void setCancelledBillDeptId(String cancelledBillDeptId) {
        this.cancelledBillDeptId = cancelledBillDeptId;
    }

    public Date getCancelledBillCreatedAt() {
        return cancelledBillCreatedAt;
    }

    public void setCancelledBillCreatedAt(Date cancelledBillCreatedAt) {
        this.cancelledBillCreatedAt = cancelledBillCreatedAt;
    }

    public String getCancelledBillCreaterName() {
        return cancelledBillCreaterName;
    }

    public void setCancelledBillCreaterName(String cancelledBillCreaterName) {
        this.cancelledBillCreaterName = cancelledBillCreaterName;
    }

    public String getCancelledBillComments() {
        return cancelledBillComments;
    }

    public void setCancelledBillComments(String cancelledBillComments) {
        this.cancelledBillComments = cancelledBillComments;
    }

    public String getRefundedBillComments() {
        return refundedBillComments;
    }

    public void setRefundedBillComments(String refundedBillComments) {
        this.refundedBillComments = refundedBillComments;
    }
}
