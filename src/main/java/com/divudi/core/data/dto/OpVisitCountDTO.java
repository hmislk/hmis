package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * One row of the OP Visits report (#25854, #25856): a group (department or specialty) with
 * its number of visits (one visit = one OPD bill) and of distinct patients.
 */
public class OpVisitCountDTO implements Serializable {

    private String label;
    private long visits;
    private long patients;

    public OpVisitCountDTO() {
    }

    public OpVisitCountDTO(String label, long visits, long patients) {
        this.label = label;
        this.visits = visits;
        this.patients = patients;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public long getVisits() {
        return visits;
    }

    public void setVisits(long visits) {
        this.visits = visits;
    }

    public long getPatients() {
        return patients;
    }

    public void setPatients(long patients) {
        this.patients = patients;
    }
}
