/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.entity;

import com.divudi.core.data.FeeType;
import com.divudi.core.entity.inward.InpatientPackageItem;
import com.divudi.core.entity.inward.PatientRoom;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author www.divudi.com
 */
@Entity
public class BillFee implements Serializable, RetirableEntity {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private double feeValue = 0.0;
    private Double feeGrossValue;
    private double feeDiscount;
    private double feeVat;
    private double feeVatPlusValue;
    private boolean freeOfCharge;

    @Transient
    private double absoluteFeeValue;

    @Transient
    private Double feeGrossValueTransient;
    @Transient
    private boolean userChangedTheGrossValueTransient;

    double feeMargin;
    double feeAdjusted;

    // Per-unit rate fields — store the rate for a single unit before qty multiplication.
    // The main fee fields (feeGrossValue, feeValue, feeMargin, feeDiscount) hold totals (unit × qty).
    private Double feeUnitGrossValue;
    private Double feeUnitValue;
    private Double feeUnitMargin;
    private Double feeUnitDiscount;

    //This records the payment made for the payment due staff or institution
    double paidValue = 0.0;
    //This records the value paid out of the total from the customer
    double settleValue = 0.0;

    // Indicates if the bill is fully settled by the client
    private Boolean fullySettled;

    // Indicates if the payment has been completed to the professional or institution
    private Boolean completedPayment;
    
    // Indicates if the fee has been collected directly by the surgeon/doctor
    private boolean feeCollectedByDoctor;

    // Per-item professional payment hold — blocks payment of this specific fee only
    // (mirrors PatientEncounter.professionalPaymentsOnHold, which holds the whole BHT). (Issue #22383)
    private Boolean feePaymentOnHold = false;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date feePaymentHoldDateTime;
    @ManyToOne
    private WebUser feePaymentHoldBy;
    @Lob
    private String feePaymentHoldNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    private BillItem referenceBillItem;
    int orderNo;

    private Double overriddenRate;
    private boolean fromPackage;
    @ManyToOne
    private InpatientPackageItem sourcePackageItem;

    private boolean returned;

    @Transient
    private double transSerial;
    @Transient
    double transNetValue;
    @ManyToOne
    private PatientRoom referencePatientRoom;

    @Transient
    private final String uuid;

    public boolean isFreeOfCharge() {
        return freeOfCharge;
    }

    public void setFreeOfCharge(boolean freeOfCharge) {
        this.freeOfCharge = freeOfCharge;
    }

    public BillFee(String uuid) {
        this.uuid = uuid;
    }

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

    public Double getOverriddenRate() {
        return overriddenRate;
    }

    public void setOverriddenRate(Double overriddenRate) {
        this.overriddenRate = overriddenRate;
    }

    public boolean isFromPackage() {
        return fromPackage;
    }

    public void setFromPackage(boolean fromPackage) {
        this.fromPackage = fromPackage;
    }

    public InpatientPackageItem getSourcePackageItem() {
        return sourcePackageItem;
    }

    public void setSourcePackageItem(InpatientPackageItem sourcePackageItem) {
        this.sourcePackageItem = sourcePackageItem;
    }

    public double getTransNetValue() {
        transNetValue = (feeValue + feeMargin) - feeDiscount + feeVat;
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
        feeVat = billFee.getFeeVat();
        feeVatPlusValue = billFee.getFeeVatPlusValue();
        feeMargin = billFee.getFeeMargin();
        paidValue = billFee.getPaidValue();
        feeUnitGrossValue = billFee.getFeeUnitGrossValue();
        feeUnitValue = billFee.getFeeUnitValue();
        feeUnitMargin = billFee.getFeeUnitMargin();
        feeUnitDiscount = billFee.getFeeUnitDiscount();
        overriddenRate = billFee.getOverriddenRate();
        fromPackage = billFee.isFromPackage();
        sourcePackageItem = billFee.getSourcePackageItem();
    }

    public void copyWithoutFinancialData(BillFee billFee) {
        fee = billFee.getFee();
        patient = billFee.getPatient();
        patienEncounter = billFee.getPatienEncounter();
        childEncounter = billFee.getChildEncounter();
        staff = billFee.getStaff();
        institution = billFee.getInstitution();
        department = billFee.getDepartment();
        speciality = billFee.getSpeciality();
        FeeAt = billFee.getFeeAt();
        fromPackage = billFee.isFromPackage();
        sourcePackageItem = billFee.getSourcePackageItem();
    }

    public double getFeeVatPlusValue() {
        return feeVatPlusValue;
    }

