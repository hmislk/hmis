package com.divudi.core.data.dto.user;

public class UserUpsertRequestDTO {
    private String name;
    private String code;
    private String email;
    private String telNo;
    private String personName;
    private String personMobile;
    private Long institutionId;
    private Long siteId;
    private Long departmentId;
    private Long roleId;
    private Boolean activated;
    private String password;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelNo() { return telNo; }
    public void setTelNo(String telNo) { this.telNo = telNo; }
    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }
    public String getPersonMobile() { return personMobile; }
    public void setPersonMobile(String personMobile) { this.personMobile = personMobile; }
    public Long getInstitutionId() { return institutionId; }
    public void setInstitutionId(Long institutionId) { this.institutionId = institutionId; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }
    public Boolean getActivated() { return activated; }
    public void setActivated(Boolean activated) { this.activated = activated; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
