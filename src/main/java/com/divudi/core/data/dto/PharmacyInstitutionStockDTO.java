package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for pharmacy institution stock display in history.xhtml
 * Represents department-level stock data with institution and site information
 */
public class PharmacyInstitutionStockDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long institutionId;
    private String institutionName;
    private Long departmentId;
    private String departmentName;
    private Long siteId;
    private String siteName;
    private Double stockQuantity;

    // Default constructor
    public PharmacyInstitutionStockDTO() {
    }

    // Constructor for direct JPQL query
    public PharmacyInstitutionStockDTO(Long institutionId, String institutionName,
                                       Long departmentId, String departmentName,
                                       Long siteId, String siteName,
                                       Double stockQuantity) {
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.stockQuantity = stockQuantity;
    }

    // Getters and setters
    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Double getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Double stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    @Override
    public String toString() {
        return "PharmacyInstitutionStockDTO{" +
                "institutionName='" + institutionName + '\'' +
                ", departmentName='" + departmentName + '\'' +
                ", siteName='" + siteName + '\'' +
                ", stockQuantity=" + stockQuantity +
                '}';
    }
}