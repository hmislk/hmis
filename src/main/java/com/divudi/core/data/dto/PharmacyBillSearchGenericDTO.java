package com.divudi.core.data.dto;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for the Pharmacy Bill Search result table used for every
 * bill type / bill type atomic that has no dedicated result table (issue
 * #24250) — adjustment sub-types, disposal issues, inpatient medicine issues,
 * donations, physical count, snapshot, etc.
 */
public class PharmacyBillSearchGenericDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private Date createdAt;
    private String creatorName;
    private String fromDepartmentName;
    private String toDepartmentName;
    private String fromInstitutionName;
    private String toInstitutionName;
    private String patientName;
    private String bhtNo;
    private Double netTotal;
    private boolean cancelled;
    private boolean refunded;
    private String comments;

    public PharmacyBillSearchGenericDTO() {
    }

    public PharmacyBillSearchGenericDTO(
            Long id,
            String deptId,
            BillType billType,
            BillTypeAtomic billTypeAtomic,
            Date createdAt,
            String creatorName,
            String fromDepartmentName,
            String toDepartmentName,
            String fromInstitutionName,
            String toInstitutionName,
            String patientName,
            String bhtNo,
            Double netTotal,
            Boolean cancelled,
            Boolean refunded,
            String comments) {
        this.id = id;
        this.deptId = deptId;
        this.billType = billType;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.creatorName = creatorName;
        this.fromDepartmentName = fromDepartmentName;
        this.toDepartmentName = toDepartmentName;
        this.fromInstitutionName = fromInstitutionName;
        this.toInstitutionName = toInstitutionName;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.netTotal = netTotal;
        this.cancelled = cancelled != null && cancelled;
        this.refunded = refunded != null && refunded;
        this.comments = comments;
    }

    /**
     * Label of the bill type atomic, falling back to the bill type for legacy
     * bills saved without an atomic.
     */
    public String getTypeLabel() {
        if (billTypeAtomic != null) {
            return billTypeAtomic.getLabel();
        }
        return billType == null ? "" : billType.getLabel();
    }

    /** Source side: department if set, otherwise institution. */
    public String getFromName() {
        return notBlank(fromDepartmentName) ? fromDepartmentName : fromInstitutionName;
    }

    /** Destination side: department if set, otherwise institution. */
    public String getToName() {
        return notBlank(toDepartmentName) ? toDepartmentName : toInstitutionName;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

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

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getFromInstitutionName() {
        return fromInstitutionName;
    }

    public void setFromInstitutionName(String fromInstitutionName) {
        this.fromInstitutionName = fromInstitutionName;
    }

    public String getToInstitutionName() {
        return toInstitutionName;
    }

    public void setToInstitutionName(String toInstitutionName) {
        this.toInstitutionName = toInstitutionName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
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

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
