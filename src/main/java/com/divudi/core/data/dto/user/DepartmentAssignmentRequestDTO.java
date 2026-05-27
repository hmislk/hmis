package com.divudi.core.data.dto.user;

import java.util.List;

public class DepartmentAssignmentRequestDTO {
    private List<Long> departmentIds;
    public List<Long> getDepartmentIds() { return departmentIds; }
    public void setDepartmentIds(List<Long> departmentIds) { this.departmentIds = departmentIds; }
}
