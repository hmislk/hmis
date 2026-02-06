/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.department;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;

/**
 * DTO for Department Creation API requests
 *
 * @author Buddhika
 */
public class DepartmentCreateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name; // Required
    private String code; // Optional - auto-generate if null
    private DepartmentType departmentType; // Required
    private Long institutionId; // Required
    private Long siteId; // Optional - Site (Institution with type Site)
    private String description; // Optional
    private String printingName; // Optional
    private String address; // Optional
    private String telephone1; // Optional
    private String telephone2; // Optional
    private String fax; // Optional
    private String email; // Optional
    private Long superDepartmentId; // Optional - parent department
    private Double margin; // Optional - department margin
    private Double pharmacyMarginFromPurchaseRate; // Optional

    public DepartmentCreateRequestDTO() {
    }

    public DepartmentCreateRequestDTO(String name, String code, DepartmentType departmentType,
                                     Long institutionId, Long siteId, String description,
                                     String printingName, String address, String telephone1,
                                     String telephone2, String fax, String email,
                                     Long superDepartmentId, Double margin,
                                     Double pharmacyMarginFromPurchaseRate) {
        this.name = name;
        this.code = code;
        this.departmentType = departmentType;
        this.institutionId = institutionId;
        this.siteId = siteId;
        this.description = description;
        this.printingName = printingName;
        this.address = address;
        this.telephone1 = telephone1;
        this.telephone2 = telephone2;
        this.fax = fax;
        this.email = email;
        this.superDepartmentId = superDepartmentId;
        this.margin = margin;
        this.pharmacyMarginFromPurchaseRate = pharmacyMarginFromPurchaseRate;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               departmentType != null &&
               institutionId != null;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPrintingName() {
        return printingName;
    }

    public void setPrintingName(String printingName) {
        this.printingName = printingName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTelephone1() {
        return telephone1;
    }

    public void setTelephone1(String telephone1) {
        this.telephone1 = telephone1;
    }

    public String getTelephone2() {
        return telephone2;
    }

    public void setTelephone2(String telephone2) {
        this.telephone2 = telephone2;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getSuperDepartmentId() {
        return superDepartmentId;
    }

    public void setSuperDepartmentId(Long superDepartmentId) {
        this.superDepartmentId = superDepartmentId;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public Double getPharmacyMarginFromPurchaseRate() {
        return pharmacyMarginFromPurchaseRate;
    }

    public void setPharmacyMarginFromPurchaseRate(Double pharmacyMarginFromPurchaseRate) {
        this.pharmacyMarginFromPurchaseRate = pharmacyMarginFromPurchaseRate;
    }

    @Override
    public String toString() {
        return "DepartmentCreateRequestDTO{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", departmentType=" + departmentType +
                ", institutionId=" + institutionId +
                ", siteId=" + siteId +
                ", description='" + description + '\'' +
                ", printingName='" + printingName + '\'' +
                ", address='" + address + '\'' +
                ", telephone1='" + telephone1 + '\'' +
                ", telephone2='" + telephone2 + '\'' +
                ", fax='" + fax + '\'' +
                ", email='" + email + '\'' +
                ", superDepartmentId=" + superDepartmentId +
                ", margin=" + margin +
                ", pharmacyMarginFromPurchaseRate=" + pharmacyMarginFromPurchaseRate +
                '}';
    }
}
