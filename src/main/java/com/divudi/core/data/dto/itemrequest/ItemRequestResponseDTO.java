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
    private Date createdAt;
    private String createdBy;
    private List<Long> fulfillingBillIds;
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

    public List<Long> getFulfillingBillIds() {
        return fulfillingBillIds;
    }

    public void setFulfillingBillIds(List<Long> fulfillingBillIds) {
        this.fulfillingBillIds = fulfillingBillIds;
    }

    public List<ItemRequestLineResponseDTO> getLines() {
        return lines;
    }

    public void setLines(List<ItemRequestLineResponseDTO> lines) {
        this.lines = lines;
    }
}
