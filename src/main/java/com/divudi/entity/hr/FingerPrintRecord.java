/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.data.hr.DayType;
import com.divudi.data.hr.FingerPrintRecordType;
import com.divudi.data.hr.Times;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.lab.ReportItem;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import javax.persistence.Column;
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
public class FingerPrintRecord implements Serializable {

    @OneToOne(mappedBy = "loggedRecord")
    private FingerPrintRecord verifiedRecord;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Enumerated(EnumType.STRING)
    FingerPrintRecordType fingerPrintRecordType;
    @OneToOne
    FingerPrintRecord loggedRecord;
    @ManyToOne
    Staff staff;
    @ManyToOne
    Roster roster;
    

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date recordTimeStamp;
    @Column(name = "allowedOverTime")
    boolean allowedExtraDuty;

    @ManyToOne
    private StaffShift staffShift;

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
    //Approving Properties
    private boolean approved;
    @ManyToOne
    private WebUser approver;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date approvedAt;
    private String approveComments;
    
    //private Boolean begining;
    @Enumerated(EnumType.STRING)
    private Times times;
    @Enumerated(EnumType.STRING)
    DayType dayType;
    String comments = "";
    @Transient
    boolean transNew;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public WebUser getApprover() {
        return approver;
    }

    public void setApprover(WebUser approver) {
        this.approver = approver;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getApproveComments() {
        return approveComments;
    }

    public void setApproveComments(String approveComments) {
        this.approveComments = approveComments;
    }
    
    

    public boolean isTransNew() {
        return transNew;
    }

    public void setTransNew(boolean transNew) {
        this.transNew = transNew;
    }

    public Roster getRoster() {
        return roster;
    }

    public void setRoster(Roster roster) {
        this.roster = roster;
    }

    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    public void copy(FingerPrintRecord fingerPrintRecord) {
        if (fingerPrintRecord == null) {
            return;
        }
        this.staff = fingerPrintRecord.getStaff();
        this.recordTimeStamp = fingerPrintRecord.getRecordTimeStamp();
        this.staffShift = fingerPrintRecord.getStaffShift();
        this.times = fingerPrintRecord.getTimes();
        this.verifiedRecord = fingerPrintRecord.getVerifiedRecord();
        this.loggedRecord = fingerPrintRecord.getLoggedRecord();
    }

    public String getTimeLabel() {

        Date date = getRecordTimeStamp();
        String output = "";
        SimpleDateFormat formatter;
        Locale currentLocale = new Locale("en", "US");
        String pattern = "dd-MM-yy hh:mm a";

        formatter = new SimpleDateFormat(pattern, currentLocale);
        try {
            output = formatter.format(date);
//         //   //System.out.println(pattern + " " + output);
        } catch (Exception e) {
            System.err.println(e.getMessage());

        }

        if (comments != null) {
            output += comments;
        }

        return output;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public FingerPrintRecord getVerifiedRecord() {
        return verifiedRecord;
    }

    public void setVerifiedRecord(FingerPrintRecord verifiedRecord) {
        this.verifiedRecord = verifiedRecord;
    }

    public FingerPrintRecordType getFingerPrintRecordType() {
        return fingerPrintRecordType;
    }

    public void setFingerPrintRecordType(FingerPrintRecordType fingerPrintRecordType) {
        this.fingerPrintRecordType = fingerPrintRecordType;
    }

    public FingerPrintRecord getLoggedRecord() {
        return loggedRecord;
    }

    public void setLoggedRecord(FingerPrintRecord loggedRecord) {
        this.loggedRecord = loggedRecord;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Date getRecordTimeStamp() {
        return recordTimeStamp;
    }

    public void setRecordTimeStamp(Date recordTimeStamp) {
        this.recordTimeStamp = recordTimeStamp;
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

        if (!(object instanceof FingerPrintRecord)) {
            return false;
        }
        FingerPrintRecord other = (FingerPrintRecord) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.FingerPrintRecord[ id=" + id + " ]";
    }

    public StaffShift getStaffShift() {
        return staffShift;
    }

    public void setStaffShift(StaffShift staffShift) {
        this.staffShift = staffShift;
    }

    public Times getTimes() {
        return times;
    }

    public void setTimes(Times times) {
        this.times = times;
    }

    public boolean isAllowedExtraDuty() {
        return allowedExtraDuty;
    }

    public void setAllowedExtraDuty(boolean allowedExtraDuty) {
        this.allowedExtraDuty = allowedExtraDuty;
    }

  

}
