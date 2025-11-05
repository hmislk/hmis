package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

public class InpatientPharmacyIssueDTO implements Serializable {

    private Long billItemId;
    private String itemName;
    private Double qty;
    private Double netValue;
    private Date billCreatedAt;
    private BillTypeAtomic billTypeAtomic;
    private String departmentName;
    private Boolean billCancelled;
    private Long referenceBillItemId;

    public InpatientPharmacyIssueDTO() {
    }

    public InpatientPharmacyIssueDTO(Long billItemId, String itemName, Double qty, Double netValue,
            Date billCreatedAt, BillTypeAtomic billTypeAtomic, String departmentName,
            Boolean billCancelled, Long referenceBillItemId) {
        this.billItemId = billItemId;
        this.itemName = itemName;
        this.qty = qty;
        this.netValue = netValue;
        this.billCreatedAt = billCreatedAt;
        this.billTypeAtomic = billTypeAtomic;
        this.departmentName = departmentName;
        this.billCancelled = billCancelled;
        this.referenceBillItemId = referenceBillItemId;
    }

    public InpatientPharmacyIssueDTO(Long billItemId, String itemName, Double qty, Double netValue,
            Date billCreatedAt, BillTypeAtomic billTypeAtomic, String departmentName,
            Long referenceBillItemId) {
        this.billItemId = billItemId;
        this.itemName = itemName;
        this.qty = qty;
        this.netValue = netValue;
        this.billCreatedAt = billCreatedAt;
        this.billTypeAtomic = billTypeAtomic;
        this.departmentName = departmentName;
        this.referenceBillItemId = referenceBillItemId;
    }

    // Calculated field for rate
    public Double getRate() {
        if (qty != null && qty != 0 && netValue != null) {
            return Math.abs(netValue / qty);
        }
        return 0.0;
    }

    // Helper method to determine if this is a return/cancellation
    public Boolean isCancellationOrReturn() {
        if (billTypeAtomic == null) {
            return false;
        }
        return billTypeAtomic == BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED
                || billTypeAtomic == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN
                || billTypeAtomic == BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN;
    }

    // JavaBeans property getter for JSF access
    public Boolean getCancellationOrReturn() {
        return isCancellationOrReturn();
    }

    // Getters and setters
    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Date getBillCreatedAt() {
        return billCreatedAt;
    }

    public void setBillCreatedAt(Date billCreatedAt) {
        this.billCreatedAt = billCreatedAt;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Boolean getBillCancelled() {
        return billCancelled;
    }

    public void setBillCancelled(Boolean billCancelled) {
        this.billCancelled = billCancelled;
    }

    public Long getReferenceBillItemId() {
        return referenceBillItemId;
    }

    public void setReferenceBillItemId(Long referenceBillItemId) {
        this.referenceBillItemId = referenceBillItemId;
    }
}
