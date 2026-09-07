package com.divudi.core.data.dto;

import com.divudi.core.data.hr.EmployeeStatus;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for User Dashboard
 * This class consolidates user personal and staff details for dashboard display
 * to improve performance by avoiding entity relationship loading
 *
 * @author Claude Code
 */
public class UserDashboardDto implements Serializable {

    // WebUser fields
    private Long userId;
    private String username;
    private String userCode;
    private String email;
    private String telNo;
    private String roleName;
    private String personName;
    private String institutionName;
    private String departmentName;
    private Date createdAt;
    private Date activatedAt;
    private Date lastPasswordResetAt;
    private boolean activated;

    // Staff fields (nullable if no staff linked)
    private Long staffId;
    private String staffCode;
    private String staffRegistration;
    private String staffQualification;
    private String designationName;
    private String gradeName;
    private String staffCategoryName;
    private String specialityName;
    private Date dateJoined;
    private Date dateLeft;
    private String employeeStatus;

    // Default constructor
    public UserDashboardDto() {
    }

    // Constructor for basic user details (without staff)
    public UserDashboardDto(Long userId, String username, String userCode,
                          String email, String telNo, String roleName,
                          String personName, String institutionName,
                          String departmentName, Date createdAt,
                          Date activatedAt, Date lastPasswordResetAt,
                          boolean activated) {
        this.userId = userId;
        this.username = username;
        this.userCode = userCode;
        this.email = email;
        this.telNo = telNo;
        this.roleName = roleName;
        this.personName = personName;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.createdAt = createdAt;
        this.activatedAt = activatedAt;
        this.lastPasswordResetAt = lastPasswordResetAt;
        this.activated = activated;
    }

    // Constructor with staff details (for users with staff records) - String employeeStatus
    public UserDashboardDto(Long userId, String username, String userCode,
                          String email, String telNo, String roleName,
                          String personName, String institutionName,
                          String departmentName, Date createdAt,
                          Date activatedAt, Date lastPasswordResetAt,
                          boolean activated,
                          Long staffId, String staffCode, String staffRegistration,
                          String staffQualification, String designationName,
                          String gradeName, String staffCategoryName,
                          String specialityName, Date dateJoined,
                          Date dateLeft, String employeeStatus) {
        this(userId, username, userCode, email, telNo, roleName,
             personName, institutionName, departmentName, createdAt,
             activatedAt, lastPasswordResetAt, activated);
        this.staffId = staffId;
        this.staffCode = staffCode;
        this.staffRegistration = staffRegistration;
        this.staffQualification = staffQualification;
        this.designationName = designationName;
        this.gradeName = gradeName;
        this.staffCategoryName = staffCategoryName;
        this.specialityName = specialityName;
        this.dateJoined = dateJoined;
        this.dateLeft = dateLeft;
        this.employeeStatus = employeeStatus;
    }

    // Constructor with staff details (for users with staff records) - EmployeeStatus enum
    // This constructor is used by JPQL queries that pass the enum directly
    public UserDashboardDto(Long userId, String username, String userCode,
                          String email, String telNo, String roleName,
                          String personName, String institutionName,
                          String departmentName, Date createdAt,
                          Date activatedAt, Date lastPasswordResetAt,
                          boolean activated,
                          Long staffId, String staffCode, String staffRegistration,
                          String staffQualification, String designationName,
                          String gradeName, String staffCategoryName,
                          String specialityName, Date dateJoined,
                          Date dateLeft, EmployeeStatus employeeStatus) {
        this(userId, username, userCode, email, telNo, roleName,
             personName, institutionName, departmentName, createdAt,
             activatedAt, lastPasswordResetAt, activated);
        this.staffId = staffId;
        this.staffCode = staffCode;
        this.staffRegistration = staffRegistration;
        this.staffQualification = staffQualification;
        this.designationName = designationName;
        this.gradeName = gradeName;
        this.staffCategoryName = staffCategoryName;
        this.specialityName = specialityName;
        this.dateJoined = dateJoined;
        this.dateLeft = dateLeft;
        this.employeeStatus = employeeStatus != null ? employeeStatus.toString() : null;
    }

    // Helper method to check if user has staff record
    public boolean hasStaffRecord() {
        return staffId != null;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserCode() {
        return userCode != null ? userCode : "";
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelNo() {
        return telNo != null ? telNo : "";
    }

    public void setTelNo(String telNo) {
        this.telNo = telNo;
    }

    public String getRoleName() {
        return roleName != null ? roleName : "";
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getPersonName() {
        return personName != null ? personName : "";
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getInstitutionName() {
        return institutionName != null ? institutionName : "";
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getDepartmentName() {
        return departmentName != null ? departmentName : "";
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Date activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Date getLastPasswordResetAt() {
        return lastPasswordResetAt;
    }

    public void setLastPasswordResetAt(Date lastPasswordResetAt) {
        this.lastPasswordResetAt = lastPasswordResetAt;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public Long getStaffId() {
        return staffId;
    }

    public void setStaffId(Long staffId) {
        this.staffId = staffId;
    }

    public String getStaffCode() {
        return staffCode != null ? staffCode : "";
    }

    public void setStaffCode(String staffCode) {
        this.staffCode = staffCode;
    }

    public String getStaffRegistration() {
        return staffRegistration != null ? staffRegistration : "";
    }

    public void setStaffRegistration(String staffRegistration) {
        this.staffRegistration = staffRegistration;
    }

    public String getStaffQualification() {
        return staffQualification != null ? staffQualification : "";
    }

    public void setStaffQualification(String staffQualification) {
        this.staffQualification = staffQualification;
    }

    public String getDesignationName() {
        return designationName != null ? designationName : "";
    }

    public void setDesignationName(String designationName) {
        this.designationName = designationName;
    }

    public String getGradeName() {
        return gradeName != null ? gradeName : "";
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public String getStaffCategoryName() {
        return staffCategoryName != null ? staffCategoryName : "";
    }

    public void setStaffCategoryName(String staffCategoryName) {
        this.staffCategoryName = staffCategoryName;
    }

    public String getSpecialityName() {
        return specialityName != null ? specialityName : "";
    }

    public void setSpecialityName(String specialityName) {
        this.specialityName = specialityName;
    }

    public Date getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(Date dateJoined) {
        this.dateJoined = dateJoined;
    }

    public Date getDateLeft() {
        return dateLeft;
    }

    public void setDateLeft(Date dateLeft) {
        this.dateLeft = dateLeft;
    }

    public String getEmployeeStatus() {
        return employeeStatus != null ? employeeStatus : "";
    }

    public void setEmployeeStatus(String employeeStatus) {
        this.employeeStatus = employeeStatus;
    }

    @Override
    public String toString() {
        return "UserDashboardDto{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", personName='" + personName + '\'' +
                ", hasStaffRecord=" + hasStaffRecord() +
                '}';
    }
}
