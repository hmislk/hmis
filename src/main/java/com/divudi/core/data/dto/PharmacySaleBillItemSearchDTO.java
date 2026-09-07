package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for pharmacy retail sale bill item search.
 * Used in pharmacy/pharmacy_search_sale_bill_item.xhtml.
 */
public class PharmacySaleBillItemSearchDTO implements Serializable {

    private Long billId;
    private String deptId;
    private Date billDate;
    private Long itemId;
    private String itemName;
    private String itemCode;
    private Double qty;
    private Double netValue;
    private String patientName;
    private Boolean cancelled;
    private Boolean refunded;

    public PharmacySaleBillItemSearchDTO() {
    }

    public PharmacySaleBillItemSearchDTO(
            Long billId,
            String deptId,
            Date billDate,
            Long itemId,
            String itemName,
            String itemCode,
            Double qty,
            Double netValue,
            String patientName,
            Boolean cancelled,
            Boolean refunded) {
        this.billId = billId;
        this.deptId = deptId;
        this.billDate = billDate;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.netValue = netValue;
        this.patientName = patientName;
        this.cancelled = cancelled;
        this.refunded = refunded;
    }

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }

    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }

    public Date getBillDate() { return billDate; }
    public void setBillDate(Date billDate) { this.billDate = billDate; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public Double getQty() { return qty; }
    public void setQty(Double qty) { this.qty = qty; }

    public Double getNetValue() { return netValue; }
    public void setNetValue(Double netValue) { this.netValue = netValue; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }

    public Boolean getRefunded() { return refunded; }
    public void setRefunded(Boolean refunded) { this.refunded = refunded; }
}
