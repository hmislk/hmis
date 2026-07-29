package com.divudi.core.data.dto;

import com.divudi.core.data.Title;
import java.io.Serializable;
import java.util.Date;

/**
 * Row DTO for the pharmacy-side "Medicine Returns Pending Receipt" list
 * ({@code pharmacy_return_from_ward_receive_list.xhtml}), avoiding a full
 * {@link com.divudi.core.entity.Bill} entity fetch for what is a read-only
 * listing (#21471).
 */
public class PharmacyReturnFromWardPendingReturnDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String deptId;
    private String fromDepartmentName;
    private String createrPersonName;
    private Long toStaffId;
    private String toStaffNameWithTitle;
    private Date createdAt;

    public PharmacyReturnFromWardPendingReturnDTO() {
    }

    public PharmacyReturnFromWardPendingReturnDTO(Long id, String deptId, String fromDepartmentName,
            String createrPersonName, Long toStaffId, Title toStaffTitle, String toStaffPersonName, Date createdAt) {
        this.id = id;
        this.deptId = deptId;
        this.fromDepartmentName = fromDepartmentName;
        this.createrPersonName = createrPersonName;
        this.toStaffId = toStaffId;
        this.toStaffNameWithTitle = buildNameWithTitle(toStaffTitle, toStaffPersonName);
        this.createdAt = createdAt;
    }

    private static String buildNameWithTitle(Title title, String name) {
        String label = title != null ? title.getLabel() : "";
        return (label + " " + (name != null ? name : "")).trim();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getCreaterPersonName() {
        return createrPersonName;
    }

    public void setCreaterPersonName(String createrPersonName) {
        this.createrPersonName = createrPersonName;
    }

    public Long getToStaffId() {
        return toStaffId;
    }

    public void setToStaffId(Long toStaffId) {
        this.toStaffId = toStaffId;
    }

    public String getToStaffNameWithTitle() {
        return toStaffNameWithTitle;
    }

    public void setToStaffNameWithTitle(String toStaffNameWithTitle) {
        this.toStaffNameWithTitle = toStaffNameWithTitle;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
