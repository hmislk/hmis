/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.batch;

import java.io.Serializable;

/**
 * DTO for AMP Search or Create API requests
 *
 * @author Buddhika
 */
public class AmpSearchCreateRequestDTO implements Serializable {

    private String name; // Required - AMP name to search or create
    private String genericName; // Optional - VMP name, will use any available if null
    private Long categoryId; // Optional - item category
    private Long dosageFormId; // Optional - dosage form

    public AmpSearchCreateRequestDTO() {
    }

    public AmpSearchCreateRequestDTO(String name, String genericName, Long categoryId, Long dosageFormId) {
        this.name = name;
        this.genericName = genericName;
        this.categoryId = categoryId;
        this.dosageFormId = dosageFormId;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getDosageFormId() {
        return dosageFormId;
    }

    public void setDosageFormId(Long dosageFormId) {
        this.dosageFormId = dosageFormId;
    }
}