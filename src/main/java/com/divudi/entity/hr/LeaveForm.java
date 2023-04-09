/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.LeaveType;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@XmlRootElement
public class LeaveForm extends HrForm {

    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedDate;  
    @ManyToOne
    private Shift shift;
    private double numberOfLeave;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date fromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date toDate;
    @Enumerated(EnumType.STRING)
    LeaveType leaveType;

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    
    
    
  

    public Date getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(Date requestedDate) {
        this.requestedDate = requestedDate;
    }

    
    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public double getNumberOfLeave() {
        return numberOfLeave;
    }

    public void setNumberOfLeave(double numberOfLeave) {
        this.numberOfLeave = numberOfLeave;
    }

}
