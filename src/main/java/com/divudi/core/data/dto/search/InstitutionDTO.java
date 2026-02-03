/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.search;

import com.divudi.core.data.InstitutionType;
import java.io.Serializable;

/**
 * DTO for Institution search results
 * Used in institution lookup API responses
 *
 * @author Buddhika
 */
public class InstitutionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private InstitutionType institutionType;
    private String address;
    private String phone;
    private String email;

    public InstitutionDTO() {
    }

    /**
     * Constructor for JPQL queries
     * Used in institution search queries to create DTO directly
     */
    public InstitutionDTO(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
    }

    /**
     * Constructor with institution type
     */
    public InstitutionDTO(Long id, String name, String code, InstitutionType institutionType) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.institutionType = institutionType;
    }

    /**
     * Full constructor for detailed search results
     */
    public InstitutionDTO(Long id, String name, String code, InstitutionType institutionType,
                         String address, String phone, String email) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.institutionType = institutionType;
        this.address = address;
        this.phone = phone;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "InstitutionDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", institutionType=" + institutionType +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InstitutionDTO that = (InstitutionDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
