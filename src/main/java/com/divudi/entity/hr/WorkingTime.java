/*
* Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.WorkingType;
import com.divudi.entity.Bill;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Buddhika
 */
@Entity
@XmlRootElement
public class WorkingTime implements Serializable {

    @OneToOne(mappedBy = "continuedTo")
    private WorkingTime continuedFrom;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    StaffShift staffShift;
    @Transient
    Staff staff;
    @ManyToOne
    FingerPrintRecord startRecord;
    @ManyToOne
    FingerPrintRecord endRecord;
    long durationInMinutes;
    @Transient
    long durationInHours;
    @OneToOne
    private WorkingTime continuedTo;
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
    @Enumerated(EnumType.STRING)
    private WorkingType workingType;

    private boolean addedToSalary;
    
    @ManyToOne
    private Bill professinoalPaymentBill;

    // 7am - 1pm - Normal
    // 1pm - 7pm > OT
    //7am - 7pm
    public FingerPrintRecord getStartRecord() {
        return startRecord;
    }

    public void setStartRecord(FingerPrintRecord startRecord) {
        this.startRecord = startRecord;
    }

    public FingerPrintRecord getEndRecord() {
        return endRecord;
    }

    public void setEndRecord(FingerPrintRecord endRecord) {
        this.endRecord = endRecord;
    }

    public long getDurationInMinutes() {
        return durationInMinutes;
    }

    
    
    public void setDurationInMinutes(long durationInMinutes) {
        this.durationInMinutes = durationInMinutes;
    }

    public long getDurationInHours() {
        durationInHours = durationInMinutes / 60;
        return durationInHours;
    }

    public void setDurationInHours(long durationInHours) {
        this.durationInHours = durationInHours;
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }

    public Staff getStaff() {
        if (getStaffShift() != null) {
            staff = getStaffShift().getStaff();
        }
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
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

        if (!(object instanceof WorkingTime)) {
            return false;
        }
        WorkingTime other = (WorkingTime) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.WorkingTime[ id=" + id + " ]";
    }

    public WorkingTime getContinuedTo() {
        return continuedTo;
    }

    public void setContinuedTo(WorkingTime continuedTo) {
        this.continuedTo = continuedTo;
    }

    public WorkingType getWorkingType() {
        return workingType;
    }

    public void setWorkingType(WorkingType workingType) {
        this.workingType = workingType;
    }

    public boolean isAddedToSalary() {
        return addedToSalary;
    }

    public void setAddedToSalary(boolean addedToSalary) {
        this.addedToSalary = addedToSalary;
    }

    public WorkingTime getContinuedFrom() {
        return continuedFrom;
    }

    public void setContinuedFrom(WorkingTime continuedFrom) {
        this.continuedFrom = continuedFrom;
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

    public Bill getProfessinoalPaymentBill() {
        return professinoalPaymentBill;
    }

    public void setProfessinoalPaymentBill(Bill professinoalPaymentBill) {
        this.professinoalPaymentBill = professinoalPaymentBill;
    }
}
