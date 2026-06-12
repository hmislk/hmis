package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;

/**
 * DTO for consolidated department-wise view of Retail Sale, Wholesale Sale,
 * and Inpatient Issue quantities. Used in the pharmacy item history composite
 * component (history.xhtml) — new "Department Sale &amp; Issue" tab and block.
 *
 * All quantity fields store the absolute (display) value — the sign has already
 * been reverted from the negative PharmaceuticalBillItem.qty values that
 * represent outgoing stock movements.
 *
 * @author Dr M H B Ariyaratne
 */
public class DepartmentSaleIssueDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Department department;
    private Double retailSaleQty;
    private Double wholesaleSaleQty;
    private Double inpatientIssueQty;

    public DepartmentSaleIssueDTO() {
    }

    public DepartmentSaleIssueDTO(Department department, Double retailSaleQty,
            Double wholesaleSaleQty, Double inpatientIssueQty) {
        this.department = department;
        this.retailSaleQty = retailSaleQty;
        this.wholesaleSaleQty = wholesaleSaleQty;
        this.inpatientIssueQty = inpatientIssueQty;
    }

    /**
     * Returns the total quantity across all three categories (display values).
     */
    public Double getTotalQty() {
        double total = 0.0;
        if (retailSaleQty != null) {
            total += retailSaleQty;
        }
        if (wholesaleSaleQty != null) {
            total += wholesaleSaleQty;
        }
        if (inpatientIssueQty != null) {
            total += inpatientIssueQty;
        }
        return total;
    }

    // --- Getters and Setters ---

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Double getRetailSaleQty() {
        return retailSaleQty;
    }

    public void setRetailSaleQty(Double retailSaleQty) {
        this.retailSaleQty = retailSaleQty;
    }

    public Double getWholesaleSaleQty() {
        return wholesaleSaleQty;
    }

    public void setWholesaleSaleQty(Double wholesaleSaleQty) {
        this.wholesaleSaleQty = wholesaleSaleQty;
    }

    public Double getInpatientIssueQty() {
        return inpatientIssueQty;
    }

    public void setInpatientIssueQty(Double inpatientIssueQty) {
        this.inpatientIssueQty = inpatientIssueQty;
    }

    @Override
    public String toString() {
        return "DepartmentSaleIssueDTO{"
                + "department=" + (department != null ? department.getName() : "null")
                + ", retailSaleQty=" + retailSaleQty
                + ", wholesaleSaleQty=" + wholesaleSaleQty
                + ", inpatientIssueQty=" + inpatientIssueQty
                + ", totalQty=" + getTotalQty()
                + '}';
    }
}
