/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.site;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Site API responses
 * Site is an Institution with type Site
 * Contains details of site for API responses
 *
 * @author Buddhika
 */
public class SiteResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String address;
    private String phone;
    private String mobile;
    private String email;
    private String fax;
    private Long parentInstitutionId;
    private String parentInstitutionName;
    private Boolean active;
    private Date createdAt;
    private String message;

    public SiteResponseDTO() {
    }

    public SiteResponseDTO(Long id, String name, String code, String address, String phone,
                          String mobile, String email, String fax, Long parentInstitutionId,
                          String parentInstitutionName, Boolean active, Date createdAt,
                          String message) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.address = address;
        this.phone = phone;
        this.mobile = mobile;
        this.email = email;
        this.fax = fax;
        this.parentInstitutionId = parentInstitutionId;
        this.parentInstitutionName = parentInstitutionName;
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
        return "SiteResponseDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", fax='" + fax + '\'' +
                ", parentInstitutionId=" + parentInstitutionId +
                ", parentInstitutionName='" + parentInstitutionName + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", message='" + message + '\'' +
                '}';
    }
}
