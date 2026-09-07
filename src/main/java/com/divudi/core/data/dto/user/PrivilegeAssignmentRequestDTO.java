package com.divudi.core.data.dto.user;

import java.util.List;

public class PrivilegeAssignmentRequestDTO {
    private List<String> privileges;
    private Long departmentId;
    public List<String> getPrivileges() { return privileges; }
    public void setPrivileges(List<String> privileges) { this.privileges = privileges; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
