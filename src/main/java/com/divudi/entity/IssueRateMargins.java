/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.divudi.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author pdhs
 */
@Entity
public class IssueRateMargins implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    String name;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    @ManyToOne
    WebUser creater;
    boolean retired;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    @ManyToOne
    WebUser retirer;
    
    @ManyToOne
    Department fromDepartment;
    @ManyToOne
    Department toDepartment;
    @ManyToOne
    Institution fromInstitution;
    @ManyToOne
    Institution toInstitution;
    
    double rateForPharmaceuticals;
    double rateForConsumables;
    double rateForInventory;
    
    boolean showRates;
    boolean atPurchaseRate;

    public boolean isShowRates() {
        return showRates;
    }

    public void setShowRates(boolean showRates) {
        this.showRates = showRates;
    }

    public boolean isAtPurchaseRate() {
        return atPurchaseRate;
    }

    public void setAtPurchaseRate(boolean atPurchaseRate) {
        this.atPurchaseRate = atPurchaseRate;
    }
    
    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public WebUser getCreater() {
        return creater;
    }

    public void setCreater(WebUser creater) {
        this.creater = creater;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    public Date getRetiredAt() {
        return retiredAt;
    }

    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    public WebUser getRetirer() {
        return retirer;
    }

    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public double getRateForPharmaceuticals() {
        return rateForPharmaceuticals;
    }

    public void setRateForPharmaceuticals(double rateForPharmaceuticals) {
        this.rateForPharmaceuticals = rateForPharmaceuticals;
    }

    public double getRateForConsumables() {
        return rateForConsumables;
    }

    public void setRateForConsumables(double rateForConsumables) {
        this.rateForConsumables = rateForConsumables;
    }

    public double getRateForInventory() {
        return rateForInventory;
    }

    public void setRateForInventory(double rateForInventory) {
        this.rateForInventory = rateForInventory;
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
        if (!(object instanceof IssueRateMargins)) {
            return false;
        }
        IssueRateMargins other = (IssueRateMargins) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.IssueRateMargins[ id=" + id + " ]";
    }
    
}
