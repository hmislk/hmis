package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Department;
import java.io.Serializable;

/**
 * DTO for a single Department / Bill Type sale quantity row.
 * Used in the Department Wise Sale block of the pharmacy history item details
 * panel, where rows are grouped by department and then by bill type.
 *
 * The quantity is the stored stock movement negated, so a sale reads positive
 * and a cancellation or return reads negative - letting reversals subtract
 * when the rows are totalled.
 *
 * @author Claude Code
 */
public class PharmacyDepartmentWiseSaleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Department department;
    private BillTypeAtomic billTypeAtomic;
    private Double quantity;

    public PharmacyDepartmentWiseSaleDTO() {
    }

    public PharmacyDepartmentWiseSaleDTO(Department department, BillTypeAtomic billTypeAtomic, Double quantity) {
        this.department = department;
        this.billTypeAtomic = billTypeAtomic;
        this.quantity = quantity;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PharmacyDepartmentWiseSaleDTO{"
                + "department=" + (department == null ? null : department.getName())
                + ", billTypeAtomic=" + billTypeAtomic
                + ", quantity=" + quantity
                + '}';
    }
}
