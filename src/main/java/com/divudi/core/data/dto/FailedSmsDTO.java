/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.divudi.core.data.dto;

import com.divudi.core.data.MessageType;
import com.divudi.core.data.Title;
import java.util.Date;

public class FailedSmsDTO {

    private Long smsId;
    private Date createdAt;
    private Date sentAt;
    private MessageType smsType;
    private String message;
    private String recipient;
    private Boolean success;
    private Boolean pending;
    private String failureMessage;

    // Optional but future-safe
    private String sourceModule;
    private Integer retryCount;

    private Title patientTitle;
    private String patientName;

    public FailedSmsDTO(Long smsId,
            Date createdAt,
            Date sentAt,
            MessageType smsType,
            String message,
            String recipient,
            Boolean success,
            Boolean pending,
            String failureMessage,
            Title patientTitle,
            String patientName) {

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

    // ---------------- Getters ----------------
    public Long getSmsId() {
        return smsId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public MessageType getSmsType() {
        return smsType;
    }

    public String getMessage() {
        return message;
    }

    public String getRecipient() {
        return recipient;
    }

    public Boolean getSuccess() {
        return success;
    }

    public Boolean getPending() {
        return pending;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public Title getPatientTitle() {
        return patientTitle;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientNameWithTitle() {
        return (patientTitle != null ? patientTitle.getLabel() : "") + " " + patientName;
    }
}
