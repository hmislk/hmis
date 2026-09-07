/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.search;

import java.io.Serializable;

/**
 * DTO for Department search results
 * Used in department lookup API responses
 *
 * @author Buddhika
 */
public class DepartmentDTO implements Serializable {

    private Long id;
    private String name;
    private String code;

    public DepartmentDTO() {
    }

    /**
     * Constructor for JPQL queries
     * Used in department search queries to create DTO directly
     */
    public DepartmentDTO(Long id, String name, String code) {
        this.id = id;
        this.name = name;
        this.code = code;
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

    @Override
    public String toString() {
        return "DepartmentDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepartmentDTO that = (DepartmentDTO) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}