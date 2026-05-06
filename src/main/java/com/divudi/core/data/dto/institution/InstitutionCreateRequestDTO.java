/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.institution;

import com.divudi.core.data.InstitutionType;
import java.io.Serializable;

/**
 * DTO for Institution Creation API requests
 *
 * @author Buddhika
 */
public class InstitutionCreateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name; // Required
    private String code; // Optional - auto-generate if null
    private InstitutionType institutionType; // Required
    private String address; // Optional
    private String phone; // Optional
    private String mobile; // Optional
    private String email; // Optional
    private String fax; // Optional
    private String web; // Optional
    private Long parentInstitutionId; // Optional - for branch institutions
    private String contactPersonName; // Optional
    private String ownerName; // Optional
    private String ownerEmail; // Optional

    public InstitutionCreateRequestDTO() {
    }

    public InstitutionCreateRequestDTO(String name, String code, InstitutionType institutionType,
                                      String address, String phone, String mobile, String email,
                                      String fax, String web, Long parentInstitutionId,
                                      String contactPersonName, String ownerName, String ownerEmail) {
        this.name = name;
        this.code = code;
        this.institutionType = institutionType;
        this.address = address;
        this.phone = phone;
        this.mobile = mobile;
        this.email = email;
        this.fax = fax;
        this.web = web;
        this.parentInstitutionId = parentInstitutionId;
        this.contactPersonName = contactPersonName;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
               institutionType != null;
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

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getWeb() {
        return web;
    }

    public void setWeb(String web) {
        this.web = web;
    }

    public Long getParentInstitutionId() {
        return parentInstitutionId;
    }

    public void setParentInstitutionId(Long parentInstitutionId) {
        this.parentInstitutionId = parentInstitutionId;
    }

    public String getContactPersonName() {
        return contactPersonName;
    }

    public void setContactPersonName(String contactPersonName) {
        this.contactPersonName = contactPersonName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    @Override
    public String toString() {
        return "InstitutionCreateRequestDTO{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", institutionType=" + institutionType +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", fax='" + fax + '\'' +
                ", web='" + web + '\'' +
                ", parentInstitutionId=" + parentInstitutionId +
                ", contactPersonName='" + contactPersonName + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                '}';
    }
}
