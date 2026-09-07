package com.divudi.core.data.dto;

import com.divudi.core.entity.PatientEncounter;

public class PatientEncounterDto {

    private Long id;
    private String patientName;
    private String bhtNo;
    private String phn;
    private PatientEncounter patientEncounter;

    public PatientEncounterDto() {
    }

    public PatientEncounterDto(Long id, String patientName, String bhtNo, String phn) {
        this.id = id;
        this.patientName = patientName;
        this.bhtNo = bhtNo;
        this.phn = phn;
    }

    public PatientEncounterDto(PatientEncounter pe) {
        if (pe != null) {
            this.id = pe.getId();
            if (pe.getPatient() != null && pe.getPatient().getPerson() != null) {
                this.patientName = pe.getPatient().getPerson().getName();
            }
            this.bhtNo = pe.getBhtNo();
            if (pe.getPatient() != null) {
                this.phn = pe.getPatient().getPhn();
            }
            this.patientEncounter = pe;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPhn() {
        return phn;
    }

    public void setPhn(String phn) {
        this.phn = phn;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PatientEncounterDto that = (PatientEncounterDto) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

}
