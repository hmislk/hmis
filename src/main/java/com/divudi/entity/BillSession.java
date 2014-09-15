/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.dataStructure.ChannelFee;
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
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
//    String deptId;
//    String insId;
//    String catId;
//    String sessionId;    
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Staff staff;
    @ManyToOne
    ServiceSession serviceSession;
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
    ////////////////////////////
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
//    double qty;
    //Transient Only Reporting Purpose
    @Transient
    ChannelFee doctorFee;
    @Transient
    ChannelFee tax;
    @Transient
    ChannelFee hospitalFee;
    @Transient
    ChannelFee agentFee;

    public void copy(BillSession billSession) {
        packege = billSession.getPackege();
        item = billSession.getItem();
        name = billSession.getName();
        institution = billSession.getInstitution();
        department = billSession.getDepartment();
        staff = billSession.getStaff();
        serviceSession = billSession.getServiceSession();
        category = billSession.getCategory();
        sessionDate = billSession.getSessionDate();
        sessionTime = billSession.getSessionTime();
        serialNo = billSession.getSerialNo();
        absent = billSession.isAbsent();
        patientEncounter = billSession.getPatientEncounter();

    }

//    public double getQty() {
//        return qty;
//    }
//
//    public void setQty(double qty) {
//        this.qty = qty;
//    }
//    
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

    public ServiceSession getServiceSession() {
        return serviceSession;
    }

    public void setServiceSession(ServiceSession serviceSession) {
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

//    public String getDeptId() {
//        return deptId;
//    }
//
//    public void setDeptId(String deptId) {
//        this.deptId = deptId;
//    }
//
//    public String getInsId() {
//        return insId;
//    }
//
//    public void setInsId(String insId) {
//        this.insId = insId;
//    }
//
//    public String getCatId() {
//        return catId;
//    }
//
//    public void setCatId(String catId) {
//        this.catId = catId;
//    }
//
//    public String getSessionId() {
//        return sessionId;
//    }
//
//    public void setSessionId(String sessionId) {
//        this.sessionId = sessionId;
//    }
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
//
//    public Boolean getPresent() {
//        return present;
//    }
//
//    public void setPresent(Boolean present) {
//        this.present = present;
//    }

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

}
