/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import com.divudi.core.data.MessageType;
import com.divudi.core.data.Title;
import java.util.Date;

/**
 *
 * @author ASUS
 */
public class SmsDTO {

    private Long smsId;
    private Date createdAt;
    private Date sentAt;
    private MessageType smsType;
    private String message;
    private String recipient;
    private Boolean success;
    private Boolean pending;
    private String failureMessage;
    private String sourceModule;
    private Title patientTitle;
    private String patientName;
    private String patientNameWithTitle;
    

    public SmsDTO(Long smsId, Date createdAt, Date sentAt, MessageType smsType, String message, String recipient, Boolean success, Boolean pending,String failureMessage, Title patientTitle, String patientName) {
        this.smsId = smsId;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.smsType = smsType;
        this.message = message;
        this.recipient = recipient;
        this.success = success;
        this.pending = pending;
        this.failureMessage = failureMessage;
        this.patientTitle = patientTitle;
        this.patientName = patientName;
    }
    

    // Getters and Setters
    public Long getSmsId() {
        return smsId;
    }

    public void setSmsId(Long smsId) {
        this.smsId = smsId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public MessageType getSmsType() {
        return smsType;
    }

    public void setSmsType(MessageType smsType) {
        this.smsType = smsType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public void setSourceModule(String sourceModule) {
        this.sourceModule = sourceModule;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public void setPatientTitle(Title patientTitle) {
        this.patientTitle = patientTitle;
    }

    public String getPatientNameWithTitle() {
        String temT;
        Title t = getPatientTitle();
        if (t != null) {
            temT = t.getLabel();
        } else {
            temT = "";
        }
        patientNameWithTitle = temT + " " + getPatientName();
        return patientNameWithTitle;
    }

    public void setPatientNameWithTitle(String patientNameWithTitle) {
        this.patientNameWithTitle = patientNameWithTitle;
    }

    public Boolean getPending() {
        return pending;
    }

    public void setPending(Boolean pending) {
        this.pending = pending;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }
}
