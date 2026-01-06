/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.core.entity.clinical;

import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.MeasurementUnit;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity
public class Prescription implements Serializable {

    @ManyToOne
    private Prescription parent;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Department department;
    @ManyToOne
    private WebUser webUser;
    @ManyToOne
    private Item item;

    @ManyToOne
    Patient patient;
    @ManyToOne
    PatientEncounter encounter;

    @ManyToOne
    private Category category;

    @ManyToOne
    private MeasurementUnit doseUnit;
    private Double dose;

    @ManyToOne
    private MeasurementUnit frequencyUnit;

    private Double orderNo;

    @ManyToOne
    private MeasurementUnit durationUnit;

    private Double duration;
    
    // Prescription Period
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date prescribedFrom;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date prescribedTo;

    @ManyToOne
    private MeasurementUnit issueUnit;
    private Double issue;

    @OneToMany(mappedBy = "parent")
    private List<Prescription> children;

    private boolean indoor;

    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;
    //Editer Properties
    @ManyToOne
    private WebUser editer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;
    private String comment;

    // Prescription Lifecycle Management
    // 1. Prescription Phase
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date prescribedAt;
    @ManyToOne
    private WebUser prescribedBy;
    @ManyToOne
    private Department prescribingDepartment;
    
    // 2. Order Verification Phase
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date verifiedAt;
    @ManyToOne
    private WebUser verifiedBy;
    @ManyToOne
    private Department verifyingDepartment;
    
    // 3. Dispensing Phase
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dispensedAt;
    @ManyToOne
    private WebUser dispensedBy;
    @ManyToOne
    private Department dispensingDepartment;
    private Double dispensedQuantity;
    private String dispensingNotes;
    
    // 4. Administration Phase
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date administrationStartedAt;
    @ManyToOne
    private WebUser administrationStartedBy;
    @ManyToOne
    private Department administrationStartedDepartment;
    
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date administrationStoppedAt;
    @ManyToOne
    private WebUser administrationStoppedBy;
    @ManyToOne
    private Department administrationStoppedDepartment;
    private String administrationStopReason;
    
    // 5. Omission Management
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date omittedAt;
    @ManyToOne
    private WebUser omittedBy;
    @ManyToOne
    private Department omittingDepartment;
    private String omissionReason;
    
    // 6. Review and Monitoring
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date lastReviewedAt;
    @ManyToOne
    private WebUser lastReviewedBy;
    @ManyToOne
    private Department reviewingDepartment;
    
    // 7. Additional Clinical Fields for Inpatient Management
    private String route; // Administration route (oral, IV, IM, etc.)
    private String priority; // STAT, URGENT, ROUTINE
    private boolean prn; // Pro re nata (as needed)
    private String prnIndication; // When to give PRN medication
    private Integer totalDoses; // Total number of doses prescribed
    private Integer dosesGiven; // Number of doses actually administered
    private Integer dosesRemaining; // Remaining doses
    
    // 8. Safety and Monitoring
    private String allergies; // Patient allergies related to this prescription
    private String contraindications; // Any contraindications
    private String interactions; // Drug interactions to monitor
    private String sideEffectsToMonitor; // Side effects to watch for
    
    // 9. Discharge Planning
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dischargeDate;
    private boolean continueOnDischarge;
    private String dischargeInstructions;
    @ManyToOne
    private WebUser dischargeReviewedBy;
    
    // 10. Clinical Decision Support
    private String clinicalIndication; // Why this medication was prescribed
    private String therapeuticGoal; // What we hope to achieve
    private String monitoringPlan; // How to monitor effectiveness/safety
    
    // 11. Workflow Status
    private String prescriptionStatus; // ACTIVE, HELD, DISCONTINUED, COMPLETED
    private String workflowStage; // PRESCRIBED, VERIFIED, DISPENSED, ADMINISTERED
    private boolean requiresPharmacistReview;
    private boolean requiresNurseAdministration;

