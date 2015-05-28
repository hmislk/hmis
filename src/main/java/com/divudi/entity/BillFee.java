/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.FeeType;
import com.divudi.entity.inward.PatientRoom;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;



/**
 *
 * @author www.divudi.com
 */
@Entity
public class BillFee implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Edited Properties
    @ManyToOne
    private WebUser editor;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    ///////////////////////
    @ManyToOne
    Fee fee;
    @ManyToOne
    Patient patient;
    @ManyToOne
    PatientEncounter patienEncounter;
    @ManyToOne
    PatientEncounter childEncounter;
    @ManyToOne
    Staff staff;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Speciality speciality;
    //FeeDate, FeeTime
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date FeeAt;
    @ManyToOne
    private BillFee referenceBillFee;
    @ManyToOne
    private PatientItem patientItem;
    @ManyToOne
    PriceMatrix priceMatrix;
    //////////////////
    @ManyToOne
    BillItem billItem;
    @ManyToOne
    Bill bill;
    ///////////////
    double feeValue = 0.0;
    Double feeGrossValue;
    double feeDiscount;
    double feeMargin;
    double feeAdjusted;
    double paidValue = 0.0;
    double settleValue = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    private BillItem referenceBillItem;
    int orderNo;

    @Transient
    private double transSerial;
    @Transient
    double transNetValue;
    @ManyToOne
    private PatientRoom referencePatientRoom;

    public PriceMatrix getPriceMatrix() {
        return priceMatrix;
    }

    public void setPriceMatrix(PriceMatrix priceMatrix) {
        this.priceMatrix = priceMatrix;
    }

    public double getFeeAdjusted() {
        return feeAdjusted;
    }

    public void setFeeAdjusted(double feeAdjusted) {
        this.feeAdjusted = feeAdjusted;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public double getTransNetValue() {
        transNetValue = (feeValue + feeMargin) - feeDiscount;
        return transNetValue;
    }

    public void setTransNetValue(double transNetValue) {
        this.transNetValue = transNetValue;
    }

    public void copy(BillFee billFee) {
        fee = billFee.getFee();
        patient = billFee.getPatient();
        patienEncounter = billFee.getPatienEncounter();
        childEncounter = billFee.getChildEncounter();
        staff = billFee.getStaff();
        institution = billFee.getInstitution();
        department = billFee.getDepartment();
        speciality = billFee.getSpeciality();
        FeeAt = billFee.getFeeAt();
        billItem = billFee.getBillItem();
        bill = billFee.getBill();
        feeValue = billFee.getFeeValue();
        feeGrossValue = billFee.getFeeGrossValue();
        feeAdjusted = billFee.getFeeAdjusted();
        feeDiscount = billFee.getFeeDiscount();
        feeMargin = billFee.getFeeMargin();
        paidValue = billFee.getPaidValue();

    }

    public void invertValue(BillFee billFee) {
        if (billFee == null) {
            return;
        }
       
        feeValue = 0 - billFee.getFeeValue();
        if (billFee.getFeeGrossValue() != null) {
            feeGrossValue = 0 - billFee.getFeeGrossValue();
        }
        feeDiscount = 0 - billFee.getFeeDiscount();
        feeMargin = 0 - billFee.getFeeMargin();
        feeAdjusted = 0 - billFee.getFeeAdjusted();
        paidValue = 0 - billFee.getPaidValue();
    }

    public BillFee() {
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {

        if (!(object instanceof BillFee)) {
            return false;
        }
        BillFee other = (BillFee) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.BillFee[ id=" + id + " ]";
    }

    public void setFeeValue(boolean foriegn) {
        if (tmpChangedValue != null) {
            this.feeGrossValue = tmpChangedValue;
            this.feeValue = tmpChangedValue;
            return;
        }

        if (foriegn) {
            this.feeGrossValue = getFee().getFfee();
            this.feeValue = getFee().getFfee();
        } else {
            this.feeGrossValue = getFee().getFee();
            this.feeValue = getFee().getFee();
        }
        //    ////System.out.println("Setting fee value as " + feeValue);
    }

    public void setFeeValueForDiscountAllowedAndUserChangable(boolean foriegn, double discountPercent) {
        if (tmpChangedValue != null) {
            this.feeGrossValue = tmpChangedValue;
            this.feeValue = tmpChangedValue;
            return;
        }

        if (discountPercent == 0) {
            if (foriegn) {
                this.feeGrossValue = getFee().getFfee();
                this.feeValue = getFee().getFfee();
            } else {
                this.feeGrossValue = getFee().getFee();
                this.feeValue = getFee().getFee();

            }
        }

        if (discountPercent != 0) {
            if (foriegn) {
                this.feeGrossValue = getFee().getFfee();
                this.feeValue = getFee().getFfee() / 100 * (100 - discountPercent);
            } else {
                this.feeGrossValue = getFee().getFee();
                this.feeValue = getFee().getFee() / 100 * (100 - discountPercent);

            }
        }

    }

    public void setFeeValueForDiscountAllowedNotUserChangable(boolean foriegn, double discountPercent) {
//        if (tmpChangedValue != 0) {
//            this.feeGrossValue = tmpChangedValue;
//            this.feeValue = tmpChangedValue;
//            return;
//        }

        if (discountPercent == 0) {
            if (foriegn) {
                this.feeGrossValue = getFee().getFfee();
                this.feeValue = getFee().getFfee();
            } else {
                this.feeGrossValue = getFee().getFee();
                this.feeValue = getFee().getFee();

            }
        }

        if (discountPercent != 0) {
            if (foriegn) {
                this.feeGrossValue = getFee().getFfee();
                this.feeValue = getFee().getFfee() / 100 * (100 - discountPercent);
            } else {
                this.feeGrossValue = getFee().getFee();
                this.feeValue = getFee().getFee() / 100 * (100 - discountPercent);

            }
        }

    }

    public void setFeeValueForUserChangableAndNotDiscountAllowed(boolean foriegn) {
        if (tmpChangedValue != null) {
            this.feeGrossValue = tmpChangedValue;
            this.feeValue = tmpChangedValue;
            return;
        }

        if (foriegn) {
            this.feeGrossValue = getFee().getFfee();
            this.feeValue = getFee().getFfee();
        } else {
            this.feeGrossValue = getFee().getFee();
            this.feeValue = getFee().getFee();

        }

    }

    @Transient
    private Double tmpChangedValue;

    public void setFeeValueForCreditCompany(boolean foriegn, double discountPercent) {
        if (tmpChangedValue == null) {
            if (getFee().getFeeType() != FeeType.Staff) {
                if (foriegn) {
                    this.feeGrossValue = getFee().getFfee();
                    this.feeDiscount = getFee().getFfee() / 100 * (discountPercent);
                    this.feeValue = feeGrossValue - feeDiscount;
                } else {
                    this.feeGrossValue = getFee().getFee();
                    this.feeDiscount = getFee().getFee() / 100 * (discountPercent);
                    this.feeValue = feeGrossValue - feeDiscount;
                }

            } else {
                if (foriegn) {
                    this.feeGrossValue = getFee().getFfee();
                    this.feeValue = getFee().getFfee();
                } else {
                    this.feeGrossValue = getFee().getFee();
                    this.feeValue = getFee().getFee();
                }
            }
        } else {
            if (getFee().getFeeType() != FeeType.Staff) {
                this.feeGrossValue = tmpChangedValue;
                if (tmpChangedValue != 0) {
                    this.feeDiscount = feeGrossValue / 100 * (discountPercent);
                    this.feeValue = feeGrossValue - feeDiscount;
                } else {
                    this.feeValue = 0;
                }
            } else {
                this.feeGrossValue = tmpChangedValue;
                this.feeValue = tmpChangedValue;
            }
        }
    }

    public void setFeeValue(boolean foriegn, double discountPercent) {

        if (tmpChangedValue == null) {
            if (getFee().getFeeType() != FeeType.Staff) {
                if (foriegn) {
                    this.feeGrossValue = getFee().getFfee();
                } else {
                    this.feeGrossValue = getFee().getFee();
                }

                //SETTING DISCOUNT
                this.feeDiscount = this.feeGrossValue * (discountPercent / 100);
                this.feeValue = this.feeGrossValue - this.feeDiscount;

            } else {
                if (foriegn) {
                    this.feeGrossValue = getFee().getFfee();
                    this.feeValue = getFee().getFfee();
                } else {
                    this.feeGrossValue = getFee().getFee();
                    this.feeValue = getFee().getFee();
                }
            }
        } else {
            if (getFee().getFeeType() != FeeType.Staff) {
                this.feeGrossValue = tmpChangedValue;
                if (tmpChangedValue != 0) {
                    this.feeDiscount = this.feeGrossValue * (discountPercent / 100);
                    this.feeValue = this.feeGrossValue - this.feeDiscount;
                } else {
                    this.feeValue = 0;
                }
            } else {
                this.feeGrossValue = tmpChangedValue;
                this.feeValue = tmpChangedValue;
            }
        }

        System.err.println("DISCOUNT " + feeDiscount);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Fee getFee() {
        return fee;
    }

    public void setFee(Fee fee) {
        this.fee = fee;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getPatienEncounter() {
        return patienEncounter;
    }

    public void setPatienEncounter(PatientEncounter patienEncounter) {
        this.patienEncounter = patienEncounter;
    }

    public PatientEncounter getChildEncounter() {
        return childEncounter;
    }

    public void setChildEncounter(PatientEncounter childEncounter) {
        this.childEncounter = childEncounter;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public double getFeeValue() {
        return feeValue;
    }

    public void setFeeValue(double feeValue) {
        this.feeValue = feeValue;
    }

    public double getFeeDiscount() {
        return feeDiscount;
    }

    public void setFeeDiscount(double feeDiscount) {
        this.feeDiscount = feeDiscount;
    }

    public double getFeeMargin() {
        return feeMargin;
    }

    public void setFeeMargin(double feeMargin) {
        this.feeMargin = feeMargin;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public double getPaidValue() {
        return paidValue;
    }

    public void setPaidValue(double paidValue) {
        this.paidValue = paidValue;
    }

    public Date getFeeAt() {
        return FeeAt;
    }

    public void setFeeAt(Date FeeAt) {
        this.FeeAt = FeeAt;
    }

    public Double getTmpChangedValue() {
        return tmpChangedValue;
    }

    public void setTmpChangedValue(Double tmpChangedValue) {
        this.tmpChangedValue = tmpChangedValue;
    }

    public BillFee getReferenceBillFee() {
        return referenceBillFee;
    }

    public void setReferenceBillFee(BillFee referenceBillFee) {
        this.referenceBillFee = referenceBillFee;
    }

    public double getTransSerial() {
        return transSerial;
    }

    public void setTransSerial(double transSerial) {
        this.transSerial = transSerial;
    }

    public PatientItem getPatientItem() {
        return patientItem;
    }

    public void setPatientItem(PatientItem patientItem) {
        this.patientItem = patientItem;
    }

    public WebUser getEditor() {
        return editor;
    }

    public void setEditor(WebUser editor) {
        this.editor = editor;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public BillItem getReferenceBillItem() {
        return referenceBillItem;
    }

    public void setReferenceBillItem(BillItem referenceBillItem) {
        this.referenceBillItem = referenceBillItem;
    }

    public PatientRoom getReferencePatientRoom() {
        return referencePatientRoom;
    }

    public void setReferencePatientRoom(PatientRoom referencePatientRoom) {
        this.referencePatientRoom = referencePatientRoom;
    }

    public Double getFeeGrossValue() {
        return feeGrossValue;
    }

    public void setFeeGrossValue(Double feeGrossValue) {
        this.feeGrossValue = feeGrossValue;
    }

    public double getSettleValue() {
        return settleValue;
    }

    public void setSettleValue(double settleValue) {
        this.settleValue = settleValue;
    }

}
