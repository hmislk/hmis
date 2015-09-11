/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity
public class Sms implements Serializable {
    @ManyToOne
    private PatientInvestigation patientInvestigation;
    @ManyToOne
    private Bill bill;
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @ManyToOne
    PatientReport patientReport;
    @Lob
    String sendingMessage;
    @Lob
    String receivedMessage;
    String userId;
    String password;
    String sendingUrl;
    
    
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
    //DataEntry

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    
    
    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    
    
    public PatientReport getPatientReport() {
        return patientReport;
    }

    public void setPatientReport(PatientReport patientReport) {
        this.patientReport = patientReport;
    }

    public String getSendingMessage() {
        return sendingMessage;
    }

    public void setSendingMessage(String sendingMessage) {
        this.sendingMessage = sendingMessage;
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }

    public void setReceivedMessage(String receivedMessage) {
        this.receivedMessage = receivedMessage;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSendingUrl() {
        return sendingUrl;
    }

    public void setSendingUrl(String sendingUrl) {
        this.sendingUrl = sendingUrl;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Sms)) {
            return false;
        }
        Sms other = (Sms) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.Sms[ id=" + id + " ]";
    }
    
}
