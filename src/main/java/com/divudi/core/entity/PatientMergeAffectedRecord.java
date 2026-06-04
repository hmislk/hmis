package com.divudi.core.entity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "patientmergeaffectedrecord")
public class PatientMergeAffectedRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PatientMergeRecord mergeRecord;

    private String entityClass;

    private Long entityId;

    private Long oldPatientId;

    private Long newPatientId;

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientMergeRecord getMergeRecord() {
        return mergeRecord;
    }

    public void setMergeRecord(PatientMergeRecord mergeRecord) {
        this.mergeRecord = mergeRecord;
    }

    public String getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(String entityClass) {
        this.entityClass = entityClass;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public Long getOldPatientId() {
        return oldPatientId;
    }

    public void setOldPatientId(Long oldPatientId) {
        this.oldPatientId = oldPatientId;
    }

    public Long getNewPatientId() {
        return newPatientId;
    }

    public void setNewPatientId(Long newPatientId) {
        this.newPatientId = newPatientId;
    }
    // </editor-fold>
}
