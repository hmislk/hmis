/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.itemrequest;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Full detail response for an item/service request, returned by GET/POST/PUT
 * endpoints on /api/itemrequests.
 *
 * @author Claude AI Assistant
 */
public class ItemRequestResponseDTO implements Serializable {

    private Long id;
    private String requestNo;
    private String bhtNo;
    private Long targetDepartmentId;
    private String targetDepartmentName;
    private String status;
    private String comments;
    private String rejectionReason;
    private Date createdAt;
    private String createdBy;
    private Long approvalBillId;
    private Date decidedAt;
    private String decidedBy;
    private List<ItemRequestLineResponseDTO> lines;

    public ItemRequestResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Long getTargetDepartmentId() {
        return targetDepartmentId;
    }

    public void setTargetDepartmentId(Long targetDepartmentId) {
        this.targetDepartmentId = targetDepartmentId;
    }

    public String getTargetDepartmentName() {
        return targetDepartmentName;
    }

    public void setTargetDepartmentName(String targetDepartmentName) {
        this.targetDepartmentName = targetDepartmentName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Long getApprovalBillId() {
        return approvalBillId;
    }

    public void setApprovalBillId(Long approvalBillId) {
        this.approvalBillId = approvalBillId;
    }

    public Date getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Date decidedAt) {
        this.decidedAt = decidedAt;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public List<ItemRequestLineResponseDTO> getLines() {
        return lines;
    }

    public void setLines(List<ItemRequestLineResponseDTO> lines) {
        this.lines = lines;
    }
}
