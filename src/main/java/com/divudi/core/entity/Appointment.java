/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.data.AppointmentType;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Buddhika
 */
@Entity
public class Appointment extends PatientEncounter implements Serializable {
    @OneToOne
    private Bill bill;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date appointmentDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date appointmentTimeFrom;
    @Temporal(javax.persistence.TemporalType.TIME)
    private Date appointmentTimeTo;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PatientEncounter patientEncounter;
    
    @ManyToOne
    private Patient patient;
    
    @Enumerated(EnumType.STRING)
    private AppointmentType appointmentType;
    
    private String appointmentNumber;
    
    private boolean appointmentComplete;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date appointmentCompleteAt;
    @ManyToOne
    private WebUser appointmentCompleteBy;
    
    private boolean appointmentCancel;
    private String appointmentCancelReason;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date appointmentCancelAt;
    @ManyToOne
    private WebUser appointmentCancelBy;
    
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Appointment)) {
            return false;
        }
        Appointment other = (Appointment) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.Appointment[ id=" + id + " ]";
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public String getRetireComments() {
        return retireComments;
    }

    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public Date getAppointmentTimeFrom() {
        return appointmentTimeFrom;
    }

    public void setAppointmentTimeFrom(Date appointmentTimeFrom) {
        this.appointmentTimeFrom = appointmentTimeFrom;
    }

    public Date getAppointmentTimeTo() {
        return appointmentTimeTo;
    }

    public void setAppointmentTimeTo(Date appointmentTimeTo) {
        this.appointmentTimeTo = appointmentTimeTo;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public AppointmentType getAppointmentType() {
        return appointmentType;
    }

    public void setAppointmentType(AppointmentType appointmentType) {
        this.appointmentType = appointmentType;
    }

    public String getAppointmentNumber() {
        return appointmentNumber;
    }

    public void setAppointmentNumber(String appointmentNumber) {
        this.appointmentNumber = appointmentNumber;
    }

    public boolean isAppointmentComplete() {
        return appointmentComplete;
    }

    public void setAppointmentComplete(boolean appointmentComplete) {
        this.appointmentComplete = appointmentComplete;
    }

    public Date getAppointmentCompleteAt() {
        return appointmentCompleteAt;
    }

    public void setAppointmentCompleteAt(Date appointmentCompleteAt) {
        this.appointmentCompleteAt = appointmentCompleteAt;
    }

    public WebUser getAppointmentCompleteBy() {
        return appointmentCompleteBy;
    }

    public void setAppointmentCompleteBy(WebUser appointmentCompleteBy) {
        this.appointmentCompleteBy = appointmentCompleteBy;
    }

    public boolean isAppointmentCancel() {
        return appointmentCancel;
    }

    public void setAppointmentCancel(boolean appointmentCancel) {
        this.appointmentCancel = appointmentCancel;
    }

    public String getAppointmentCancelReason() {
        return appointmentCancelReason;
    }

    public void setAppointmentCancelReason(String appointmentCancelReason) {
        this.appointmentCancelReason = appointmentCancelReason;
    }

    public Date getAppointmentCancelAt() {
        return appointmentCancelAt;
    }

    public void setAppointmentCancelAt(Date appointmentCancelAt) {
        this.appointmentCancelAt = appointmentCancelAt;
    }

    public WebUser getAppointmentCancelBy() {
        return appointmentCancelBy;
    }

    public void setAppointmentCancelBy(WebUser appointmentCancelBy) {
        this.appointmentCancelBy = appointmentCancelBy;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

}
