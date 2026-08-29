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
    private Double total;
    private Double margin;
    private Double discount;
    private String departmentName;

    public InpatientPharmacyBillItemDTO() {
    }

    public InpatientPharmacyBillItemDTO(Long billId, String itemName, String itemCode, Double qty, Double netValue) {
        this.billId = billId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.netValue = netValue;
    }

    // Constructor adding the Total/Margin/Discount breakdown and issuing department
    // (Inpatient Dashboard Direct Issues report enhancement, issue #21871). Added
    // rather than modifying the original constructor per project rule (never change
    // existing constructor signatures).
    public InpatientPharmacyBillItemDTO(Long billId, String itemName, String itemCode,
            Double qty, Double total, Double margin, Double discount, Double netValue,
            String departmentName) {
        this.billId = billId;
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.qty = qty;
        this.total = total;
        this.margin = margin;
        this.discount = discount;
        this.netValue = netValue;
        this.departmentName = departmentName;
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

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}
