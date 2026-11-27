package com.divudi.core.entity.inward;

import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * One period of an admission during which a given admission type was in force
 * (#25814). An admission that never changed type has no rows: its current
 * admission type then covers the whole stay. When the type is changed for the
 * first time two rows are written (old type from admission to the change, new
 * type from the change, with a null toDate); later changes close the open row
 * and add another.
 */
@Entity
public class AdmissionTypeHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PatientEncounter patientEncounter;
    @ManyToOne
    private AdmissionType admissionType;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    /** Null means this type is still in force. */
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;

    @Column(length = 500)
    private String reason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @ManyToOne
    private WebUser creater;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
}
