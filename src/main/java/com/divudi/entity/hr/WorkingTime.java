/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.WorkingType;
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
    WorkingTime continuedTo;
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
    WorkingType workingType;

    boolean addedToSalary;
    
    
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
        staff = getStaffShift().getStaff();
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
}
