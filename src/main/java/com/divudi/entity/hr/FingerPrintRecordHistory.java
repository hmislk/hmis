/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author safrin
 */
@Entity
@XmlRootElement
public class FingerPrintRecordHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    private FingerPrintRecord fingerPrintRecord;
    @ManyToOne
    Staff staff;
    @ManyToOne
    Shift shift;
    @ManyToOne
    Roster roster;
    @ManyToOne
    StaffShift staffShift;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date beforeChange;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date afterChange;
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

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }
    
    

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FingerPrintRecordHistory)) {
            return false;
        }
        FingerPrintRecordHistory other = (FingerPrintRecordHistory) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return id + "";
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
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

    public FingerPrintRecord getFingerPrintRecord() {
        return fingerPrintRecord;
    }

    public void setFingerPrintRecord(FingerPrintRecord fingerPrintRecord) {
        this.fingerPrintRecord = fingerPrintRecord;
    }

    public Date getBeforeChange() {
        return beforeChange;
    }

    public void setBeforeChange(Date beforeChange) {
        this.beforeChange = beforeChange;
    }

    public Date getAfterChange() {
        return afterChange;
    }

    public void setAfterChange(Date afterChange) {
        this.afterChange = afterChange;
    }

}
