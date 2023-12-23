/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.clinical;

import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.FrequencyUnit;
import com.divudi.entity.pharmacy.MeasurementUnit;
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
        return "com.divudi.entity.clinical.FavouriteItem[ id=" + id + " ]";
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

}
