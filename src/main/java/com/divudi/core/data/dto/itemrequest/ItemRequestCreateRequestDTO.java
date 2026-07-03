/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.itemrequest;

import java.io.Serializable;
import java.util.List;

/**
 * Request body for submitting a new item/service request against a patient's
 * active BHT. Sent by external systems to POST /api/itemrequests.
 *
 * @author Claude AI Assistant
 */
public class ItemRequestCreateRequestDTO implements Serializable {

    private String bhtNo;
    private Long targetDepartmentId;
    private String comments;
    private List<ItemRequestLineDTO> lines;

    public ItemRequestCreateRequestDTO() {
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

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public List<ItemRequestLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<ItemRequestLineDTO> lines) {
        this.lines = lines;
    }
}