    public void setFeeVatPlusValue(double feeVatPlusValue) {
        this.feeVatPlusValue = feeVatPlusValue;
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
        feeVat = 0 - billFee.getFeeVat();
        feeVatPlusValue = 0 - billFee.getFeeVatPlusValue();
        feeMargin = 0 - billFee.getFeeMargin();
        feeAdjusted = 0 - billFee.getFeeAdjusted();
        paidValue = 0 - billFee.getPaidValue();
        feeUnitGrossValue = billFee.getFeeUnitGrossValue() != null ? 0 - billFee.getFeeUnitGrossValue() : null;
        feeUnitValue = billFee.getFeeUnitValue() != null ? 0 - billFee.getFeeUnitValue() : null;
        feeUnitMargin = billFee.getFeeUnitMargin() != null ? 0 - billFee.getFeeUnitMargin() : null;
        feeUnitDiscount = billFee.getFeeUnitDiscount() != null ? 0 - billFee.getFeeUnitDiscount() : null;
    }

    public void invertValue() {
        feeValue = 0 - feeValue;
        if (feeGrossValue != null) {
            feeGrossValue = 0 - feeGrossValue;
        }
        feeDiscount = 0 - feeDiscount;
        feeVat = 0 - feeVat;
        feeVatPlusValue = 0 - feeVatPlusValue;
        feeMargin = 0 - feeMargin;
        feeAdjusted = 0 - feeAdjusted;
        paidValue = 0 - paidValue;
        if (feeUnitGrossValue != null) feeUnitGrossValue = 0 - feeUnitGrossValue;
        if (feeUnitValue != null) feeUnitValue = 0 - feeUnitValue;
        if (feeUnitMargin != null) feeUnitMargin = 0 - feeUnitMargin;
        if (feeUnitDiscount != null) feeUnitDiscount = 0 - feeUnitDiscount;
    }

    public BillFee() {
        this.uuid = UUID.randomUUID().toString();
    }

