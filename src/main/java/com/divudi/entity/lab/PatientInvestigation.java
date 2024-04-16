/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.lab;

import com.divudi.entity.BillComponent;
import com.divudi.entity.BillItem;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Packege;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;

/**
 *
 * @author Buddhika
 */
@Entity
public class PatientInvestigation implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
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
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private Investigation investigation;
    @ManyToOne
    private BillItem billItem;
    @ManyToOne
    BillComponent billComponent;
    @ManyToOne
    private Packege packege;
    @ManyToOne
    private PatientEncounter encounter;
    //Sample Collection
    private Boolean collected = false;
    @ManyToOne
    private WebUser sampleCollecter;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date sampledAt;
    private Boolean sampleOutside = false;
    private String sampleComments;
    @ManyToOne
    private Department sampleDepartment;
    @ManyToOne
    private Institution sampleInstitution;
    //SampleReceiveAtLabfo
    private Boolean received = false;
    @ManyToOne
    private WebUser receivedCollecter;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date receivedAt;
    private String receiveComments;
    @ManyToOne
    private Department receiveDepartment;
    @ManyToOne
    private Institution receiveInstitution;
    //Performed
    private Boolean performed = false;
    @ManyToOne
    private WebUser performedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date performedAt;
    private String performComments;
    @ManyToOne
    private Department performDepartment;
    @ManyToOne
    private Institution performInstitution;
    //DataEntry
    private Boolean dataEntered = false;
    @ManyToOne
    private WebUser dataEntryUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dataEntryAt;
    private String dataEntryComments;
    @ManyToOne
    private Department dataEntryDepartment;
    @ManyToOne
    private Institution dataEntryInstitution;
    //Approve
    private Boolean approved = false;
    @ManyToOne
    private WebUser approveUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date approveAt;
    private String approveComments;
    @ManyToOne
    private Department approveDepartment;
    @ManyToOne
    private Institution approveInstitution;
    //Printing
    private Boolean printed = false;
    @ManyToOne
    private WebUser printingUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date printingAt;
    private String printingComments;
    @ManyToOne
    private Department printingDepartment;
    @ManyToOne
    private Institution printingInstitution;
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
    //Return
    private Boolean returned = false;
    @ManyToOne
    private WebUser returnedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date returnedAt;
    private String returnComments;
    @ManyToOne
    private Department returnDepartment;
    @ManyToOne
    private Institution returnInstitution;
    //outsourced
    @ManyToOne
    private Institution outsourceInstitution;
    private Double outsourcedValue;
    private Boolean outsourced = false;
    @ManyToOne
    private WebUser outsourcedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date outsourcedAt;
    private String outsourcedComments;
    @ManyToOne
    private Department outsourcedDepartment;
    @ManyToOne
    private Institution outsourcedInstitution;
    @OneToMany(mappedBy="patientInvestigation",fetch = FetchType.EAGER)
    List<PatientReport> patientReports;

    public List<PatientReport> getPatientReports() {
        if(patientReports == null){
            patientReports = new ArrayList<>();
        }
        return patientReports;
    }

    public void setPatientReports(List<PatientReport> patientReports) {
        this.patientReports = patientReports;
    }
    
    
    
    public BillComponent getBillComponent() {
        return billComponent;
    }

    public void setBillComponent(BillComponent billComponent) {
        this.billComponent = billComponent;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        
        if (!(object instanceof PatientInvestigation)) {
            return false;
        }
        PatientInvestigation other = (PatientInvestigation) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.PatientInvestigation[ id=" + id + " ]";
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public PatientEncounter getEncounter() {
        return encounter;
    }

    public void setEncounter(PatientEncounter encounter) {
        this.encounter = encounter;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }

    public Boolean getSampleOutside() {
        //////// // System.out.println("Getting "+sampleOutside);
        
        return sampleOutside;
    }

    public void setSampleOutside(Boolean sampleOutside) {
        //////// // System.out.println("Setting "+sampleOutside);
        this.sampleOutside = sampleOutside;
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

    public Boolean getReceived() {
        return received;
    }

    public void setReceived(Boolean received) {
        this.received = received;
    }

    public Department getReceiveDepartment() {
        return receiveDepartment;
    }

    public void setReceiveDepartment(Department receiveDepartment) {
        this.receiveDepartment = receiveDepartment;
    }

    public Institution getReceiveInstitution() {
        return receiveInstitution;
    }

    public void setReceiveInstitution(Institution receiveInstitution) {
        this.receiveInstitution = receiveInstitution;
    }

    public Boolean getPerformed() {
        return performed;
    }

    public void setPerformed(Boolean performed) {
        this.performed = performed;
    }

    public Department getPerformDepartment() {
        return performDepartment;
    }

    public void setPerformDepartment(Department performDepartment) {
        this.performDepartment = performDepartment;
    }

    public Institution getPerformInstitution() {
        return performInstitution;
    }

    public void setPerformInstitution(Institution performInstitution) {
        this.performInstitution = performInstitution;
    }

    public Boolean getDataEntered() {
        return dataEntered;
    }

    public void setDataEntered(Boolean dataEntered) {
        this.dataEntered = dataEntered;
    }

    public Department getDataEntryDepartment() {
        return dataEntryDepartment;
    }

    public void setDataEntryDepartment(Department dataEntryDepartment) {
        this.dataEntryDepartment = dataEntryDepartment;
    }

    public Institution getDataEntryInstitution() {
        return dataEntryInstitution;
    }

    public void setDataEntryInstitution(Institution dataEntryInstitution) {
        this.dataEntryInstitution = dataEntryInstitution;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public Department getApproveDepartment() {
        return approveDepartment;
    }

    public void setApproveDepartment(Department approveDepartment) {
        this.approveDepartment = approveDepartment;
    }

    public Institution getApproveInstitution() {
        return approveInstitution;
    }

    public void setApproveInstitution(Institution approveInstitution) {
        this.approveInstitution = approveInstitution;
    }

    public Boolean getPrinted() {
        return printed;
    }

    public void setPrinted(Boolean printed) {
        this.printed = printed;
    }

    public Department getPrintingDepartment() {
        return printingDepartment;
    }

    public void setPrintingDepartment(Department printingDepartment) {
        this.printingDepartment = printingDepartment;
    }

    public Institution getPrintingInstitution() {
        return printingInstitution;
    }

    public void setPrintingInstitution(Institution printingInstitution) {
        this.printingInstitution = printingInstitution;
    }

    public Boolean getCancelled() {
        
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
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

    public Boolean getReturned() {
        return returned;
    }

    public void setReturned(Boolean returned) {
        this.returned = returned;
    }

    public Department getReturnDepartment() {
        return returnDepartment;
    }

    public void setReturnDepartment(Department returnDepartment) {
        this.returnDepartment = returnDepartment;
    }

    public Institution getReturnInstitution() {
        return returnInstitution;
    }

    public void setReturnInstitution(Institution returnInstitution) {
        this.returnInstitution = returnInstitution;
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

    public String getSampleComments() {
        return sampleComments;
    }

    public void setSampleComments(String sampleComments) {
        this.sampleComments = sampleComments;
    }

    public WebUser getReceivedCollecter() {
        return receivedCollecter;
    }

    public void setReceivedCollecter(WebUser receivedCollecter) {
        this.receivedCollecter = receivedCollecter;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getReceiveComments() {
        return receiveComments;
    }

    public void setReceiveComments(String receiveComments) {
        this.receiveComments = receiveComments;
    }

    public WebUser getPerformedUser() {
        return performedUser;
    }

    public void setPerformedUser(WebUser performedUser) {
        this.performedUser = performedUser;
    }

    public Date getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Date performedAt) {
        this.performedAt = performedAt;
    }

    public String getPerformComments() {
        return performComments;
    }

    public void setPerformComments(String performComments) {
        this.performComments = performComments;
    }

    public WebUser getDataEntryUser() {
        return dataEntryUser;
    }

    public void setDataEntryUser(WebUser dataEntryUser) {
        this.dataEntryUser = dataEntryUser;
    }

    public Date getDataEntryAt() {
        return dataEntryAt;
    }

    public void setDataEntryAt(Date dataEntryAt) {
        this.dataEntryAt = dataEntryAt;
    }

    public String getDataEntryComments() {
        return dataEntryComments;
    }

    public void setDataEntryComments(String dataEntryComments) {
        this.dataEntryComments = dataEntryComments;
    }

    public WebUser getApproveUser() {
        return approveUser;
    }

    public void setApproveUser(WebUser approveUser) {
        this.approveUser = approveUser;
    }

    public Date getApproveAt() {
        return approveAt;
    }

    public void setApproveAt(Date approveAt) {
        this.approveAt = approveAt;
    }

    public String getApproveComments() {
        return approveComments;
    }

    public void setApproveComments(String approveComments) {
        this.approveComments = approveComments;
    }

    public WebUser getPrintingUser() {
        return printingUser;
    }

    public void setPrintingUser(WebUser printingUser) {
        this.printingUser = printingUser;
    }

    public Date getPrintingAt() {
        return printingAt;
    }

    public void setPrintingAt(Date printingAt) {
        this.printingAt = printingAt;
    }

    public String getPrintingComments() {
        return printingComments;
    }

    public void setPrintingComments(String printingComments) {
        this.printingComments = printingComments;
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

    public WebUser getReturnedUser() {
        return returnedUser;
    }

    public void setReturnedUser(WebUser returnedUser) {
        this.returnedUser = returnedUser;
    }

    public Date getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(Date returnedAt) {
        this.returnedAt = returnedAt;
    }

    public String getReturnComments() {
        return returnComments;
    }

    public void setReturnComments(String returnComments) {
        this.returnComments = returnComments;
    }

    public Institution getOutsourceInstitution() {
        return outsourceInstitution;
    }

    public void setOutsourceInstitution(Institution outsourceInstitution) {
        this.outsourceInstitution = outsourceInstitution;
    }

    public Double getOutsourcedValue() {
        return outsourcedValue;
    }

    public void setOutsourcedValue(Double outsourcedValue) {
        this.outsourcedValue = outsourcedValue;
    }

    public Boolean getOutsourced() {
        return outsourced;
    }

    public void setOutsourced(Boolean outsourced) {
        this.outsourced = outsourced;
    }

    public WebUser getOutsourcedUser() {
        return outsourcedUser;
    }

    public void setOutsourcedUser(WebUser outsourcedUser) {
        this.outsourcedUser = outsourcedUser;
    }

    public Date getOutsourcedAt() {
        return outsourcedAt;
    }

    public void setOutsourcedAt(Date outsourcedAt) {
        this.outsourcedAt = outsourcedAt;
    }

    public String getOutsourcedComments() {
        return outsourcedComments;
    }

    public void setOutsourcedComments(String outsourcedComments) {
        this.outsourcedComments = outsourcedComments;
    }

    public Department getOutsourcedDepartment() {
        return outsourcedDepartment;
    }

    public void setOutsourcedDepartment(Department outsourcedDepartment) {
        this.outsourcedDepartment = outsourcedDepartment;
    }

    public Institution getOutsourcedInstitution() {
        return outsourcedInstitution;
    }

    public void setOutsourcedInstitution(Institution outsourcedInstitution) {
        this.outsourcedInstitution = outsourcedInstitution;
    }

    public Packege getPackege() {
        return packege;
    }

    public void setPackege(Packege packege) {
        this.packege = packege;
    }
}
