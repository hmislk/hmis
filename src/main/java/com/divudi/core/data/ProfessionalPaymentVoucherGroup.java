package com.divudi.core.data;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One per-patient (OPD) or per-BHT (inward) block of a professional payment
 * bill, used to print individual payment vouchers separated by page breaks.
 */
public class ProfessionalPaymentVoucherGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private Patient patient;
    private PatientEncounter patientEncounter;
    private List<BillItem> billItems = new ArrayList<>();
    private double subtotal;

    public void addBillItem(BillItem billItem) {
        if (billItem == null) {
            return;
        }
        billItems.add(billItem);
        subtotal += billItem.getNetValue();
    }

    public String getDisplayName() {
        if (patientEncounter != null
                && patientEncounter.getPatient() != null
                && patientEncounter.getPatient().getPerson() != null) {
            return patientEncounter.getPatient().getPerson().getNameWithTitle();
        }
        if (patient != null && patient.getPerson() != null) {
            return patient.getPerson().getNameWithTitle();
        }
        return "Miscellaneous";
    }

    public String getDisplayIdentifier() {
        if (patientEncounter != null) {
            return "BHT : " + (patientEncounter.getBhtNo() == null ? "" : patientEncounter.getBhtNo());
        }
        if (patient != null) {
            return "MRN : " + (patient.getPhn() == null ? "" : patient.getPhn());
        }
        return "";
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
