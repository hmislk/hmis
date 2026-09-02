package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BhtIssueRequestPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- from department ----
    private String fromDepartmentPrintingName = "";
    private String fromDepartmentName = "";
    private String fromDepartmentAddress = "";
    private String fromDepartmentTelephone1 = "";
    private String fromDepartmentTelephone2 = "";
    private String fromDepartmentFax = "";

    // ---- to department ----
    private String toDepartmentName = "";

    // ---- bill header ----
    private String requestNo = "";
    private Date createdAt;
    private String comments = "";
    private boolean completed;
    private boolean cancelled;

    // ---- patient / encounter ----
    private String bhtNo = "";
    private String patientName = "";
    private String patientPhn = "";
    private String roomName = "";

    // ---- requester ----
    private String requestedByName = "";
    private String systemUserName = "";

    private List<BhtIssueRequestItemPrintDto> items = new ArrayList<>();

    public String getFromDepartmentPrintingName() {
        return fromDepartmentPrintingName;
    }

    public void setFromDepartmentPrintingName(String fromDepartmentPrintingName) {
        this.fromDepartmentPrintingName = fromDepartmentPrintingName;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getFromDepartmentAddress() {
        return fromDepartmentAddress;
    }

    public void setFromDepartmentAddress(String fromDepartmentAddress) {
        this.fromDepartmentAddress = fromDepartmentAddress;
    }

    public String getFromDepartmentTelephone1() {
        return fromDepartmentTelephone1;
    }

    public void setFromDepartmentTelephone1(String fromDepartmentTelephone1) {
        this.fromDepartmentTelephone1 = fromDepartmentTelephone1;
    }

    public String getFromDepartmentTelephone2() {
        return fromDepartmentTelephone2;
    }

    public void setFromDepartmentTelephone2(String fromDepartmentTelephone2) {
        this.fromDepartmentTelephone2 = fromDepartmentTelephone2;
    }

    public String getFromDepartmentFax() {
        return fromDepartmentFax;
    }

    public void setFromDepartmentFax(String fromDepartmentFax) {
        this.fromDepartmentFax = fromDepartmentFax;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public void setPatientPhn(String patientPhn) {
        this.patientPhn = patientPhn;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public void setRequestedByName(String requestedByName) {
        this.requestedByName = requestedByName;
    }

    public String getSystemUserName() {
        return systemUserName;
    }

    public void setSystemUserName(String systemUserName) {
        this.systemUserName = systemUserName;
    }

    public List<BhtIssueRequestItemPrintDto> getItems() {
        return items;
    }

    public void setItems(List<BhtIssueRequestItemPrintDto> items) {
        this.items = items;
    }
}
