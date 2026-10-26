package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight candidate row for the Item Mapping "available items" picker
 * (department / institution / outside-charge mapping pages — see
 * ItemMappingController.fillAvailableItems()).
 *
 * itemType is populated by running one JPQL query per Item subtype
 * (Investigation, Service, InwardService), each passing its own literal
 * label into the constructor call, rather than a single TYPE(i)-based CASE
 * WHEN projection whose EclipseLink support is not guaranteed.
 *
 */
public class ItemMappingCandidateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String code;
    private String itemType;
    private String institutionName;
    private String departmentName;
    private Double total;
    private Date createdAt;

    public ItemMappingCandidateDTO() {
    }

    /**
     * Constructor for the JPQL constructor query in
     * ItemMappingController.fillAvailableItems().
     */
    public ItemMappingCandidateDTO(Long id, String name, String code, String itemType,
            String institutionName, String departmentName, Double total, Date createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.itemType = itemType;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.total = total;
        this.createdAt = createdAt;
    }

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

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
