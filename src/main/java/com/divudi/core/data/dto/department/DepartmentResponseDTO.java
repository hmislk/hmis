/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.department;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Department API responses
 * Contains details of department for API responses
 *
 * @author Buddhika
 */
public class DepartmentResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private DepartmentType departmentType;
    private Long institutionId;
    private String institutionName;
    private Long siteId;
    private String siteName;
    private String description;
    private String printingName;
    private String address;
    private String telephone1;
    private String telephone2;
    private String fax;
    private String email;
    private Long superDepartmentId;
    private String superDepartmentName;
    private Double margin;
    private Double pharmacyMarginFromPurchaseRate;
    private Boolean active;
    private Date createdAt;
    private String message;

    public DepartmentResponseDTO() {
    }

    public DepartmentResponseDTO(Long id, String name, String code, DepartmentType departmentType,
                                Long institutionId, String institutionName, Long siteId,
                                String siteName, String description, String printingName,
                                String address, String telephone1, String telephone2, String fax,
                                String email, Long superDepartmentId, String superDepartmentName,
                                Double margin, Double pharmacyMarginFromPurchaseRate, Boolean active,
                                Date createdAt, String message) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.departmentType = departmentType;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.description = description;
        this.printingName = printingName;
        this.address = address;
        this.telephone1 = telephone1;
        this.telephone2 = telephone2;
        this.fax = fax;
        this.email = email;
        this.superDepartmentId = superDepartmentId;
        this.superDepartmentName = superDepartmentName;
        this.margin = margin;
        this.pharmacyMarginFromPurchaseRate = pharmacyMarginFromPurchaseRate;
        this.active = active;
        this.createdAt = createdAt;
        this.message = message;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
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

    public String getSuperDepartmentName() {
        return superDepartmentName;
    }

    public void setSuperDepartmentName(String superDepartmentName) {
        this.superDepartmentName = superDepartmentName;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "DepartmentResponseDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", departmentType=" + departmentType +
                ", institutionId=" + institutionId +
                ", institutionName='" + institutionName + '\'' +
                ", siteId=" + siteId +
                ", siteName='" + siteName + '\'' +
                ", description='" + description + '\'' +
                ", printingName='" + printingName + '\'' +
                ", address='" + address + '\'' +
                ", telephone1='" + telephone1 + '\'' +
                ", telephone2='" + telephone2 + '\'' +
                ", fax='" + fax + '\'' +
                ", email='" + email + '\'' +
                ", superDepartmentId=" + superDepartmentId +
                ", superDepartmentName='" + superDepartmentName + '\'' +
                ", margin=" + margin +
                ", pharmacyMarginFromPurchaseRate=" + pharmacyMarginFromPurchaseRate +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", message='" + message + '\'' +
                '}';
    }
}
