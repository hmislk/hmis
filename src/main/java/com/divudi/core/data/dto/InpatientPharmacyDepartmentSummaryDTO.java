package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Per-issuing-department aggregate across all direct-issue and issue-return
 * bills in one patient encounter (Inpatient Dashboard Direct Issues report,
 * issue #21871). Built by in-memory accumulation over
 * InpatientPharmacyBillItemDTO rows, not a JPQL projection.
 *
 * @author Claude Code
 */
public class InpatientPharmacyDepartmentSummaryDTO implements Serializable {

    private String departmentName;
    private double qty;
    private double total;
    private double margin;
    private double discount;
    private double netTotal;

    public InpatientPharmacyDepartmentSummaryDTO() {
    }

    public InpatientPharmacyDepartmentSummaryDTO(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getMargin() {
        return margin;
    }

    public void setMargin(double margin) {
        this.margin = margin;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }
}
