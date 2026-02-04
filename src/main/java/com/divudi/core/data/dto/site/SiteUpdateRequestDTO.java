/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.site;

import java.io.Serializable;

/**
 * DTO for Site Update API requests
 * Site is an Institution with type Site
 *
 * @author Buddhika
 */
public class SiteUpdateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id; // Required
    private String name; // Optional - update if provided
    private String code; // Optional - update if provided
    private String address; // Optional - update if provided
    private String phone; // Optional - update if provided
    private String mobile; // Optional - update if provided
    private String email; // Optional - update if provided
    private String fax; // Optional - update if provided
    private Boolean active; // Optional - update if provided

    public SiteUpdateRequestDTO() {
    }

    public SiteUpdateRequestDTO(Long id, String name, String code, String address,
                               String phone, String mobile, String email, String fax,
                               Boolean active) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.address = address;
        this.phone = phone;
        this.mobile = mobile;
        this.email = email;
        this.fax = fax;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "SiteUpdateRequestDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", mobile='" + mobile + '\'' +
                ", email='" + email + '\'' +
                ", fax='" + fax + '\'' +
                ", active=" + active +
                '}';
    }
}
