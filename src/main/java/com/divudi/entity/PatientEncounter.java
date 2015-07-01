/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.PaymentMethod;
import com.divudi.data.SymanticType;
import com.divudi.data.inward.PatientEncounterType;
import com.divudi.entity.clinical.ClinicalFindingValue;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.EncounterComponent;
import com.divudi.entity.inward.PatientRoom;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author buddhika
 */
@Entity
public class PatientEncounter implements Serializable {
//    @OneToMany(mappedBy = "patientEncounter",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
//     List<PatientRoom> patientRooms;

    @OneToMany(mappedBy = "patientEncounter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<EncounterComponent> encounterComponents;

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    //Main Properties   
    Long id;
    String bhtNo;
    long bhtLong;
    @ManyToOne
    Patient patient;
    @ManyToOne
    Person guardian;
    @ManyToOne
    private PatientRoom currentPatientRoom;
    @ManyToOne
    private Item item;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date fromTime;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date toTime;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    @Enumerated(EnumType.STRING)
    PaymentMethod paymentMethod;
    @ManyToOne
    Institution creditCompany;
    @ManyToOne
    private Staff referringDoctor;
    @ManyToOne
    private Staff opdDoctor;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date dateOfAdmission;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date timeOfAdmission;
    @ManyToOne
    AdmissionType admissionType;
    Boolean discharged = false;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date timeOfDischarge;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date dateOfDischarge;
    double creditLimit;
    double creditUsedAmount;
    private double creditPaidAmount;
    @Enumerated(EnumType.STRING)
    PatientEncounterType patientEncounterType;
    @OneToMany(mappedBy = "parentEncounter")
    List<PatientEncounter> childEncounters;
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<ClinicalFindingValue> clinicalFindingValues;
    String name;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date dateTime;
    @ManyToOne
    PatientEncounter parentEncounter;
    @ManyToOne
    BillSession billSession;
    private boolean paymentFinalized;
    String referanceNo;
    String policyNo;
    boolean foriegner;
    boolean claimable;
    double grantTotal;
    double discount;
    double netTotal;
    double adjustedTotal;
    @Transient
    Date timePeriod;
    @Transient
    double transTotal;
    @Transient
    double transPaid;
    @Transient
    double transPaidByCompany;
    @Transient
    double transPaidByPatient;
    @Transient
    long transDayCount;
    @ManyToOne
    Bill finalBill;
    
    @ManyToOne
    Institution referredByInstitution;
    String referralId;

    public double getTransPaidByCompany() {
        return transPaidByCompany;
    }

    public void setTransPaidByCompany(double transPaidByCompany) {
        this.transPaidByCompany = transPaidByCompany;
    }

    public double getTransPaidByPatient() {
        return transPaidByPatient;
    }

    public void setTransPaidByPatient(double transPaidByPatient) {
        this.transPaidByPatient = transPaidByPatient;
    }
    
    

    public double getTransTotal() {
        return transTotal;
    }

    public void setTransTotal(double transTotal) {
        this.transTotal = transTotal;
    }

    public double getTransPaid() {
        return transPaid;
    }

    public void setTransPaid(double transPaid) {
        this.transPaid = transPaid;
    }

    public boolean getRetired() {
        return retired;
    }

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getAdjustedTotal() {
        return adjustedTotal;
    }

    public void setAdjustedTotal(double adjustedTotal) {
        this.adjustedTotal = adjustedTotal;
    }

    public Date getTimePeriod() {
        Calendar calFrm = Calendar.getInstance();
        Calendar calTo = Calendar.getInstance();
        calFrm.setTime(fromTime);
        calTo.setTime(toTime);

        Long lg = calFrm.getTimeInMillis() - calTo.getTimeInMillis();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(lg);

        timePeriod = cal.getTime();

        return timePeriod;
    }

    public void setTimePeriod(Date timePeriod) {
        this.timePeriod = timePeriod;
    }

    public boolean isForiegner() {
        return foriegner;
    }

    public void setForiegner(boolean foriegner) {
        this.foriegner = foriegner;
    }

    public boolean isClaimable() {
        return claimable;
    }

    public void setClaimable(boolean claimable) {
        this.claimable = claimable;
    }
    @Lob
    String comments;
    @Transient
    List<ClinicalFindingValue> diagnosis;
    @ManyToOne
    Department department;

    @Transient
    List<ClinicalFindingValue> investigations;

    @Transient
    List<ClinicalFindingValue> symptoms;

    @Transient
    List<ClinicalFindingValue> signs;

    @Transient
    List<ClinicalFindingValue> procedures;

    @Transient
    List<ClinicalFindingValue> plans;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date printingAdmissionTime;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date printingDischargeTime;

    public List<ClinicalFindingValue> getDiagnosis() {
        if (diagnosis == null) {
            diagnosis = new ArrayList<>();
            for (ClinicalFindingValue v : clinicalFindingValues) {
                if (v.getClinicalFindingItem().getSymanticType() == SymanticType.Disease_or_Syndrome) {
                    diagnosis.add(v);
                }
            }
        }
        return diagnosis;
    }

    public void setDiagnosis(List<ClinicalFindingValue> diagnosis) {
        this.diagnosis = diagnosis;
    }

    public List<ClinicalFindingValue> getInvestigations() {
        if (investigations == null) {
            investigations = new ArrayList<>();
            for (ClinicalFindingValue v : clinicalFindingValues) {
                if (v.getClinicalFindingItem().getSymanticType() == SymanticType.Laboratory_Procedure) {
                    investigations.add(v);
                }
            }

        }
        return investigations;
    }

    public void setInvestigations(List<ClinicalFindingValue> investigations) {
        this.investigations = investigations;
    }

    public List<ClinicalFindingValue> getSymptoms() {
        if (symptoms == null) {
            symptoms = new ArrayList<>();
            for (ClinicalFindingValue v : clinicalFindingValues) {
                if (v.getClinicalFindingItem().getSymanticType() == SymanticType.Symptom) {
                    symptoms.add(v);
                }
            }

        }
        return symptoms;
    }

    public void setSymptoms(List<ClinicalFindingValue> symptoms) {
        this.symptoms = symptoms;
    }

    public List<ClinicalFindingValue> getSigns() {
        if (signs == null) {
            signs = new ArrayList<>();
            for (ClinicalFindingValue v : clinicalFindingValues) {
                if (v.getClinicalFindingItem().getSymanticType() == SymanticType.Sign) {
                    signs.add(v);
                }
            }
        }
        return signs;
    }

    public void setSigns(List<ClinicalFindingValue> signs) {
        this.signs = signs;
    }

    public List<ClinicalFindingValue> getProcedures() {
        if (procedures == null) {
            procedures = new ArrayList<>();
            for (ClinicalFindingValue v : clinicalFindingValues) {
                if (v.getClinicalFindingItem().getSymanticType() == SymanticType.Therapeutic_Procedure) {
                    procedures.add(v);
                }
            }
        }
        return procedures;
    }

    public void setProcedures(List<ClinicalFindingValue> procedures) {
        this.procedures = procedures;
    }

    public List<ClinicalFindingValue> getPlans() {
        if (plans == null) {
            plans = new ArrayList<>();
            for (ClinicalFindingValue v : clinicalFindingValues) {
                if (v.getClinicalFindingItem().getSymanticType() == SymanticType.Preventive_Procedure) {
                    plans.add(v);
                }
            }
        }
        return plans;
    }

    public void setPlans(List<ClinicalFindingValue> plans) {
        this.plans = plans;
    }

    public BillSession getBillSession() {
        return billSession;
    }

    public void setBillSession(BillSession billSession) {
        this.billSession = billSession;
    }

    public List<PatientEncounter> getChildEncounters() {
        return childEncounters;
    }

    public void setChildEncounters(List<PatientEncounter> childEncounters) {
        this.childEncounters = childEncounters;
    }

    public PatientEncounter getParentEncounter() {
        return parentEncounter;
    }

    public void setParentEncounter(PatientEncounter parentEncounter) {
        this.parentEncounter = parentEncounter;
    }

    public List<ClinicalFindingValue> getClinicalFindingValues() {
        return clinicalFindingValues;
    }

    public void setClinicalFindingValues(List<ClinicalFindingValue> clinicalFindingValues) {
        this.clinicalFindingValues = clinicalFindingValues;
    }

    public PatientEncounterType getPatientEncounterType() {
        return patientEncounterType;
    }

    public void setPatientEncounterType(PatientEncounterType patientEncounterType) {
        this.patientEncounterType = patientEncounterType;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

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

        if (!(object instanceof PatientEncounter)) {
            return false;
        }
        PatientEncounter other = (PatientEncounter) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.PatientEncounter[ id=" + id + " ]";
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

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public long getBhtLong() {
        return bhtLong;
    }

    public void setBhtLong(long bhtLong) {
        this.bhtLong = bhtLong;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public Person getGuardian() {
        if (guardian == null) {
            guardian = new Person();
        }
        return guardian;
    }

    public void setGuardian(Person guardian) {
        this.guardian = guardian;
    }

    public Date getDateOfAdmission() {
        return dateOfAdmission;
    }

    public void setDateOfAdmission(Date dateOfAdmission) {
        this.dateOfAdmission = dateOfAdmission;
        printingAdmissionTime = dateOfAdmission;
    }

    public Date getTimeOfAdmission() {
        return timeOfAdmission;
    }

    public void setTimeOfAdmission(Date timeOfAdmission) {
        this.timeOfAdmission = timeOfAdmission;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Boolean isDischarged() {
        return discharged;
    }

    public Boolean getDischarged() {
        return discharged;
    }

    public void setDischarged(Boolean discharged) {
        this.discharged = discharged;
    }

    public Date getTimeOfDischarge() {
        return timeOfDischarge;
    }

    public void setTimeOfDischarge(Date timeOfDischarge) {
        this.timeOfDischarge = timeOfDischarge;
    }

    public Date getDateOfDischarge() {
        return dateOfDischarge;
    }

    public void setDateOfDischarge(Date dateOfDischarge) {
        this.dateOfDischarge = dateOfDischarge;
        this.printingDischargeTime = dateOfDischarge;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(double creditLimit) {
        this.creditLimit = creditLimit;
    }

    public double getCreditUsedAmount() {
        return creditUsedAmount;
    }

    public void setCreditUsedAmount(double creditUsedAmount) {
        this.creditUsedAmount = creditUsedAmount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    public List<EncounterComponent> getEncounterComponents() {
        return encounterComponents;
    }

    public void setEncounterComponents(List<EncounterComponent> encounterComponents) {
        this.encounterComponents = encounterComponents;
    }

//    public List<PatientRoom> getPatientRooms() {
//        return patientRooms;
//    }
//
//    public void setPatientRooms(List<PatientRoom> patientRooms) {
//        this.patientRooms = patientRooms;
//    }
//    
//    public PatientRoom getLastPateintRoom(){
//        return getPatientRooms().get(getPatientRooms().size()-1);
//    }
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public boolean isPaymentFinalized() {
        return paymentFinalized;
    }

    public void setPaymentFinalized(boolean paymentFinalized) {
        this.paymentFinalized = paymentFinalized;
    }

    public PatientRoom getCurrentPatientRoom() {
        return currentPatientRoom;
    }

    public void setCurrentPatientRoom(PatientRoom currentPatientRoom) {
        this.currentPatientRoom = currentPatientRoom;
    }

    public String getReferanceNo() {
        return referanceNo;
    }

    public void setReferanceNo(String referanceNo) {
        this.referanceNo = referanceNo;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public Staff getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Staff referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public Staff getOpdDoctor() {
        return opdDoctor;
    }

    public void setOpdDoctor(Staff opdDoctor) {
        this.opdDoctor = opdDoctor;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public double getCreditPaidAmount() {
        return creditPaidAmount;
    }

    public double getPaidByCreditCompany() {
        return 0 - creditPaidAmount;
    }

    public void setCreditPaidAmount(double creditPaidAmount) {
        this.creditPaidAmount = creditPaidAmount;
    }

    public Date getPrintingAdmissionTime() {
        if (printingAdmissionTime == null) {
            printingAdmissionTime = dateOfAdmission;
        }
        return printingAdmissionTime;
    }

    public void setPrintingAdmissionTime(Date printingAdmissionTime) {
        this.printingAdmissionTime = printingAdmissionTime;
    }

    public Date getPrintingDischargeTime() {
        if (printingDischargeTime == null) {
            printingDischargeTime = dateOfDischarge;
        }
        return printingDischargeTime;
    }

    public void setPrintingDischargeTime(Date printingDischargeTime) {
        this.printingDischargeTime = printingDischargeTime;
    }

    public Bill getFinalBill() {
        return finalBill;
    }

    public void setFinalBill(Bill finalBill) {
        this.finalBill = finalBill;
    }

    public Institution getReferredByInstitution() {
        return referredByInstitution;
    }

    public void setReferredByInstitution(Institution referredByInstitution) {
        this.referredByInstitution = referredByInstitution;
    }

    public String getReferralId() {
        return referralId;
    }

    public void setReferralId(String referralId) {
        this.referralId = referralId;
    }

    public long getTransDayCount() {
        return transDayCount;
    }

    public void setTransDayCount(long transDayCount) {
        this.transDayCount = transDayCount;
    }

}
