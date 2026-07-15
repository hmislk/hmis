package com.divudi.core.entity.clinical;

import com.divudi.core.data.MedicationAdministrationStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.RetirableEntity;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 * A single point-in-time dose-administration event recorded by ward staff
 * against ward-held pharmacy stock (#21469, part of #21466).
 *
 * <p>Distinct from {@link Prescription}, which represents the continuous
 * medication order. Each {@code MedicationAdministrationRecord} represents
 * one dose actually given (or refused/held) at a point in time. Only AMP
 * items (never VMPs) may be administered, and {@link #itemBatch} records the
 * specific batch given for traceability.</p>
 *
 * <p>Stock is NOT deducted at the point each record is created -
 * {@link #stockDeducted} stays {@code false} until one or more records are
 * later selected and deducted from ward stock together in a single batch
 * transaction ({@link #stockDeductionBill}).</p>
 */
@Entity
@Table(name = "MEDICATIONADMINISTRATIONRECORD")
public class MedicationAdministrationRecord implements Serializable, RetirableEntity {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private PatientEncounter patientEncounter;

    @ManyToOne
    private Prescription prescription;

    @ManyToOne
    private Item item;

    @ManyToOne
    private ItemBatch itemBatch;

    private Double qty;

    private String doseDescription;

    @Temporal(TemporalType.TIMESTAMP)
    private Date administeredAt;

    @ManyToOne
    private Staff administeredBy;

    @ManyToOne
    private Department department;

    @Enumerated(EnumType.STRING)
    private MedicationAdministrationStatus status;

    private String comments;

    private boolean stockDeducted;

    @ManyToOne
    private Bill stockDeductionBill;

    @ManyToOne
    private WebUser creater;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

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

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public ItemBatch getItemBatch() {
        return itemBatch;
    }

    public void setItemBatch(ItemBatch itemBatch) {
        this.itemBatch = itemBatch;
    }

    public Double getQty() {
        return qty;
    }

    public void setQty(Double qty) {
        this.qty = qty;
    }

    public String getDoseDescription() {
        return doseDescription;
    }

    public void setDoseDescription(String doseDescription) {
        this.doseDescription = doseDescription;
    }

    public Date getAdministeredAt() {
        return administeredAt;
    }

    public void setAdministeredAt(Date administeredAt) {
        this.administeredAt = administeredAt;
    }

    public Staff getAdministeredBy() {
        return administeredBy;
    }

    public void setAdministeredBy(Staff administeredBy) {
        this.administeredBy = administeredBy;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public MedicationAdministrationStatus getStatus() {
        return status;
    }

    public void setStatus(MedicationAdministrationStatus status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public boolean isStockDeducted() {
        return stockDeducted;
    }

    public void setStockDeducted(boolean stockDeducted) {
        this.stockDeducted = stockDeducted;
    }

    public Bill getStockDeductionBill() {
        return stockDeductionBill;
    }

    public void setStockDeductionBill(Bill stockDeductionBill) {
        this.stockDeductionBill = stockDeductionBill;
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

    @Override
    public boolean isRetired() {
        return retired;
    }

    @Override
    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    @Override
    public WebUser getRetirer() {
        return retirer;
    }

    @Override
    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    @Override
    public Date getRetiredAt() {
        return retiredAt;
    }

    @Override
    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    @Override
    public String getRetireComments() {
        return retireComments;
    }

    @Override
    public void setRetireComments(String retireComments) {
        this.retireComments = retireComments;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof MedicationAdministrationRecord)) {
            return false;
        }
        MedicationAdministrationRecord other = (MedicationAdministrationRecord) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.clinical.MedicationAdministrationRecord[ id=" + id + " ]";
    }

}
