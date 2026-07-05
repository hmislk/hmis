package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Item-level DTO for the encounter-scoped inpatient pharmacy direct-issue and
 * issue-return lists (Inpatient Dashboard redesign, issue #21852). Each row is
 * one BillItem projected against its parent bill id, used to build the
 * expandable per-bill item rows in the new pharmacy history pages.
 *
 * @author Claude Code
 */
public class InpatientPharmacyBillItemDTO implements Serializable {

    private Long billId;
    private String itemName;
    private String itemCode;
    private Double qty;
    private Double netValue;

    public InpatientPharmacyBillItemDTO() {
    }

    public InpatientPharmacyBillItemDTO(Long billId, String itemName, String itemCode, Double qty, Double netValue) {
        this.billId = billId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.netValue = netValue;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCode() {
        return itemCode;
    }

    public void setItemCode(String itemCode) {
        this.itemCode = itemCode;
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
}
