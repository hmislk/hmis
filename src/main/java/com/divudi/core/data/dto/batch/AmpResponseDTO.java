/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.batch;

import java.io.Serializable;

/**
 * DTO for AMP API responses
 * Used for both search and create operations
 *
 * @author Buddhika
 */
public class AmpResponseDTO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String genericName;
    private String categoryName;
    private Boolean created; // true if newly created, false if found existing

    public AmpResponseDTO() {
    }

    /**
     * Constructor for search results (existing AMP)
     */
    public AmpResponseDTO(Long id, String name, String code, String genericName, String categoryName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.genericName = genericName;
        this.categoryName = categoryName;
        this.created = false; // Existing AMP
    }

    /**
     * Full constructor including creation flag
     */
    public AmpResponseDTO(Long id, String name, String code, String genericName, String categoryName, Boolean created) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.genericName = genericName;
        this.categoryName = categoryName;
        this.created = created;
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

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Boolean getCreated() {
        return created;
    }

    public void setCreated(Boolean created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "AmpResponseDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", genericName='" + genericName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", created=" + created +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AmpResponseDTO that = (AmpResponseDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}