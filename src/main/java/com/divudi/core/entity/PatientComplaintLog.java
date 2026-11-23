package com.divudi.core.entity;

import com.divudi.core.data.PatientComplaintAction;
import com.divudi.core.data.PatientComplaintStatus;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * One line in the history of a patient complaint: who did what and when
 * (#25886). Staff only; never shown to the patient.
 */
@Entity
public class PatientComplaintLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PatientComplaint complaint;

    @Enumerated(EnumType.STRING)
    private PatientComplaintAction action;
    @Enumerated(EnumType.STRING)
    private PatientComplaintStatus fromStatus;
    @Enumerated(EnumType.STRING)
    private PatientComplaintStatus toStatus;
    @Column(length = 2000)
    private String note;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    /** Null when the patient submitted it in the portal; the patient is stored instead. */
    @ManyToOne
    private WebUser creater;
    @ManyToOne
    private Patient patient;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientComplaint getComplaint() {
        return complaint;
    }

    public void setComplaint(PatientComplaint complaint) {
        this.complaint = complaint;
    }

    public PatientComplaintAction getAction() {
        return action;
    }

    public void setAction(PatientComplaintAction action) {
        this.action = action;
    }

    public PatientComplaintStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(PatientComplaintStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public PatientComplaintStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(PatientComplaintStatus toStatus) {
        this.toStatus = toStatus;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }
}
