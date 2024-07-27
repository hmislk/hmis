package com.divudi.data.lab;

import com.divudi.entity.Bill;
import com.divudi.java.CommonFunctions;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a barcode associated with a bill and its related patient investigations and samples.
 * 
 * @version 1.0
 * @since 2024-07-27
 */
public class BillBarcode {
    private Bill bill;    
    private List<PatientInvestigationWrapper> patientInvestigationWrappers;
    private List<PatientSampleWrapper> patientSampleWrappers;
    private String uuid;

    public BillBarcode() {
    }

    public BillBarcode(Bill bill) {
        this.bill = bill;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public List<PatientInvestigationWrapper> getPatientInvestigationWrappers() {
        if (patientInvestigationWrappers == null) {
            patientInvestigationWrappers = new ArrayList<>();
        }
        return patientInvestigationWrappers;
    }

    public void setPatientInvestigationWrappers(List<PatientInvestigationWrapper> patientInvestigationWrappers) {
        this.patientInvestigationWrappers = patientInvestigationWrappers;
    }

    public List<PatientSampleWrapper> getPatientSampleWrappers() {
        if (patientSampleWrappers == null) {
            patientSampleWrappers = new ArrayList<>();
        }
        return patientSampleWrappers;
    }

    public void setPatientSampleWrappers(List<PatientSampleWrapper> patientSampleWrappers) {
        this.patientSampleWrappers = patientSampleWrappers;
    }

    public String getUuid() {
        if (uuid == null || uuid.trim().equals("")) {
            uuid = CommonFunctions.generateUuid();
        }
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "BillBarcode{" +
                "uuid='" + getUuid() + '\'' +
                ", bill=" + bill +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BillBarcode that = (BillBarcode) obj;
        return Objects.equals(getUuid(), that.getUuid());
    }
}
