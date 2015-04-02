/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
 * @author Buddhika
 */
@Entity
@XmlRootElement
public class SalaryCycle implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date salaryFromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date salaryToDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date overTimeFromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date overTimeToDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date extraDutyFromDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date extraDutyToDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date workedToDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date workedFromDate;
     @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dayOffPhToDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date dayOffPhFromDate;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date salaryAdvanceFromDate;
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date salaryAdvanceToDate;
    private int salaryYear;
    private int salaryMonth;
    private int salaryDate;

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
    private String name;

    public void processName() {
        name = "";
        if (salaryFromDate != null) {
            name = new SimpleDateFormat("yyyy-MM-dd").format(salaryFromDate);
        }

        if (salaryToDate != null) {
            name += " to " + new SimpleDateFormat("yyyy-MM-dd").format(salaryToDate);
        }

        if (salaryFromDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(salaryFromDate);
            salaryYear = cal.get(Calendar.YEAR);
            salaryMonth = cal.get(Calendar.MONTH);
            salaryDate = cal.get(Calendar.DATE);
        }

    }

    public Date getWorkedToDate() {
        return workedToDate;
    }

    public void setWorkedToDate(Date workedToDate) {
        this.workedToDate = workedToDate;
    }

    public Date getWorkedFromDate() {
        return workedFromDate;
    }

    public void setWorkedFromDate(Date workedFromDate) {
        this.workedFromDate = workedFromDate;
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

        if (!(object instanceof SalaryCycle)) {
            return false;
        }
        SalaryCycle other = (SalaryCycle) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.data.hr.SalaryCycle[ id=" + id + " ]";
    }

    public int getSalaryYear() {
        return salaryYear;
    }

    public void setSalaryYear(int salaryYear) {
        this.salaryYear = salaryYear;
    }

    public int getSalaryMonth() {
        return salaryMonth;
    }

    public void setSalaryMonth(int salaryMonth) {
        this.salaryMonth = salaryMonth;
    }

    public int getSalaryDate() {
        return salaryDate;
    }

    public void setSalaryDate(int salaryDate) {
        this.salaryDate = salaryDate;
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

    public Date getExtraDutyFromDate() {
        return extraDutyFromDate;
    }

    public void setExtraDutyFromDate(Date extraDutyFromDate) {
        this.extraDutyFromDate = extraDutyFromDate;
    }

    public Date getExtraDutyToDate() {
        return extraDutyToDate;
    }

    public void setExtraDutyToDate(Date extraDutyToDate) {
        this.extraDutyToDate = extraDutyToDate;
    }

    public Date getSalaryFromDate() {
        return salaryFromDate;
    }

    public void setSalaryFromDate(Date salaryFromDate) {
        this.salaryFromDate = salaryFromDate;
    }

    public Date getSalaryToDate() {
        return salaryToDate;
    }

    public void setSalaryToDate(Date salaryToDate) {
        this.salaryToDate = salaryToDate;
    }

    public Date getOverTimeFromDate() {
        return overTimeFromDate;
    }

    public void setOverTimeFromDate(Date overTimeFromDate) {
        this.overTimeFromDate = overTimeFromDate;
    }

    public Date getOverTimeToDate() {
        return overTimeToDate;
    }

    public void setOverTimeToDate(Date overTimeToDate) {
        this.overTimeToDate = overTimeToDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getSalaryAdvanceFromDate() {
        return salaryAdvanceFromDate;
    }

    public void setSalaryAdvanceFromDate(Date salaryAdvanceFromDate) {
        this.salaryAdvanceFromDate = salaryAdvanceFromDate;
    }

    public Date getSalaryAdvanceToDate() {
        return salaryAdvanceToDate;
    }

    public void setSalaryAdvanceToDate(Date salaryAdvanceToDate) {
        this.salaryAdvanceToDate = salaryAdvanceToDate;
    }

    public Date getDayOffPhToDate() {
        return dayOffPhToDate;
    }

    public void setDayOffPhToDate(Date dayOffPhToDate) {
        this.dayOffPhToDate = dayOffPhToDate;
    }

    public Date getDayOffPhFromDate() {
        return dayOffPhFromDate;
    }

    public void setDayOffPhFromDate(Date dayOffPhFromDate) {
        this.dayOffPhFromDate = dayOffPhFromDate;
    }

    
    
    
    
}
