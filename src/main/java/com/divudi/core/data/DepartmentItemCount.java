package com.divudi.core.data;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class DepartmentItemCount {
    private Long departmentId;
    private String departmentName;
    private Long itemCount;

    public DepartmentItemCount() {
    }



    public DepartmentItemCount(Long department_id, String departmentName, Long itemCount) {
        this.departmentId = department_id;
        this.departmentName = departmentName;
        this.itemCount = itemCount;
    }



    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }


}
