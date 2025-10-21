package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import java.io.Serializable;

/**
 * Simplified DTO for pharmacy department stock display
 * Contains Department entity and stock quantity only
 */
public class PharmacyDepartmentStockDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Department department;
    private Double qty;

    // Default constructor
    public PharmacyDepartmentStockDTO() {
    }

    // Constructor for use in code
    public PharmacyDepartmentStockDTO(Department department, Double qty) {
        this.department = department;
        this.qty = qty;
    }

    // Getters and setters
    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    @Override
    public String toString() {
        return "PharmacyDepartmentStockDTO{" +
                "department=" + (department != null ? department.getName() : "null") +
                ", qty=" + qty +
                '}';
    }
}