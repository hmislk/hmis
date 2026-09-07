package com.divudi.core.data.dto;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for pharmacy batch stock display with expiry information
 * Contains batch details, expiry date, location, and quantity
 */
public class PharmacyBatchStockDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String batchNo;
    private Date dateOfExpire;
    private Institution institution;
    private Department department;
    private Double quantity;

    // Default constructor
    public PharmacyBatchStockDTO() {
    }

    // Constructor for JPQL constructor query
    public PharmacyBatchStockDTO(String batchNo, Date dateOfExpire,
                                 Institution institution, Department department,
                                 Double quantity) {
        this.batchNo = batchNo;
        this.dateOfExpire = dateOfExpire;
        this.institution = institution;
        this.department = department;
        this.quantity = quantity;
    }

    // Getters and setters
    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Date getDateOfExpire() {
        return dateOfExpire;
    }

    public void setDateOfExpire(Date dateOfExpire) {
        this.dateOfExpire = dateOfExpire;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "PharmacyBatchStockDTO{" +
                "batchNo='" + batchNo + '\'' +
                ", dateOfExpire=" + dateOfExpire +
                ", institution=" + (institution != null ? institution.getName() : "null") +
                ", department=" + (department != null ? department.getName() : "null") +
                ", quantity=" + quantity +
                '}';
    }
}