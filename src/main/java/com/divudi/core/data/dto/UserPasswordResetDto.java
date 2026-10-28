package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the Password Reset Report. One row per web user, showing when the
 * user's password was last updated and who performed that update.
 *
 * @author Claude Code
 */
public class UserPasswordResetDto implements Serializable {

    public static final String UPDATED_BY_SELF = "Self";
    public static final String UPDATED_BY_ADMIN = "Admin";
    public static final String UPDATED_BY_UNKNOWN = "Unknown";
    public static final String UPDATED_BY_NEVER = "Never updated";

    private Long webUserId;
    private String username;
    private String userCode;
    private String staffName;
    private Date lastPasswordResetAt;
    private boolean retired;
    private String updatedBy;
    private Date lastLoginAt;

    public UserPasswordResetDto() {
    }

    // Constructor for JPQL DTO projection. Staff person is preferred over the
    // web user person, falling back to the username when neither is linked.
    public UserPasswordResetDto(Long webUserId, String username, String userCode,
            Title staffPersonTitle, String staffPersonName,
            Title webUserPersonTitle, String webUserPersonName,
            Date lastPasswordResetAt, Boolean retired) {
        this.webUserId = webUserId;
        this.username = username;
        this.userCode = userCode;
        this.lastPasswordResetAt = lastPasswordResetAt;
        this.retired = retired != null && retired;
        if (staffPersonName != null && !staffPersonName.trim().isEmpty()) {
            this.staffName = nameWithTitle(staffPersonTitle, staffPersonName);
        } else if (webUserPersonName != null && !webUserPersonName.trim().isEmpty()) {
            this.staffName = nameWithTitle(webUserPersonTitle, webUserPersonName);
        } else {
            this.staffName = username;
        }
    }

    private static String nameWithTitle(Title title, String name) {
        String t = title == null || title.getLabel() == null ? "" : title.getLabel().trim();
        String n = name.trim();
        return t.isEmpty() ? n : t + " " + n;
    }

    public Long getWebUserId() {
        return webUserId;
    }

    public void setWebUserId(Long webUserId) {
        this.webUserId = webUserId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public Date getLastPasswordResetAt() {
        return lastPasswordResetAt;
    }

    public void setLastPasswordResetAt(Date lastPasswordResetAt) {
        this.lastPasswordResetAt = lastPasswordResetAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // PrimeFaces p:tag severity for the Updated By badge
    public String getUpdatedBySeverity() {
        if (UPDATED_BY_SELF.equals(updatedBy)) {
            return "success";
        }
        if (UPDATED_BY_ADMIN.equals(updatedBy)) {
            return "info";
        }
        if (UPDATED_BY_NEVER.equals(updatedBy)) {
            return "danger";
        }
        return "warning";
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
