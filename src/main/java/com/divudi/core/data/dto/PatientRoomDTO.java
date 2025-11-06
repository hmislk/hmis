package com.divudi.core.data.dto;

import java.io.Serializable;

public class PatientRoomDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private Long admissionId;
    private String bhtNo;
    private String currentRoomNo;
    private Boolean inPatient;

    public PatientRoomDTO() {
    }

    // Use find Rooms in Nursing Work Bench
    public PatientRoomDTO(Long id, Long admissionId, String bhtNo, String currentRoomNo, Boolean inPatient) {
        this.id = id;
        this.admissionId = admissionId;
        this.bhtNo = bhtNo;
        this.currentRoomNo = currentRoomNo;
        this.inPatient = inPatient;
    }
    
    // Use find BHT in Nursing Work Bench
    public PatientRoomDTO(Long admissionId, String bhtNo, String currentRoomNo) {
        this.admissionId = admissionId;
        this.bhtNo = bhtNo;
        this.currentRoomNo = currentRoomNo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getCurrentRoomNo() {
        return currentRoomNo;
    }

    public void setCurrentRoomNo(String currentRoomNo) {
        this.currentRoomNo = currentRoomNo;
    }

    public Boolean getInPatient() {
        return inPatient;
    }

    public void setInPatient(Boolean inPatient) {
        this.inPatient = inPatient;
    }

}

