package com.divudi.core.light.common;

import java.io.Serializable;
import java.util.Date;

/**
 * Lightweight DTO for Pharmacy Snapshot Bills list views.
 * Matches JPQL constructor usage in controllers for findLightsByJpql.
 */
public class PharmacySnapshotBillLight implements Serializable {

    private Long id;
    private String deptId;
    private Date createdAt;
    private String institutionName;
    private String departmentName;
    private Long itemsCount;
    private Double netTotal;

    public PharmacySnapshotBillLight(Long id,
                                     String deptId,
                                     Date createdAt,
                                     String institutionName,
                                     String departmentName,
                                     Long itemsCount,
                                     Double netTotal) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.itemsCount = itemsCount;
        this.netTotal = netTotal;
    }

    public PharmacySnapshotBillLight() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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

    public Long getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Long itemsCount) {
        this.itemsCount = itemsCount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }
}

