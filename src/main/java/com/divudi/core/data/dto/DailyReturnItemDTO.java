package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * Lightweight projection for detailed Daily Return items.
 */
public class DailyReturnItemDTO implements Serializable {
    private String departmentName;
    private Double total;

    public DailyReturnItemDTO() {
    }

    public DailyReturnItemDTO(String departmentName, Double total) {
        this.departmentName = departmentName;
        this.total = total;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }
}
