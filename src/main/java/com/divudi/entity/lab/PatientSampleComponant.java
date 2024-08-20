/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.lab;

import com.divudi.entity.Bill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika_ari
 */
@Entity
public class PatientSampleComponant implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private PatientSample patientSample;
    @ManyToOne
    private PatientInvestigation patientInvestigation;
    @ManyToOne
    private Bill bill;
    //Created Properties
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    //Sample Collection
    private Boolean collected;
    @ManyToOne
    private WebUser sampleCollecter;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date sampledAt;
    private Boolean sampleOutside;
    private String sampleComments;
    @ManyToOne
    private Department sampleDepartment;
    @ManyToOne
    private Institution sampleInstitution;
    //
    @ManyToOne
    private Item investigationComponant;
    @ManyToOne
    private Item test;
    @ManyToOne
    private InvestigationTube tube;
    @ManyToOne
    private Machine machine;
    @ManyToOne
    private Sample sample;
            //Cancellation
    private Boolean cancelled = false;
    @ManyToOne
    private WebUser cancelledUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date cancelledAt;
    private String cancellComments;
    @ManyToOne
    private Department cancellDepartment;
    @ManyToOne
    private Institution cancellInstitution;
    
    //Retairing properties
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdStr() {
        String formatted = String.format("%09d", id);
        return formatted;
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
        if (!(object instanceof PatientSampleComponant)) {
            return false;
        }
        PatientSampleComponant other = (PatientSampleComponant) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.lab.PatientSample[ id=" + id + " ]";
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public Item getInvestigationComponant() {
        return investigationComponant;
    }

    public void setInvestigationComponant(Item investigationComponant) {
        this.investigationComponant = investigationComponant;
    }

    public Item getTest() {
        return test;
    }

    public void setTest(Item test) {
        this.test = test;
    }

    public InvestigationTube getTube() {
        return tube;
    }

    public void setTube(InvestigationTube tube) {
        this.tube = tube;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }

    public WebUser getSampleCollecter() {
        return sampleCollecter;
    }

    public void setSampleCollecter(WebUser sampleCollecter) {
        this.sampleCollecter = sampleCollecter;
    }

    public Date getSampledAt() {
        return sampledAt;
    }

    public void setSampledAt(Date sampledAt) {
        this.sampledAt = sampledAt;
    }

    public Boolean getSampleOutside() {
        return sampleOutside;
    }

    public void setSampleOutside(Boolean sampleOutside) {
        this.sampleOutside = sampleOutside;
    }

    public String getSampleComments() {
        return sampleComments;
    }

    public void setSampleComments(String sampleComments) {
        this.sampleComments = sampleComments;
    }

    public Department getSampleDepartment() {
        return sampleDepartment;
    }

    public void setSampleDepartment(Department sampleDepartment) {
        this.sampleDepartment = sampleDepartment;
    }

    public Institution getSampleInstitution() {
        return sampleInstitution;
    }

    public void setSampleInstitution(Institution sampleInstitution) {
        this.sampleInstitution = sampleInstitution;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public WebUser getCancelledUser() {
        return cancelledUser;
    }

    public void setCancelledUser(WebUser cancelledUser) {
        this.cancelledUser = cancelledUser;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellComments() {
        return cancellComments;
    }

    public void setCancellComments(String cancellComments) {
        this.cancellComments = cancellComments;
    }

    public Department getCancellDepartment() {
        return cancellDepartment;
    }

    public void setCancellDepartment(Department cancellDepartment) {
        this.cancellDepartment = cancellDepartment;
    }

    public Institution getCancellInstitution() {
        return cancellInstitution;
    }

    public void setCancellInstitution(Institution cancellInstitution) {
        this.cancellInstitution = cancellInstitution;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
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

    public PatientSample getPatientSample() {
        return patientSample;
    }

    public void setPatientSample(PatientSample patientSample) {
        this.patientSample = patientSample;
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


    
    
}
