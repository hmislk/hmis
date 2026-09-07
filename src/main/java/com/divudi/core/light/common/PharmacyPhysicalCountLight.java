package com.divudi.core.light.common;

import java.io.Serializable;
import java.util.Date;

public class PharmacyPhysicalCountLight implements Serializable {
    private Long id;
    private String deptId;
    private Date createdAt;
    private String institutionName;
    private String departmentName;
    private Long itemsCount;

    public PharmacyPhysicalCountLight(Long id, String deptId, Date createdAt,
                                      String institutionName, String departmentName,
                                      Long itemsCount) {
        this.id = id;
        this.deptId = deptId;
        this.createdAt = createdAt;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.itemsCount = itemsCount;
    }

    public PharmacyPhysicalCountLight() {}

    public Long getId() { return id; }
    public String getDeptId() { return deptId; }
    public Date getCreatedAt() { return createdAt; }
    public String getInstitutionName() { return institutionName; }
    public String getDepartmentName() { return departmentName; }
    public Long getItemsCount() { return itemsCount; }
}

