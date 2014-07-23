/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class ServiceSession implements Serializable {

    @OneToMany(mappedBy = "serviceSession")
     List<Fee> fees;
     static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
     Long id;
     String name;
     String description;
    @Transient
     String dayString;
    int sessionWeekday;
    @Temporal(javax.persistence.TemporalType.DATE)
    Date sessionDate;
    @Temporal(javax.persistence.TemporalType.TIME)
    Date sessionTime;
    @Temporal(javax.persistence.TemporalType.TIME)
    Date sessionAt;
     double hospitalFee;
     double staffFee;
     double tax;
     double totalFee;
     double hospitalFfee;
     double staffFfee;
     double taxf;
     double totalFfee;
    int startingNo;
    int numberIncrement;
    int maxNo;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Department department;
    @ManyToOne
    Staff staff;
    @ManyToOne
    Speciality speciality;
    boolean continueNumbers;
    @Lob
    String comments;
    @OneToOne
    ServiceSession afterSession;
    @OneToOne(mappedBy = "afterSession")
    ServiceSession beforeSession;
    /////Newly Added
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
     Date startingTime;
     boolean refundable=false;
     int displayCount;
     double displayPercent;
     double duration;
     int roomNo;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getSessionWeekday() {
        return sessionWeekday;
    }

    public void setSessionWeekday(int sessionWeekday) {
        this.sessionWeekday = sessionWeekday;
    }

    public Date getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(Date sessionDate) {
        this.sessionDate = sessionDate;
    }

    public Date getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(Date sessionTime) {
        this.sessionTime = sessionTime;
    }

    public Date getSessionAt() {
        return sessionAt;
    }

    public void setSessionAt(Date sessionAt) {
        this.sessionAt = sessionAt;
    }

    public int getStartingNo() {
        return startingNo;
    }

    public void setStartingNo(int startingNo) {
        this.startingNo = startingNo;
    }

    public int getNumberIncrement() {
        return numberIncrement;
    }

    public void setNumberIncrement(int numberIncrement) {
        this.numberIncrement = numberIncrement;
    }

    public int getMaxNo() {
        return maxNo;
    }

    public void setMaxNo(int maxNo) {
        this.maxNo = maxNo;
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

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public boolean isContinueNumbers() {
        return continueNumbers;
    }

    public void setContinueNumbers(boolean continueNumbers) {
        this.continueNumbers = continueNumbers;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public ServiceSession getAfterSession() {
        return afterSession;
    }

    public void setAfterSession(ServiceSession afterSession) {
        this.afterSession = afterSession;
    }

    public ServiceSession getBeforeSession() {
        return beforeSession;
    }

    public void setBeforeSession(ServiceSession beforeSession) {
        this.beforeSession = beforeSession;
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
        
        if (!(object instanceof ServiceSession)) {
            return false;
        }
        ServiceSession other = (ServiceSession) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.ServiceSession[ id=" + id + " ]";
    }

    public String getDayString() {
        switch (sessionWeekday) {
            case 1:
                dayString = "Sunday";
                break;
            case 2:
                dayString = "Monday";
                break;
            case 3:
                dayString = "Tuesday";
                break;
            case 4:
                dayString = "Wednesday";
                break;
            case 5:
                dayString = "Thursday";
                break;
            case 6:
                dayString = "Friday";
                break;
            case 7:
                dayString = "Sutarday";
                break;

        }
        return dayString;
    }

    public Date getStartingTime() {
        return startingTime;
    }

    public void setStartingTime(Date startingTime) {
        this.startingTime = startingTime;
    }

    public boolean isRefundable() {
        return refundable;
    }

    public void setRefundable(boolean refundable) {
        this.refundable = refundable;
    }

    public int getDisplayCount() {
        return displayCount;
    }

    public void setDisplayCount(int displayCount) {
        this.displayCount = displayCount;
    }

    public double getDisplayPercent() {
        return displayPercent;
    }

    public void setDisplayPercent(double displayPercent) {
        this.displayPercent = displayPercent;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(int roomNo) {
        this.roomNo = roomNo;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(double staffFee) {
        this.staffFee = staffFee;
    }

    public double getTotalFee() {       
        return totalFee;
    }

    
    
    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }

    public double getHospitalForiegnFee() {
        return hospitalFfee;
    }

    public void setHospitalFfee(double hospitalFfee) {
        this.hospitalFfee = hospitalFfee;
    }

    public double getStaffForiegnFee() {
        return staffFfee;
    }

    public void setStaffFfee(double staffFfee) {
        this.staffFfee = staffFfee;
    }

    public double getTotalFfee() {
        return totalFfee;
    }

    public void setTotalFfee(double totalFfee) {
        this.totalFfee = totalFfee;
    }

    public List<Fee> getFees() {
        return fees;
    }

    public void setFees(List<Fee> fees) {
        this.fees = fees;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getTaxForiegn() {
        return taxf;
    }

    public void setTaxf(double taxf) {
        this.taxf = taxf;
    }
}
