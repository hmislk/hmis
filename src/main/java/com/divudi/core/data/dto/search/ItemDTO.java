/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.search;

import java.io.Serializable;

/**
 * DTO for Item search results
 * Used in item lookup API responses
 *
 * @author Buddhika
 */
public class ItemDTO implements Serializable {

    private Long id;
    private String name;
    private String code;
    private String barcode;
    private String genericName;
    private String categoryName;

    public ItemDTO() {
    }

    /**
     * Constructor for JPQL queries
     * Used in item search queries to create DTO directly
     */
    public ItemDTO(Long id, String name, String code, String barcode, String genericName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.genericName = genericName;
    }

    /**
     * Extended constructor with category information
     */
    public ItemDTO(Long id, String name, String code, String barcode, String genericName, String categoryName) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.barcode = barcode;
        this.genericName = genericName;
        this.categoryName = categoryName;
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

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
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

    @Override
    public String toString() {
        return "ItemDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", barcode='" + barcode + '\'' +
                ", genericName='" + genericName + '\'' +
                ", categoryName='" + categoryName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemDTO itemDTO = (ItemDTO) o;
        return id != null && id.equals(itemDTO.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}