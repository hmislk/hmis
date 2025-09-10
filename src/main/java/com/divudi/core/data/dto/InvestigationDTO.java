package com.divudi.core.data.dto;

import java.io.Serializable;

public class InvestigationDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String name;
    private String categoryName;
    private String institutionName;
    private String machineName;
    private Boolean retired;
    private String departmentName;

    public InvestigationDTO() {
    }

    public InvestigationDTO(Long id, String name, String categoryName, String institutionName,
                             String machineName, Boolean retired, String departmentName) {
        this.id = id;
        this.name = name;
        this.categoryName = categoryName;
        this.institutionName = institutionName;
        this.machineName = machineName;
        this.retired = retired;
        this.departmentName = departmentName;
    }
    
    public InvestigationDTO(Long id, String name) {
        this.id = id;
        this.name = name;
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}

