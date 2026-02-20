package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for User Department Tracking
 * This class tracks departments assigned to users via privileges
 * and departments actually accessed by users through login history
 *
 * @author Claude Code
 */
public class UserDepartmentDto implements Serializable {

    private Long departmentId;
    private String departmentName;
    private String institutionName;
    private boolean assignedViaPrivilege;  // From WebUserPrivilege
    private boolean accessed;               // From Logins history
    private Date lastAccessedAt;           // Most recent login to this dept
    private Long accessCount;              // Number of logins to this dept

    // Default constructor
    public UserDepartmentDto() {
    }

    // Constructor for privilege-assigned departments (3 params)
    public UserDepartmentDto(Long departmentId, String departmentName,
                           String institutionName) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.assignedViaPrivilege = true;
        this.accessed = false;
    }

    // Constructor for accessed departments from login history (5 params)
    public UserDepartmentDto(Long departmentId, String departmentName,
                           String institutionName, Date lastAccessedAt,
                           Long accessCount) {
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.assignedViaPrivilege = false;
        this.accessed = true;
        this.lastAccessedAt = lastAccessedAt;
        this.accessCount = accessCount;
    }

    // Getters and Setters
    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName != null ? departmentName : "";
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getInstitutionName() {
        return institutionName != null ? institutionName : "";
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public boolean isAssignedViaPrivilege() {
        return assignedViaPrivilege;
    }

    public void setAssignedViaPrivilege(boolean assignedViaPrivilege) {
        this.assignedViaPrivilege = assignedViaPrivilege;
    }

    public boolean isAccessed() {
        return accessed;
    }

    public void setAccessed(boolean accessed) {
        this.accessed = accessed;
    }

    public Date getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Date lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Long getAccessCount() {
        return accessCount != null ? accessCount : 0L;
    }

    public void setAccessCount(Long accessCount) {
        this.accessCount = accessCount;
    }

    @Override
    public String toString() {
        return "UserDepartmentDto{" +
                "departmentId=" + departmentId +
                ", departmentName='" + departmentName + '\'' +
                ", assignedViaPrivilege=" + assignedViaPrivilege +
                ", accessed=" + accessed +
                ", accessCount=" + accessCount +
                '}';
    }
}
