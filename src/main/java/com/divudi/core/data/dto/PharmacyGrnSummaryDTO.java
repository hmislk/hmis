package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight GRN-row projection nested under a PO row on the DTO/optimized
 * "Create GRN from PO" page (pharmacy_purchase_order_list_for_recieve_dto.xhtml).
 * Mirrors what the legacy page shows via Bill.listOfBill, without loading the
 * full Bill entity graph.
 */
public class PharmacyGrnSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private String deptId;
    private Date createdAt;
    private Double netTotal;
    private String billTypeAtomic;
    private Boolean cancelled;
    private Boolean refunded;

    public PharmacyGrnSummaryDTO(
            Long billId,
            String deptId,
            Date createdAt,
            Double netTotal,
            Object billTypeAtomic,
            Object cancelled,
            Object refunded) {
        this.billId = billId;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.netTotal = netTotal;
        this.billTypeAtomic = billTypeAtomic != null ? billTypeAtomic.toString() : null;
        this.cancelled = cancelled != null ? (Boolean) cancelled : false;
        this.refunded = refunded != null ? (Boolean) refunded : false;
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

    public String getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(String billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }
}
