package com.divudi.core.entity;

import com.divudi.core.data.PatientMergeStatus;
import com.divudi.core.data.PatientMergeType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "patientmergerecord")
public class PatientMergeRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date mergeDate;

    @ManyToOne
    private WebUser mergedBy;

    @ManyToOne
    private Patient primaryPatient;

    @ManyToOne
    private Patient secondaryPatient;

    @Lob
    private String primarySnapshotJson;

    @Lob
    private String secondarySnapshotJson;

    private String mergeReason;

    @Enumerated(EnumType.STRING)
    private PatientMergeType mergeType;

    @Enumerated(EnumType.STRING)
    private PatientMergeStatus status;

    @ManyToOne
    private WebUser reversedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date reversedAt;

    @OneToMany(mappedBy = "mergeRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PatientMergeAffectedRecord> affectedRecords = new ArrayList<>();

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getMergeDate() {
        return mergeDate;
    }

    public void setMergeDate(Date mergeDate) {
        this.mergeDate = mergeDate;
    }

    public WebUser getMergedBy() {
        return mergedBy;
    }

    public void setMergedBy(WebUser mergedBy) {
        this.mergedBy = mergedBy;
    }

    public Patient getPrimaryPatient() {
        return primaryPatient;
    }

    public void setPrimaryPatient(Patient primaryPatient) {
        this.primaryPatient = primaryPatient;
    }

    public Patient getSecondaryPatient() {
        return secondaryPatient;
    }

    public void setSecondaryPatient(Patient secondaryPatient) {
        this.secondaryPatient = secondaryPatient;
    }

    public String getPrimarySnapshotJson() {
        return primarySnapshotJson;
    }

    public void setPrimarySnapshotJson(String primarySnapshotJson) {
        this.primarySnapshotJson = primarySnapshotJson;
    }

    public String getSecondarySnapshotJson() {
        return secondarySnapshotJson;
    }

    public void setSecondarySnapshotJson(String secondarySnapshotJson) {
        this.secondarySnapshotJson = secondarySnapshotJson;
    }

    public String getMergeReason() {
        return mergeReason;
    }

    public void setMergeReason(String mergeReason) {
        this.mergeReason = mergeReason;
    }

    public PatientMergeType getMergeType() {
        return mergeType;
    }

    public void setMergeType(PatientMergeType mergeType) {
        this.mergeType = mergeType;
    }

    public PatientMergeStatus getStatus() {
        return status;
    }

    public void setStatus(PatientMergeStatus status) {
        this.status = status;
    }

    public WebUser getReversedBy() {
        return reversedBy;
    }

    public void setReversedBy(WebUser reversedBy) {
        this.reversedBy = reversedBy;
    }

    public Date getReversedAt() {
        return reversedAt;
    }

    public void setReversedAt(Date reversedAt) {
        this.reversedAt = reversedAt;
    }

    public List<PatientMergeAffectedRecord> getAffectedRecords() {
        return affectedRecords;
    }

    public void setAffectedRecords(List<PatientMergeAffectedRecord> affectedRecords) {
        this.affectedRecords = affectedRecords;
    }
    // </editor-fold>
}
