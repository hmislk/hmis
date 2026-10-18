package com.divudi.core.entity.clinical;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PatientEncounter;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * One record (admission or OPD bill) included in a {@link CoveringLetter}.
 */
@Entity
public class CoveringLetterItem implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private CoveringLetter coveringLetter;
    @ManyToOne
    private PatientEncounter patientEncounter;
    @ManyToOne
    private Bill bill;
    private double amount;
    private int orderNo;
    private boolean retired;

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
        if (!(object instanceof CoveringLetterItem)) {
            return false;
        }
        CoveringLetterItem other = (CoveringLetterItem) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.clinical.CoveringLetterItem[ id=" + id + " ]";
    }

    public CoveringLetter getCoveringLetter() {
        return coveringLetter;
    }

    public void setCoveringLetter(CoveringLetter coveringLetter) {
        this.coveringLetter = coveringLetter;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

}