    @Override
    public int hashCode() {
        // If id is not null, use id for hashcode, otherwise use fee and staff
        return Objects.hash(id != null ? id : Objects.hash(fee, staff));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof BillFee)) {
            return false;
        }
        BillFee other = (BillFee) object;

        if (this.id != null && other.id != null) {
            return this.id.equals(other.id);
        }
        return this.uuid.equals(other.uuid);
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.BillFee[ id=" + id + " ]";
    }

    public void setFeeValueBoolean(boolean foriegn) {
        if (tmpChangedValue != null) {
            this.feeUnitGrossValue = tmpChangedValue;
            this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
            this.feeValue = tmpChangedValue * this.getBillItem().getQty();
//            this.feeVatPlusValue = this.feeVat + this.feeValue;
            return;
        }

        if (foriegn) {
            this.feeUnitGrossValue = getFee().getFfee();
            this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
            this.feeValue = getFee().getFfee() * this.getBillItem().getQty();
//            this.feeVatPlusValue = this.feeVat + this.feeValue;
        } else {
            this.feeUnitGrossValue = getFee().getFee();
            this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
            this.feeValue = getFee().getFee() * this.getBillItem().getQty();
//            this.feeVatPlusValue = this.feeVat + this.feeValue;
        }
        //    //////// // System.out.println("Setting fee value as " + feeValue);
    }

    public void setFeeValueForDiscountAllowedAndUserChangable(boolean foriegn, double discountPercent) {
        if (tmpChangedValue != null) {
            this.feeUnitGrossValue = tmpChangedValue;
            this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
            this.feeValue = tmpChangedValue * this.getBillItem().getQty();
            return;
        }

        if (discountPercent == 0) {
            if (foriegn) {
                this.feeUnitGrossValue = getFee().getFfee();
                this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                this.feeValue = getFee().getFfee() * this.getBillItem().getQty();
            } else {
                this.feeUnitGrossValue = getFee().getFee();
                this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                this.feeValue = getFee().getFee() * this.getBillItem().getQty();

            }
        }

        if (discountPercent != 0) {
            if (foriegn) {
                this.feeUnitGrossValue = getFee().getFfee();
                this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                this.feeValue = (getFee().getFfee() / 100 * (100 - discountPercent)) * this.getBillItem().getQty();
            } else {
                this.feeUnitGrossValue = getFee().getFee();
                this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                this.feeValue = (getFee().getFee() / 100 * (100 - discountPercent)) * this.getBillItem().getQty();

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
                this.feeUnitGrossValue = getFee().getFfee();
                this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                this.feeValue = getFee().getFfee() * this.getBillItem().getQty();
            } else {
                this.feeUnitGrossValue = getFee().getFee();
                this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                this.feeValue = getFee().getFee() * this.getBillItem().getQty();

            }
        }

        if (discountPercent != 0) {
            if (foriegn) {
                this.feeUnitGrossValue = getFee().getFfee();
                this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                this.feeValue = (getFee().getFfee() / 100 * (100 - discountPercent)) * this.getBillItem().getQty();
            } else {
                this.feeUnitGrossValue = getFee().getFee();
                this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                this.feeValue = (getFee().getFee() / 100 * (100 - discountPercent)) * this.getBillItem().getQty();

            }
        }

    }

    public void setFeeValueForUserChangableAndNotDiscountAllowed(boolean foriegn) {
        if (tmpChangedValue != null) {
            this.feeUnitGrossValue = tmpChangedValue;
            this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
            this.feeValue = tmpChangedValue * this.getBillItem().getQty();
            return;
        }

        if (foriegn) {
            this.feeUnitGrossValue = getFee().getFfee();
            this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
            this.feeValue = getFee().getFfee() * this.getBillItem().getQty();
        } else {
            this.feeUnitGrossValue = getFee().getFee();
            this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
            this.feeValue = getFee().getFee() * this.getBillItem().getQty();

        }

    }

    @Transient
    private Double tmpChangedValue;
    @Transient
    private Double tmpSettleChangedValue;

    public void setFeeValueForCreditCompany(boolean foriegn, double discountPercent) {
        System.out.println("setFeeValueForCreditCompany");
        if (tmpChangedValue == null) {
            if (getFee().getFeeType() != FeeType.Staff) {
                if (foriegn) {
                    this.feeUnitGrossValue = getFee().getFfee();
                    this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                    this.feeDiscount = (getFee().getFfee() / 100 * (discountPercent)) * this.getBillItem().getQty();
                    this.feeValue = feeGrossValue - feeDiscount;
                } else {
                    this.feeUnitGrossValue = getFee().getFee();
                    this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                    this.feeDiscount = (getFee().getFee() / 100 * (discountPercent)) * this.getBillItem().getQty();
                    this.feeValue = feeGrossValue - feeDiscount;
                }

            } else {
                if (foriegn) {
                    this.feeUnitGrossValue = getFee().getFfee();
                    this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                    this.feeValue = getFee().getFfee() * this.getBillItem().getQty();
                } else {
                    this.feeUnitGrossValue = getFee().getFee();
                    this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                    this.feeValue = getFee().getFee() * this.getBillItem().getQty();
                }
            }
        } else {
            this.feeUnitGrossValue = tmpChangedValue;
            if (getFee().getFeeType() != FeeType.Staff) {
                this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
                if (tmpChangedValue != 0) {
                    this.feeDiscount = feeGrossValue / 100 * (discountPercent);
                    this.feeValue = feeGrossValue - feeDiscount;
                } else {
                    this.feeValue = 0;
                }
            } else {
                this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
                this.feeValue = tmpChangedValue * this.getBillItem().getQty();
            }
        }
    }

    public void setFeeValueForeignAndDiscount(boolean foriegn, double discountPercent) {
        System.out.println("setFeeValueForeignAndDiscount");
        if (tmpChangedValue == null) {
            if (getFee().getFeeType() != FeeType.Staff) {
                if (foriegn) {
                    this.feeUnitGrossValue = getFee().getFfee();
                    this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                } else {
                    this.feeUnitGrossValue = getFee().getFee();
                    this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                }

                // SETTING DISCOUNT
                this.feeDiscount = this.feeGrossValue * (discountPercent / 100);
                this.feeValue = this.feeGrossValue - this.feeDiscount;

            } else {
                if (foriegn) {
                    this.feeUnitGrossValue = getFee().getFfee();
                    this.feeGrossValue = getFee().getFfee() * this.getBillItem().getQty();
                    this.feeValue = getFee().getFfee() * this.getBillItem().getQty();
                } else {
                    this.feeUnitGrossValue = getFee().getFee();
                    this.feeGrossValue = getFee().getFee() * this.getBillItem().getQty();
                    this.feeValue = getFee().getFee() * this.getBillItem().getQty();
                }
            }
        } else {
            this.feeUnitGrossValue = tmpChangedValue;
            if (getFee().getFeeType() != FeeType.Staff) {
                this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
                if (tmpChangedValue != 0) {
                    this.feeDiscount = this.feeGrossValue * (discountPercent / 100);
                    this.feeValue = this.feeGrossValue - this.feeDiscount;
                } else {
                    this.feeValue = 0;
                }
            } else {
                this.feeGrossValue = tmpChangedValue * this.getBillItem().getQty();
                this.feeValue = tmpChangedValue * this.getBillItem().getQty();
            }
        }
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

    public double getDisplayRate() {
        if (tmpChangedValue != null) {
            return tmpChangedValue;
        }
        return fee != null ? fee.getFee() : 0.0;
    }

    public void setDisplayRate(double displayRate) {
        this.tmpChangedValue = displayRate;
    }

    public Double getTmpSettleChangedValue() {
        return tmpSettleChangedValue;
    }

    public void setTmpSettleChangedValue(Double tmpSettleChangedValue) {
        this.tmpSettleChangedValue = tmpSettleChangedValue;
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

    public Double getFeeUnitGrossValue() {
        return feeUnitGrossValue;
    }

    public void setFeeUnitGrossValue(Double feeUnitGrossValue) {
        this.feeUnitGrossValue = feeUnitGrossValue;
    }

    public Double getFeeUnitValue() {
        return feeUnitValue;
    }

    public void setFeeUnitValue(Double feeUnitValue) {
        this.feeUnitValue = feeUnitValue;
    }

    public Double getFeeUnitMargin() {
        return feeUnitMargin;
    }

    public void setFeeUnitMargin(Double feeUnitMargin) {
        this.feeUnitMargin = feeUnitMargin;
    }

    public Double getFeeUnitDiscount() {
        return feeUnitDiscount;
    }

    public void setFeeUnitDiscount(Double feeUnitDiscount) {
        this.feeUnitDiscount = feeUnitDiscount;
    }

    public double getSettleValue() {
        return settleValue;
    }

    public void setSettleValue(double settleValue) {
        this.settleValue = settleValue;
    }

    public double getFeeVat() {
        return feeVat;
    }

    public void setFeeVat(double feeVat) {
        this.feeVat = feeVat;
    }

    public double getAbsoluteFeeValue() {
        absoluteFeeValue = Math.abs(feeValue);
        return absoluteFeeValue;
    }

    public Boolean getFullySettled() {
        if (fullySettled == null) {
            fullySettled = (feeValue - settleValue) < 1;
        }
        return fullySettled;
    }

    public void setFullySettled(Boolean fullySettled) {
        this.fullySettled = fullySettled;
    }

    public Boolean getCompletedPayment() {
        if (completedPayment == null) {
            double absolutePaidValue = Math.abs(paidValue);
            double absoluteFeeValue = Math.abs(feeValue);
            double difference = Math.abs(absolutePaidValue - absoluteFeeValue);
            completedPayment = difference < 1;
        }
        return completedPayment;
    }

    public void setCompletedPayment(Boolean completedPayment) {
        this.completedPayment = completedPayment;
    }

    public boolean isReturned() {
        return returned;
    }

    public void setReturned(boolean returned) {
        this.returned = returned;
    }

    public String getUuid() {
        return uuid;
    }

    public Double getFeeGrossValueTransient() {
        return feeGrossValueTransient;
    }

    public void setFeeGrossValueTransient(Double feeGrossValueTransient) {
        this.feeGrossValueTransient = feeGrossValueTransient;
    }

    public boolean isUserChangedTheGrossValueTransient() {
        return userChangedTheGrossValueTransient;
    }

    public void setUserChangedTheGrossValueTransient(boolean userChangedTheGrossValueTransient) {
        this.userChangedTheGrossValueTransient = userChangedTheGrossValueTransient;
    }

    public boolean isFeeCollectedByDoctor() {
        return feeCollectedByDoctor;
    }

    public void setFeeCollectedByDoctor(boolean feeCollectedByDoctor) {
        this.feeCollectedByDoctor = feeCollectedByDoctor;
    }

    public Boolean getFeePaymentOnHold() {
        return feePaymentOnHold;
    }

    public boolean isFeePaymentOnHold() {
        return Boolean.TRUE.equals(feePaymentOnHold);
    }

    public void setFeePaymentOnHold(Boolean feePaymentOnHold) {
        this.feePaymentOnHold = feePaymentOnHold;
    }

    public Date getFeePaymentHoldDateTime() {
        return feePaymentHoldDateTime;
    }

    public void setFeePaymentHoldDateTime(Date feePaymentHoldDateTime) {
        this.feePaymentHoldDateTime = feePaymentHoldDateTime;
    }

    public WebUser getFeePaymentHoldBy() {
        return feePaymentHoldBy;
    }

    public void setFeePaymentHoldBy(WebUser feePaymentHoldBy) {
        this.feePaymentHoldBy = feePaymentHoldBy;
    }

    public String getFeePaymentHoldNotes() {
        return feePaymentHoldNotes;
    }

    public void setFeePaymentHoldNotes(String feePaymentHoldNotes) {
        this.feePaymentHoldNotes = feePaymentHoldNotes;
    }

}
