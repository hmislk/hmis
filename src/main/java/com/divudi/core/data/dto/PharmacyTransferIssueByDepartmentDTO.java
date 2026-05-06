package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;

/**
 * DTO for Pharmacy Transfer Issue grouped by Transferred out department
 * Used in pharmacy history item details tab
 *
 * @author Claude Code
 */
public class PharmacyTransferIssueByDepartmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Department toDepartment;
    private Double quantity;

    public PharmacyTransferIssueByDepartmentDTO() {
    }

    public PharmacyTransferIssueByDepartmentDTO(Department toDepartment, Double quantity) {
        this.toDepartment = toDepartment;
        this.quantity = quantity;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PharmacyTransferIssueByDepartmentDTO{" +
                "toDepartment=" + toDepartment +
                ", quantity=" + quantity +
                '}';
    }
}