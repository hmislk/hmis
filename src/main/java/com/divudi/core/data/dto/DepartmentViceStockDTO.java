package com.divudi.core.data.dto;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;

/**
 * DTO for department vice stock report
 * Represents aggregated stock data grouped by department with institution, site, and department type information
 */
public class DepartmentViceStockDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer serialNo;
    private String institutionName;
    private String siteName;
    private String departmentName;
    private DepartmentType departmentType;
    private Double quantity;
    private Double purchaseValue;
    private Double retailValue;
    private Double costValue;

    // Default constructor
    public DepartmentViceStockDTO() {
    }

    // Constructor for direct JPQL query injection
    public DepartmentViceStockDTO(String institutionName, String siteName, String departmentName,
                                 DepartmentType departmentType, Double quantity, Double purchaseValue,
                                 Double retailValue, Double costValue) {
        this.institutionName = institutionName;
        this.siteName = siteName;
        this.departmentName = departmentName;
        this.departmentType = departmentType;
        this.quantity = quantity;
        this.purchaseValue = purchaseValue;
        this.retailValue = retailValue;
        this.costValue = costValue;
    }

    // Getters and setters
    public Integer getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(Integer serialNo) {
        this.serialNo = serialNo;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(Double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public Double getRetailValue() {
        return retailValue;
    }

    public void setRetailValue(Double retailValue) {
        this.retailValue = retailValue;
    }

    public Double getCostValue() {
        return costValue;
    }

    public void setCostValue(Double costValue) {
        this.costValue = costValue;
    }

    @Override
    public String toString() {
        return "DepartmentViceStockDTO{" +
                "serialNo=" + serialNo +
                ", institutionName='" + institutionName + '\'' +
                ", siteName='" + siteName + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", departmentType=" + departmentType +
                ", quantity=" + quantity +
                ", purchaseValue=" + purchaseValue +
                ", retailValue=" + retailValue +
                ", costValue=" + costValue +
                '}';
    }
}