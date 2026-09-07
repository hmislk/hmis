package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import java.io.Serializable;
import java.util.Date;

public class InpatientServiceIssueDTO implements Serializable {

    private Long billItemId;
    private String itemName;
    private Double qty;
    private Double netValue;
    private Date billCreatedAt;
    private BillTypeAtomic billTypeAtomic;
    private String departmentName;
    private Boolean billCancelled;

    public InpatientServiceIssueDTO() {
    }

    public InpatientServiceIssueDTO(Long billItemId, String itemName, Double qty, Double netValue,
            Date billCreatedAt, BillTypeAtomic billTypeAtomic, String departmentName,
            Boolean billCancelled) {
        this.billItemId = billItemId;
        this.itemName = itemName;
        this.qty = qty;
        this.netValue = netValue;
        this.billCreatedAt = billCreatedAt;
        this.billTypeAtomic = billTypeAtomic;
        this.departmentName = departmentName;
        this.billCancelled = billCancelled;
    }

    public Boolean isCancellation() {
        if (billTypeAtomic == null) {
            return false;
        }
        return billTypeAtomic == BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL_CANCELLATION
                || billTypeAtomic == BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL_CANCELLATION;
    }

    public Boolean getCancellation() {
        return isCancellation();
    }

    public Double getRate() {
        if (qty != null && qty != 0 && netValue != null) {
            return Math.abs(netValue / qty);
        }
        return 0.0;
    }

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
}
