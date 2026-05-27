/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.institution;

import com.divudi.core.data.InstitutionType;
import java.io.Serializable;

/**
 * DTO for Institution Update API requests
 *
 * @author Buddhika
 */
public class InstitutionUpdateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id; // Required
    private String name; // Optional - update if provided
    private String code; // Optional - update if provided
    private InstitutionType institutionType; // Optional - update if provided
    private String address; // Optional - update if provided
    private String phone; // Optional - update if provided
    private String mobile; // Optional - update if provided
    private String email; // Optional - update if provided
    private String fax; // Optional - update if provided
    private String web; // Optional - update if provided
    private String contactPersonName; // Optional - update if provided
    private String ownerName; // Optional - update if provided
    private String ownerEmail; // Optional - update if provided
    private Boolean active; // Optional - update if provided

    public InstitutionUpdateRequestDTO() {
    }

    public InstitutionUpdateRequestDTO(Long id, String name, String code, InstitutionType institutionType,
                                      String address, String phone, String mobile, String email,
                                      String fax, String web, String contactPersonName,
                                      String ownerName, String ownerEmail, Boolean active) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.institutionType = institutionType;
        this.address = address;
        this.phone = phone;
        this.mobile = mobile;
        this.email = email;
        this.fax = fax;
        this.web = web;
        this.contactPersonName = contactPersonName;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "InstitutionUpdateRequestDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", institutionType=" + institutionType +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", fax='" + fax + '\'' +
                ", web='" + web + '\'' +
                ", contactPersonName='" + contactPersonName + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", active=" + active +
                '}';
    }
}
