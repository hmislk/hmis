package com.divudi.core.data.dto;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PharmacyTransferIssuedListDTO implements Serializable {

    private Long billId;
    private String deptId;
    private String fromDepartmentName;
    private DepartmentType departmentType;
    private String issuerName;
    private String staffName;
    private Date createdAt;
    private Boolean cancelled;
    private Boolean fullyIssued;
    private String cancelledByName;
    private Date cancelledAt;
    private List<PharmacyTransferReceivedListDTO> receivedBills = new ArrayList<>();
    private Long pendingReceiveBillId;
    private Boolean pendingReceiveIsFinalized;

    public PharmacyTransferIssuedListDTO() {
    }

    public PharmacyTransferIssuedListDTO(Long billId, String deptId, String fromDepartmentName,
            DepartmentType departmentType, String issuerName, String staffName,
            Date createdAt, Boolean cancelled, Boolean fullyIssued,
            String cancelledByName, Date cancelledAt) {
        this.billId = billId;
        this.deptId = deptId;
        this.fromDepartmentName = fromDepartmentName;
        this.departmentType = departmentType;
        this.issuerName = issuerName;
        this.staffName = staffName;
        this.createdAt = createdAt;
        this.cancelled = cancelled != null ? cancelled : false;
        this.fullyIssued = fullyIssued != null ? fullyIssued : false;
        this.cancelledByName = cancelledByName;
        this.cancelledAt = cancelledAt;
    }

    public String getDepartmentTypeLabel() {
        return departmentType != null ? departmentType.getLabel() : null;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Boolean getFullyIssued() {
        return fullyIssued;
    }

    public void setFullyIssued(Boolean fullyIssued) {
        this.fullyIssued = fullyIssued;
    }

    public String getCancelledByName() {
        return cancelledByName;
    }

    public void setCancelledByName(String cancelledByName) {
        this.cancelledByName = cancelledByName;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<PharmacyTransferReceivedListDTO> getReceivedBills() {
        return receivedBills;
    }

    public void setReceivedBills(List<PharmacyTransferReceivedListDTO> receivedBills) {
        this.receivedBills = receivedBills;
    }

    public Long getPendingReceiveBillId() {
        return pendingReceiveBillId;
    }

    public void setPendingReceiveBillId(Long pendingReceiveBillId) {
        this.pendingReceiveBillId = pendingReceiveBillId;
    }

    public Boolean getPendingReceiveIsFinalized() {
        return pendingReceiveIsFinalized != null && pendingReceiveIsFinalized;
    }

    public void setPendingReceiveIsFinalized(Boolean pendingReceiveIsFinalized) {
        this.pendingReceiveIsFinalized = pendingReceiveIsFinalized;
    }
}
