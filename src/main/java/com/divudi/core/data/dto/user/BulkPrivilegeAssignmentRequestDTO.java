package com.divudi.core.data.dto.user;

import java.util.List;

public class BulkPrivilegeAssignmentRequestDTO {
    private List<Long> userIds;
    private List<String> privileges;
    private Long departmentId;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    public List<String> getPrivileges() { return privileges; }
    public void setPrivileges(List<String> privileges) { this.privileges = privileges; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
