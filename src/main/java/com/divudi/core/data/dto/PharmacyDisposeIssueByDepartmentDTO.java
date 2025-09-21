package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;

/**
 * DTO for Pharmacy Dispose Issue grouped by Issue department
 * Used in pharmacy history item details tab
 *
 * @author Claude Code
 */
public class PharmacyDisposeIssueByDepartmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Department issueDepartment;
    private Double quantity;

    public PharmacyDisposeIssueByDepartmentDTO() {
    }

    public PharmacyDisposeIssueByDepartmentDTO(Department issueDepartment, Double quantity) {
        this.issueDepartment = issueDepartment;
        this.quantity = quantity;
    }

    public Department getIssueDepartment() {
        return issueDepartment;
    }

    public void setIssueDepartment(Department issueDepartment) {
        this.issueDepartment = issueDepartment;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PharmacyDisposeIssueByDepartmentDTO{" +
                "issueDepartment=" + issueDepartment +
                ", quantity=" + quantity +
                '}';
    }
}