    public String getFormattedPrescription() {
        DecimalFormat df = new DecimalFormat("#.##"); // Formats the number to avoid unnecessary decimal places

        StringBuilder prescriptionText = new StringBuilder();

        if (item != null) {
            prescriptionText.append(item.getName()); // Append medicine name
        }
        // Append dose with formatted value
        if (dose != null) {
            String formattedDose = dose % 1 == 0 ? String.valueOf(dose.intValue()) : df.format(dose);
            prescriptionText.append(" ").append(formattedDose);
        }

        // Append dose unit
        if (doseUnit != null) {
            prescriptionText.append(" ").append(doseUnit.getName());
        }

        // Append frequency
        if (frequencyUnit != null) {
            prescriptionText.append(" ").append(frequencyUnit.getName());
        }

        // Append duration
        if (duration != null) {
            String formattedDuration = duration % 1 == 0 ? String.valueOf(duration.intValue()) : df.format(duration);
            prescriptionText.append(" for ").append(formattedDuration);
        }

        // Append duration unit
        if (durationUnit != null ) {
            prescriptionText.append(" ").append(durationUnit.getName());
        }

        if(indoor){
             prescriptionText.append(" (indoor)");
        }else{
             prescriptionText.append(" (outdoor)");
        }

        return prescriptionText.toString();
    }

    public String getFormattedPrescriptionWithoutIndoorOutdoor() {
        DecimalFormat df = new DecimalFormat("#.##"); // Formats the number to avoid unnecessary decimal places

        StringBuilder prescriptionText = new StringBuilder();

        if (item != null) {
            prescriptionText.append(item.getName()); // Append medicine name
        }
        // Append dose with formatted value
        if (dose != null) {
            String formattedDose = dose % 1 == 0 ? String.valueOf(dose.intValue()) : df.format(dose);
            prescriptionText.append(" ").append(formattedDose);
        }

        // Append dose unit
        if (doseUnit != null) {
            prescriptionText.append(" ").append(doseUnit.getName());
        }

        // Append frequency
        if (frequencyUnit != null) {
            prescriptionText.append(" ").append(frequencyUnit.getName());
        }

        // Append duration
        if (duration != null) {
            String formattedDuration = duration % 1 == 0 ? String.valueOf(duration.intValue()) : df.format(duration);
            prescriptionText.append(" for ").append(formattedDuration);
        }

        // Append duration unit
        if (durationUnit != null ) {
            prescriptionText.append(" ").append(durationUnit.getName());
        }

        return prescriptionText.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getEncounter() {
        return encounter;
    }

    public void setEncounter(PatientEncounter encounter) {
        this.encounter = encounter;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Prescription)) {
            return false;
        }
        Prescription other = (Prescription) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.clinical.Prescription[ id=" + id + " ]";
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public MeasurementUnit getDoseUnit() {
        return doseUnit;
    }

    public void setDoseUnit(MeasurementUnit dosageUnit) {
        this.doseUnit = dosageUnit;
    }

    public Double getDose() {
        return dose;
    }

    public void setDose(Double dose) {
        this.dose = dose;
    }

    public MeasurementUnit getFrequencyUnit() {
        return frequencyUnit;
    }

    public void setFrequencyUnit(MeasurementUnit frequencyUnit) {
        this.frequencyUnit = frequencyUnit;
    }

    public Double getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(Double orderNo) {
        this.orderNo = orderNo;
    }

    public List<Prescription> getChildren() {
        return children;
    }

