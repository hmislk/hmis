package com.divudi.entity;

import com.divudi.data.TokenType;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author acer
 */
@Entity
public class Token implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // <editor-fold defaultstate="collapsed" desc="Class variables">
    private String tokenId;
    private String tokenNumber;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date tokenDate;
     @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date tokenAt;
    @Enumerated(EnumType.ORDINAL)
    private TokenType tokenType;
    @ManyToOne
    private Category caterory;
    private Bill bill;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date issuedAt;
    private boolean called;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date calledAt;
    private boolean inProgress;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date startedAt;
    private boolean completed;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date completedAt;
    @ManyToOne
    private Patient patient;
    @ManyToOne
    private Department department;
    @ManyToOne
    private Department fromDepartment;
    @ManyToOne
    private Department toDepartment;
    @ManyToOne
    private Institution institution;
    @ManyToOne
    private Institution fromInstitution;
    @ManyToOne
    private Institution toInstitution;
    @ManyToOne
    private Department serviceCounter;
    @ManyToOne
    private WebUser createdBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    private boolean retired;
    @ManyToOne
    private WebUser retiredBy;
    @ManyToOne
    private Staff staff;
    @ManyToOne
    private Staff fromStaff;
    @ManyToOne
    private Staff toStaff;
    @ManyToOne
    private PatientEncounter patientEncounter;
    @ManyToOne
    private Department counter;
    @Transient
    private String idStr;
    @ManyToOne
    private Doctor doctor;
// </editor-fold> 

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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Token)) {
            return false;
        }
        Token other = (Token) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Token[ id=" + id + " ]";
    }

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(Date calledAt) {
        this.calledAt = calledAt;
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Patient getPatient() {
        if (patientEncounter != null) {
            patient = patientEncounter.getPatient();
        }
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getServiceCounter() {
        return serviceCounter;
    }

    public void setServiceCounter(Department serviceCounter) {
        this.serviceCounter = serviceCounter;
    }

    public WebUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(WebUser createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date CreatedAt) {
        this.createdAt = CreatedAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public WebUser getRetiredBy() {
        return retiredBy;
    }

    public void setRetiredBy(WebUser retiredBy) {
        this.retiredBy = retiredBy;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }
    // </editor-fold> 

    public boolean isCalled() {
        return called;
    }
    
    

    public void setCalled(boolean called) {
        this.called = called;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public Department getCounter() {
        return counter;
    }

    public void setCounter(Department counter) {
        this.counter = counter;
    }

    public Date getTokenDate() {
        return tokenDate;
    }

    public void setTokenDate(Date tokenDate) {
        this.tokenDate = tokenDate;
    }

    public Category getCaterory() {
        return caterory;
    }

    public void setCaterory(Category caterory) {
        this.caterory = caterory;
    }

    public String getIdStr() {
        return id + "";
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Date getTokenAt() {
        return tokenAt;
    }

    public void setTokenAt(Date tokenAt) {
        this.tokenAt = tokenAt;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

  
    
    
    
    
    
}
