/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.department;

import com.divudi.core.data.DepartmentType;
import java.io.Serializable;

/**
 * DTO for Department Update API requests
 *
 * @author Buddhika
 */
public class DepartmentUpdateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id; // Required
    private String name; // Optional - update if provided
    private String code; // Optional - update if provided
    private DepartmentType departmentType; // Optional - update if provided
    private String description; // Optional - update if provided
    private String printingName; // Optional - update if provided
    private String address; // Optional - update if provided
    private String telephone1; // Optional - update if provided
    private String telephone2; // Optional - update if provided
    private String fax; // Optional - update if provided
    private String email; // Optional - update if provided
    private Double margin; // Optional - update if provided
    private Double pharmacyMarginFromPurchaseRate; // Optional - update if provided
    private Boolean active; // Optional - update if provided

    public DepartmentUpdateRequestDTO() {
    }

    public DepartmentUpdateRequestDTO(Long id, String name, String code, DepartmentType departmentType,
                                     String description, String printingName, String address,
                                     String telephone1, String telephone2, String fax, String email,
                                     Double margin, Double pharmacyMarginFromPurchaseRate,
                                     Boolean active) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.departmentType = departmentType;
        this.description = description;
        this.printingName = printingName;
        this.address = address;
        this.telephone1 = telephone1;
        this.telephone2 = telephone2;
        this.fax = fax;
        this.email = email;
        this.margin = margin;
        this.pharmacyMarginFromPurchaseRate = pharmacyMarginFromPurchaseRate;
        this.active = active;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return id != null;
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

    @Override
    public String toString() {
        return "DepartmentUpdateRequestDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", departmentType=" + departmentType +
                ", description='" + description + '\'' +
                ", printingName='" + printingName + '\'' +
                ", address='" + address + '\'' +
                ", telephone1='" + telephone1 + '\'' +
                ", telephone2='" + telephone2 + '\'' +
                ", fax='" + fax + '\'' +
                ", email='" + email + '\'' +
                ", margin=" + margin +
                ", pharmacyMarginFromPurchaseRate=" + pharmacyMarginFromPurchaseRate +
                ", active=" + active +
                '}';
    }
}
