/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.site;

import java.io.Serializable;

/**
 * DTO for Site Creation API requests
 * Site is an Institution with type Site
 *
 * @author Buddhika
 */
public class SiteCreateRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name; // Required
    private String code; // Optional - auto-generate if null
    private String address; // Optional
    private String phone; // Optional
    private String mobile; // Optional
    private String email; // Optional
    private String fax; // Optional
    private Long parentInstitutionId; // Optional - main institution this site belongs to
    // Bed-board SVG fields (issue #21592). Optional; additive only.
    private String svgParentView; // Optional - bed-board floor-plan canvas
    private String svgChildView; // Optional - bed-board child tile shape

    public SiteCreateRequestDTO() {
    }

    public SiteCreateRequestDTO(String name, String code, String address, String phone,
                               String mobile, String email, String fax,
                               Long parentInstitutionId) {
        this.name = name;
        this.code = code;
        this.address = address;
        this.phone = phone;
        this.mobile = mobile;
        this.email = email;
        this.fax = fax;
        this.parentInstitutionId = parentInstitutionId;
    }

    /**
     * Validate required fields
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty();
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

    public String getSvgParentView() {
        return svgParentView;
    }

    public void setSvgParentView(String svgParentView) {
        this.svgParentView = svgParentView;
    }

    public String getSvgChildView() {
        return svgChildView;
    }

    public void setSvgChildView(String svgChildView) {
        this.svgChildView = svgChildView;
    }

    @Override
    public String toString() {
        return "SiteCreateRequestDTO{" +
                "name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", address='<REDACTED>'" +
                ", phone='<REDACTED>'" +
                ", mobile='<REDACTED>'" +
                ", email='<REDACTED>'" +
                ", fax='<REDACTED>'" +
                ", parentInstitutionId=" + parentInstitutionId +
                '}';
    }
}
