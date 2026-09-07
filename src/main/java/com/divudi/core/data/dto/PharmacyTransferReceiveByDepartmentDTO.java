package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;

/**
 * DTO for Pharmacy Transfer Receive grouped by Send from department
 * Used in pharmacy history item details tab
 *
 * @author Claude Code
 */
public class PharmacyTransferReceiveByDepartmentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Department fromDepartment;
    private Double quantity;

    public PharmacyTransferReceiveByDepartmentDTO() {
    }

    public PharmacyTransferReceiveByDepartmentDTO(Department fromDepartment, Double quantity) {
        this.fromDepartment = fromDepartment;
        this.quantity = quantity;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PharmacyTransferReceiveByDepartmentDTO{" +
                "fromDepartment=" + fromDepartment +
                ", quantity=" + quantity +
                '}';
    }
}