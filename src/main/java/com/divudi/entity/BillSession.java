/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity;

import com.divudi.data.dataStructure.ChannelFee;
import com.divudi.entity.channel.SessionInstance;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author www.divudi.com
 */
@Entity
public class BillSession implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;
    @ManyToOne
    Packege packege;
    @ManyToOne
    Item item;
    String name;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Completed Properties
    private Boolean markedToCancel;
    private Boolean markedToRefund;
    private Boolean nextInLine;
    private Boolean currentlyConsulted;
    private boolean completed;
    @ManyToOne
    private WebUser completedBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date completedAt;
    
    @ManyToOne
    private WebUser markedToCancelBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date markedToCancelAt;
    @ManyToOne
    private WebUser markedToRefundBy;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date markedToRefundAt;
    
    
    
    
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;

    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Staff staff;
    @ManyToOne
    @Deprecated //Use SessionInstance instead
    ServiceSession serviceSession;
    @ManyToOne
    private SessionInstance sessionInstance;
    @Deprecated
    @ManyToOne
    private ServiceSessionInstance serviceSessionInstance;
    @ManyToOne
    Category category;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date sessionDate;
    @Temporal(javax.persistence.TemporalType.TIME)
    Date sessionTime;
    int serialNo;
    //Present
    //  Boolean present = true;
    //Absent
    boolean absent = false;
    @ManyToOne
    WebUser absentMarkedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date absentMarkedAt;
    @ManyToOne
    WebUser absentUnmarkedUser;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date absentUnmarkedAt;
    @ManyToOne
    PatientEncounter patientEncounter;
    @ManyToOne
    Bill bill;
    @OneToOne(fetch = FetchType.LAZY)
    BillItem billItem;
    @ManyToOne
    //Used to Save Billed Session and Cancell Bill Session Data
    BillSession referenceBillSession;
    @ManyToOne
    //Used in Credit Payment For BillSession
    BillSession paidBillSession;
    //Transient Only Reporting Purpose
    @Transient
    ChannelFee doctorFee;
    @Transient
    ChannelFee tax;
    @Transient
    ChannelFee hospitalFee;
    @Transient
    ChannelFee agentFee;
    private boolean reservedBooking=false;

    public void copy(BillSession billSession) {
        packege = billSession.getPackege();
        item = billSession.getItem();
        name = billSession.getName();
        institution = billSession.getInstitution();
        department = billSession.getDepartment();
        staff = billSession.getStaff();
        serviceSession = billSession.getServiceSession();
        sessionInstance = billSession.getSessionInstance();
        category = billSession.getCategory();
        sessionDate = billSession.getSessionDate();
        sessionTime = billSession.getSessionTime();
        serialNo = billSession.getSerialNo();
        absent = billSession.isAbsent();
        patientEncounter = billSession.getPatientEncounter();
    }


    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public Date getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @Deprecated
    public ServiceSession getServiceSession() {
        //Use SessionInstance instead
        return serviceSession;
    }

    
    @Deprecated
    public void setServiceSession(ServiceSession serviceSession) {
        //Use SessionInstance instead
        this.serviceSession = serviceSession;
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

        if (!(object instanceof BillSession)) {
            return false;
        }
        BillSession other = (BillSession) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.BillSession[ id=" + id + " ]";
    }

    public Packege getPackege() {
        return packege;
    }

    public void setPackege(Packege packege) {
        this.packege = packege;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
    
    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
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

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Date getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(Date sessionTime) {
        this.sessionTime = sessionTime;
    }

    public ChannelFee getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(ChannelFee doctorFee) {
        this.doctorFee = doctorFee;
    }

    public ChannelFee getTax() {
        return tax;
    }

    public void setTax(ChannelFee tax) {
        this.tax = tax;
    }

    public ChannelFee getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(ChannelFee hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public ChannelFee getAgentFee() {
        return agentFee;
    }

    public void setAgentFee(ChannelFee agentFee) {
        this.agentFee = agentFee;
    }

    public boolean isAbsent() {
        return absent;
    }

    public void setAbsent(boolean absent) {
        this.absent = absent;
    }

    public WebUser getAbsentMarkedUser() {
        return absentMarkedUser;
    }

    public void setAbsentMarkedUser(WebUser absentMarkedUser) {
        this.absentMarkedUser = absentMarkedUser;
    }

    public Date getAbsentMarkedAt() {
        return absentMarkedAt;
    }

    public void setAbsentMarkedAt(Date absentMarkedAt) {
        this.absentMarkedAt = absentMarkedAt;
    }

    public WebUser getAbsentUnmarkedUser() {
        return absentUnmarkedUser;
    }

    public void setAbsentUnmarkedUser(WebUser absentUnmarkedUser) {
        this.absentUnmarkedUser = absentUnmarkedUser;
    }

    public Date getAbsentUnmarkedAt() {
        return absentUnmarkedAt;
    }

    public void setAbsentUnmarkedAt(Date absentUnmarkedAt) {
        this.absentUnmarkedAt = absentUnmarkedAt;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public BillSession getReferenceBillSession() {
        return referenceBillSession;
    }

    public void setReferenceBillSession(BillSession referenceBillSession) {
        this.referenceBillSession = referenceBillSession;
    }

    public BillSession getPaidBillSession() {
        return paidBillSession;
    }

    public void setPaidBillSession(BillSession paidBillSession) {
        this.paidBillSession = paidBillSession;
    }

    @Deprecated
    public ServiceSessionInstance getServiceSessionInstance() {
        // Use sessionInstance inatead
        return serviceSessionInstance;
    }

    @Deprecated
    public void setServiceSessionInstance(ServiceSessionInstance serviceSessionInstance) {
        // Use sessionInstance inatead
        this.serviceSessionInstance = serviceSessionInstance;
    }

    public SessionInstance getSessionInstance() {
        return sessionInstance;
    }

    public void setSessionInstance(SessionInstance sessionInstance) {
        this.sessionInstance = sessionInstance;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public WebUser getCompletedBy() {
        return completedBy;
    }

    public void setCompletedBy(WebUser completedBy) {
        this.completedBy = completedBy;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getMarkedToCancel() {
        return markedToCancel;
    }

    public void setMarkedToCancel(Boolean markedToCancel) {
        this.markedToCancel = markedToCancel;
    }

    public Boolean getMarkedToRefund() {
        return markedToRefund;
    }

    public void setMarkedToRefund(Boolean markedToRefund) {
        this.markedToRefund = markedToRefund;
    }

    public Boolean getNextInLine() {
        return nextInLine;
    }

    public void setNextInLine(Boolean nextInLine) {
        this.nextInLine = nextInLine;
    }

    public Boolean getCurrentlyConsulted() {
        return currentlyConsulted;
    }

    public void setCurrentlyConsulted(Boolean currentlyConsulted) {
        this.currentlyConsulted = currentlyConsulted;
    }

    public WebUser getMarkedToCancelBy() {
        return markedToCancelBy;
    }

    public void setMarkedToCancelBy(WebUser markedToCancelBy) {
        this.markedToCancelBy = markedToCancelBy;
    }

    public Date getMarkedToCancelAt() {
        return markedToCancelAt;
    }

    public void setMarkedToCancelAt(Date markedToCancelAt) {
        this.markedToCancelAt = markedToCancelAt;
    }

    public WebUser getMarkedToRefundBy() {
        return markedToRefundBy;
    }

    public void setMarkedToRefundBy(WebUser markedToRefundBy) {
        this.markedToRefundBy = markedToRefundBy;
    }

    public Date getMarkedToRefundAt() {
        return markedToRefundAt;
    }

    public void setMarkedToRefundAt(Date markedToRefundAt) {
        this.markedToRefundAt = markedToRefundAt;
    }

    public boolean isReservedBooking() {
        return reservedBooking;
    }

    public void setReservedBooking(boolean reservedBooking) {
        this.reservedBooking = reservedBooking;
    }
    
    
    

}
