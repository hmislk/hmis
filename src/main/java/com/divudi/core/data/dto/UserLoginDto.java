package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for User Login Records
 * This class represents user login data for the User Logins report
 * to improve performance by avoiding entity relationship loading
 *
 * @author Claude Code
 */
public class UserLoginDto implements Serializable {

    private Long loginId;
    private Long webUserId;
    private String username;
    private String userCode;
    private String userPersonName;
    private String institutionName;
    private String departmentName;
    private Date logedAt;
    private Date logoutAt;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String screenResolution;
    private String computerName;

    // Default constructor
    public UserLoginDto() {
    }

    // Constructor for JPQL DTO projection (backward compatible - basic info)
    public UserLoginDto(Long loginId, Long webUserId, String username,
                       String userCode, String userPersonName,
                       String institutionName, String departmentName,
                       Date logedAt, Date logoutAt, String ipAddress) {
        this.loginId = loginId;
        this.webUserId = webUserId;
        this.username = username;
        this.userCode = userCode;
        this.userPersonName = userPersonName;
        this.institutionName = institutionName;
        this.departmentName = departmentName;
        this.logedAt = logedAt;
        this.logoutAt = logoutAt;
        this.ipAddress = ipAddress;
    }

    // Enhanced constructor with additional system info
    public UserLoginDto(Long loginId, Long webUserId, String username,
                       String userCode, String userPersonName,
                       String institutionName, String departmentName,
                       Date logedAt, Date logoutAt, String ipAddress,
                       String browser, String operatingSystem,
                       String screenResolution, String computerName) {
        this(loginId, webUserId, username, userCode, userPersonName,
             institutionName, departmentName, logedAt, logoutAt, ipAddress);
        this.browser = browser;
        this.operatingSystem = operatingSystem;
        this.screenResolution = screenResolution;
        this.computerName = computerName;
    }

    // Getters with null-safe defaults
    public Long getLoginId() {
        return loginId;
    }

    public void setLoginId(Long loginId) {
        this.loginId = loginId;
    }

    public Long getWebUserId() {
        return webUserId;
    }

    public void setWebUserId(Long webUserId) {
        this.webUserId = webUserId;
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

    public String getUserPersonName() {
        return userPersonName != null ? userPersonName : "";
    }

    public void setUserPersonName(String userPersonName) {
        this.userPersonName = userPersonName;
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

    public Date getLogedAt() {
        return logedAt;
    }

    public void setLogedAt(Date logedAt) {
        this.logedAt = logedAt;
    }

    public Date getLogoutAt() {
        return logoutAt;
    }

    public void setLogoutAt(Date logoutAt) {
        this.logoutAt = logoutAt;
    }

    public String getIpAddress() {
        return ipAddress != null ? ipAddress : "";
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getBrowser() {
        return browser != null ? browser : "";
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOperatingSystem() {
        return operatingSystem != null ? operatingSystem : "";
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getScreenResolution() {
        return screenResolution != null ? screenResolution : "";
    }

    public void setScreenResolution(String screenResolution) {
        this.screenResolution = screenResolution;
    }

    public String getComputerName() {
        return computerName != null ? computerName : "";
    }

    public void setComputerName(String computerName) {
        this.computerName = computerName;
    }

    @Override
    public String toString() {
        return "UserLoginDto{" +
                "loginId=" + loginId +
                ", webUserId=" + webUserId +
                ", username='" + username + '\'' +
                ", userPersonName='" + userPersonName + '\'' +
                ", logedAt=" + logedAt +
                ", logoutAt=" + logoutAt +
                '}';
    }
}
