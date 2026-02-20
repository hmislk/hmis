/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.institution;

import com.divudi.core.data.InstitutionType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Institution API responses
 * Contains details of institution for API responses
 *
 * @author Buddhika
 */
public class InstitutionResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private InstitutionType institutionType;
    private String address;
    private String phone;
    private String mobile;
    private String email;
    private String fax;
    private String web;
    private Long parentInstitutionId;
    private String parentInstitutionName;
    private String contactPersonName;
    private String ownerName;
    private String ownerEmail;
    private Boolean active;
    private Date createdAt;
    private String message;

    public InstitutionResponseDTO() {
    }

    public InstitutionResponseDTO(Long id, String name, String code, InstitutionType institutionType,
                                 String address, String phone, String mobile, String email,
                                 String fax, String web, Long parentInstitutionId,
                                 String parentInstitutionName, String contactPersonName,
                                 String ownerName, String ownerEmail, Boolean active,
                                 Date createdAt, String message) {
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
        this.parentInstitutionId = parentInstitutionId;
        this.parentInstitutionName = parentInstitutionName;
        this.contactPersonName = contactPersonName;
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
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

    public String getParentInstitutionName() {
        return parentInstitutionName;
    }

    public void setParentInstitutionName(String parentInstitutionName) {
        this.parentInstitutionName = parentInstitutionName;
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
        return "InstitutionResponseDTO{" +
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
                ", parentInstitutionId=" + parentInstitutionId +
                ", parentInstitutionName='" + parentInstitutionName + '\'' +
                ", contactPersonName='" + contactPersonName + '\'' +
                ", ownerName='" + ownerName + '\'' +
                ", ownerEmail='" + ownerEmail + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", message='" + message + '\'' +
                '}';
    }
}