    public void setChildren(List<Prescription> children) {
        this.children = children;
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

    public WebUser getEditer() {
        return editer;
    }

    public void setEditer(WebUser editer) {
        this.editer = editer;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public Prescription getParent() {
        return parent;
    }

    public void setParent(Prescription parent) {
        this.parent = parent;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public MeasurementUnit getDurationUnit() {
        return durationUnit;
    }

    public void setDurationUnit(MeasurementUnit durationUnit) {
        this.durationUnit = durationUnit;
    }

    public Double getDuration() {
        return duration;
    }

    public void setDuration(Double duration) {
        this.duration = duration;
    }

    public Date getPrescribedFrom() {
        return prescribedFrom;
    }

    public void setPrescribedFrom(Date prescribedFrom) {
        this.prescribedFrom = prescribedFrom;
    }

    public Date getPrescribedTo() {
        return prescribedTo;
    }

    public void setPrescribedTo(Date prescribedTo) {
        this.prescribedTo = prescribedTo;
    }

    public MeasurementUnit getIssueUnit() {
        return issueUnit;
    }

    public void setIssueUnit(MeasurementUnit issueUnit) {
        this.issueUnit = issueUnit;
    }

    public Double getIssue() {
        return issue;
    }

    public void setIssue(Double issue) {
        this.issue = issue;
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

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public boolean isIndoor() {
        return indoor;
    }

    public void setIndoor(boolean indoor) {
        this.indoor = indoor;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // Getter and Setter methods for Prescription Lifecycle Management
    
    // 1. Prescription Phase
    public Date getPrescribedAt() {
        return prescribedAt;
    }

    public void setPrescribedAt(Date prescribedAt) {
        this.prescribedAt = prescribedAt;
    }

    public WebUser getPrescribedBy() {
        return prescribedBy;
    }

    public void setPrescribedBy(WebUser prescribedBy) {
        this.prescribedBy = prescribedBy;
    }

    public Department getPrescribingDepartment() {
        return prescribingDepartment;
    }

    public void setPrescribingDepartment(Department prescribingDepartment) {
        this.prescribingDepartment = prescribingDepartment;
    }

    // 2. Order Verification Phase
    public Date getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Date verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public WebUser getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(WebUser verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Department getVerifyingDepartment() {
        return verifyingDepartment;
    }

    public void setVerifyingDepartment(Department verifyingDepartment) {
        this.verifyingDepartment = verifyingDepartment;
    }

    // 3. Dispensing Phase
    public Date getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(Date dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public WebUser getDispensedBy() {
        return dispensedBy;
    }

    public void setDispensedBy(WebUser dispensedBy) {
        this.dispensedBy = dispensedBy;
    }

    public Department getDispensingDepartment() {
        return dispensingDepartment;
    }

    public void setDispensingDepartment(Department dispensingDepartment) {
        this.dispensingDepartment = dispensingDepartment;
    }

    public Double getDispensedQuantity() {
        return dispensedQuantity;
    }

    public void setDispensedQuantity(Double dispensedQuantity) {
        this.dispensedQuantity = dispensedQuantity;
    }

    public String getDispensingNotes() {
        return dispensingNotes;
    }

    public void setDispensingNotes(String dispensingNotes) {
        this.dispensingNotes = dispensingNotes;
    }

    // 4. Administration Phase
    public Date getAdministrationStartedAt() {
        return administrationStartedAt;
    }

    public void setAdministrationStartedAt(Date administrationStartedAt) {
        this.administrationStartedAt = administrationStartedAt;
    }

    public WebUser getAdministrationStartedBy() {
        return administrationStartedBy;
    }

    public void setAdministrationStartedBy(WebUser administrationStartedBy) {
        this.administrationStartedBy = administrationStartedBy;
    }

    public Department getAdministrationStartedDepartment() {
        return administrationStartedDepartment;
    }

    public void setAdministrationStartedDepartment(Department administrationStartedDepartment) {
        this.administrationStartedDepartment = administrationStartedDepartment;
    }

    public Date getAdministrationStoppedAt() {
        return administrationStoppedAt;
    }

    public void setAdministrationStoppedAt(Date administrationStoppedAt) {
        this.administrationStoppedAt = administrationStoppedAt;
    }

    public WebUser getAdministrationStoppedBy() {
        return administrationStoppedBy;
    }

    public void setAdministrationStoppedBy(WebUser administrationStoppedBy) {
        this.administrationStoppedBy = administrationStoppedBy;
    }

    public Department getAdministrationStoppedDepartment() {
        return administrationStoppedDepartment;
    }

    public void setAdministrationStoppedDepartment(Department administrationStoppedDepartment) {
        this.administrationStoppedDepartment = administrationStoppedDepartment;
    }

    public String getAdministrationStopReason() {
        return administrationStopReason;
    }

    public void setAdministrationStopReason(String administrationStopReason) {
        this.administrationStopReason = administrationStopReason;
    }

    // 5. Omission Management
    public Date getOmittedAt() {
        return omittedAt;
    }

    public void setOmittedAt(Date omittedAt) {
        this.omittedAt = omittedAt;
    }

    public WebUser getOmittedBy() {
        return omittedBy;
    }

    public void setOmittedBy(WebUser omittedBy) {
        this.omittedBy = omittedBy;
    }

    public Department getOmittingDepartment() {
        return omittingDepartment;
    }

    public void setOmittingDepartment(Department omittingDepartment) {
        this.omittingDepartment = omittingDepartment;
    }

    public String getOmissionReason() {
        return omissionReason;
    }

    public void setOmissionReason(String omissionReason) {
        this.omissionReason = omissionReason;
    }

    // 6. Review and Monitoring
    public Date getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Date lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public WebUser getLastReviewedBy() {
        return lastReviewedBy;
    }

    public void setLastReviewedBy(WebUser lastReviewedBy) {
        this.lastReviewedBy = lastReviewedBy;
    }

    public Department getReviewingDepartment() {
        return reviewingDepartment;
    }

    public void setReviewingDepartment(Department reviewingDepartment) {
        this.reviewingDepartment = reviewingDepartment;
    }

    // 7. Additional Clinical Fields
    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isPrn() {
        return prn;
    }

    public void setPrn(boolean prn) {
        this.prn = prn;
    }

    public String getPrnIndication() {
        return prnIndication;
    }

    public void setPrnIndication(String prnIndication) {
        this.prnIndication = prnIndication;
    }

    public Integer getTotalDoses() {
        return totalDoses;
    }

    public void setTotalDoses(Integer totalDoses) {
        this.totalDoses = totalDoses;
    }

    public Integer getDosesGiven() {
        return dosesGiven;
    }

    public void setDosesGiven(Integer dosesGiven) {
        this.dosesGiven = dosesGiven;
    }

    public Integer getDosesRemaining() {
        return dosesRemaining;
    }

    public void setDosesRemaining(Integer dosesRemaining) {
        this.dosesRemaining = dosesRemaining;
    }

    // 8. Safety and Monitoring
    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getContraindications() {
        return contraindications;
    }

    public void setContraindications(String contraindications) {
        this.contraindications = contraindications;
    }

    public String getInteractions() {
        return interactions;
    }

    public void setInteractions(String interactions) {
        this.interactions = interactions;
    }

    public String getSideEffectsToMonitor() {
        return sideEffectsToMonitor;
    }

    public void setSideEffectsToMonitor(String sideEffectsToMonitor) {
        this.sideEffectsToMonitor = sideEffectsToMonitor;
    }

    // 9. Discharge Planning
    public Date getDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(Date dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public boolean isContinueOnDischarge() {
        return continueOnDischarge;
    }

    public void setContinueOnDischarge(boolean continueOnDischarge) {
        this.continueOnDischarge = continueOnDischarge;
    }

    public String getDischargeInstructions() {
        return dischargeInstructions;
    }

    public void setDischargeInstructions(String dischargeInstructions) {
        this.dischargeInstructions = dischargeInstructions;
    }

    public WebUser getDischargeReviewedBy() {
        return dischargeReviewedBy;
    }

    public void setDischargeReviewedBy(WebUser dischargeReviewedBy) {
        this.dischargeReviewedBy = dischargeReviewedBy;
    }

    // 10. Clinical Decision Support
    public String getClinicalIndication() {
        return clinicalIndication;
    }

    public void setClinicalIndication(String clinicalIndication) {
        this.clinicalIndication = clinicalIndication;
    }

    public String getTherapeuticGoal() {
        return therapeuticGoal;
    }

    public void setTherapeuticGoal(String therapeuticGoal) {
        this.therapeuticGoal = therapeuticGoal;
    }

    public String getMonitoringPlan() {
        return monitoringPlan;
    }

    public void setMonitoringPlan(String monitoringPlan) {
        this.monitoringPlan = monitoringPlan;
    }

    // 11. Workflow Status
    public String getPrescriptionStatus() {
        return prescriptionStatus;
    }

    public void setPrescriptionStatus(String prescriptionStatus) {
        this.prescriptionStatus = prescriptionStatus;
    }

    public String getWorkflowStage() {
        return workflowStage;
    }

    public void setWorkflowStage(String workflowStage) {
        this.workflowStage = workflowStage;
    }

    public boolean isRequiresPharmacistReview() {
        return requiresPharmacistReview;
    }

    public void setRequiresPharmacistReview(boolean requiresPharmacistReview) {
        this.requiresPharmacistReview = requiresPharmacistReview;
    }

    public boolean isRequiresNurseAdministration() {
        return requiresNurseAdministration;
    }

    public void setRequiresNurseAdministration(boolean requiresNurseAdministration) {
        this.requiresNurseAdministration = requiresNurseAdministration;
    }

    /**
     * Creates a clone of this prescription entity with all attributes copied except id and billItem.
     * The cloned prescription will have a null id, making it ready for persistence as a new entity.
     *
     * @return A new Prescription instance with copied attributes (excluding id and billItem)
     */
    public Prescription cloneForNewEntity() {
        Prescription cloned = new Prescription();

        // Note: id is intentionally NOT copied (remains null for new entity)
        // Note: billItem is intentionally NOT copied (as specified in requirements)

        // Basic entity relationships
        cloned.setInstitution(this.institution);
        cloned.setDepartment(this.department);
        cloned.setWebUser(this.webUser);
        cloned.setItem(this.item);
        cloned.setPatient(this.patient);
        cloned.setEncounter(this.encounter);
        cloned.setCategory(this.category);

        // Dosage information
        cloned.setDoseUnit(this.doseUnit);
        cloned.setDose(this.dose);
        cloned.setFrequencyUnit(this.frequencyUnit);
        cloned.setOrderNo(this.orderNo);
        cloned.setDurationUnit(this.durationUnit);
        cloned.setDuration(this.duration);

        // Prescription period
        cloned.setPrescribedFrom(this.prescribedFrom);
        cloned.setPrescribedTo(this.prescribedTo);

        // Issue information
        cloned.setIssueUnit(this.issueUnit);
        cloned.setIssue(this.issue);

        // Parent relationship
        cloned.setParent(this.parent);
        // Note: children collection is NOT copied as it should be empty for new entity

        cloned.setIndoor(this.indoor);
        cloned.setComment(this.comment);

        // Audit trail fields - these will typically be set by the system when saving
        // We copy them but they may be overwritten during persistence
        cloned.setCreater(this.creater);
        cloned.setCreatedAt(this.createdAt);
        cloned.setRetired(this.retired);
        cloned.setRetirer(this.retirer);
        cloned.setRetiredAt(this.retiredAt);
        cloned.setRetireComments(this.retireComments);
        cloned.setEditer(this.editer);
        cloned.setEditedAt(this.editedAt);

        // Prescription lifecycle management
        cloned.setPrescribedAt(this.prescribedAt);
        cloned.setPrescribedBy(this.prescribedBy);
        cloned.setPrescribingDepartment(this.prescribingDepartment);

        cloned.setVerifiedAt(this.verifiedAt);
        cloned.setVerifiedBy(this.verifiedBy);
        cloned.setVerifyingDepartment(this.verifyingDepartment);

        cloned.setDispensedAt(this.dispensedAt);
        cloned.setDispensedBy(this.dispensedBy);
        cloned.setDispensingDepartment(this.dispensingDepartment);
        cloned.setDispensedQuantity(this.dispensedQuantity);
        cloned.setDispensingNotes(this.dispensingNotes);

        cloned.setAdministrationStartedAt(this.administrationStartedAt);
        cloned.setAdministrationStartedBy(this.administrationStartedBy);
        cloned.setAdministrationStartedDepartment(this.administrationStartedDepartment);
        cloned.setAdministrationStoppedAt(this.administrationStoppedAt);
        cloned.setAdministrationStoppedBy(this.administrationStoppedBy);
        cloned.setAdministrationStoppedDepartment(this.administrationStoppedDepartment);
        cloned.setAdministrationStopReason(this.administrationStopReason);

        cloned.setOmittedAt(this.omittedAt);
        cloned.setOmittedBy(this.omittedBy);
        cloned.setOmittingDepartment(this.omittingDepartment);
        cloned.setOmissionReason(this.omissionReason);

        cloned.setLastReviewedAt(this.lastReviewedAt);
        cloned.setLastReviewedBy(this.lastReviewedBy);
        cloned.setReviewingDepartment(this.reviewingDepartment);

        // Additional clinical fields
        cloned.setRoute(this.route);
        cloned.setPriority(this.priority);
        cloned.setPrn(this.prn);
        cloned.setPrnIndication(this.prnIndication);
        cloned.setTotalDoses(this.totalDoses);
        cloned.setDosesGiven(this.dosesGiven);
        cloned.setDosesRemaining(this.dosesRemaining);

        // Safety and monitoring
        cloned.setAllergies(this.allergies);
        cloned.setContraindications(this.contraindications);
        cloned.setInteractions(this.interactions);
        cloned.setSideEffectsToMonitor(this.sideEffectsToMonitor);

        // Discharge planning
        cloned.setDischargeDate(this.dischargeDate);
        cloned.setContinueOnDischarge(this.continueOnDischarge);
        cloned.setDischargeInstructions(this.dischargeInstructions);
        cloned.setDischargeReviewedBy(this.dischargeReviewedBy);

        // Clinical decision support
        cloned.setClinicalIndication(this.clinicalIndication);
        cloned.setTherapeuticGoal(this.therapeuticGoal);
        cloned.setMonitoringPlan(this.monitoringPlan);

        // Workflow status
        cloned.setPrescriptionStatus(this.prescriptionStatus);
        cloned.setWorkflowStage(this.workflowStage);
        cloned.setRequiresPharmacistReview(this.requiresPharmacistReview);
        cloned.setRequiresNurseAdministration(this.requiresNurseAdministration);

        return cloned;
    }

}
