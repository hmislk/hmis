/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 * @author Buddhika
 */
@Entity
@XmlRootElement
public class HrmVariables implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    /////////
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
    private double epfRate;
    private double etfRate;
    private double payeeRate;
    private double epfCompanyRate;
    private double etfCompanyRate;
    private double payeeCompanyRate;
    private double payeeLimit;
    @OneToMany(mappedBy = "hrmVariables", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PayeeTaxRange> taxRanges = new ArrayList<>();
    

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

        if (!(object instanceof HrmVariables)) {
            return false;
        }
        HrmVariables other = (HrmVariables) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.HrmVariables[ id=" + id + " ]";
    }

    public double getEpfRate() {
        return epfRate;
    }

    public void setEpfRate(double epfRate) {
        this.epfRate = epfRate;
    }

    public double getEtfRate() {
        return etfRate;
    }

    public void setEtfRate(double etfRate) {
        this.etfRate = etfRate;
    }

    public double getPayeeRate() {
        return payeeRate;
    }

    public void setPayeeRate(double payeeRate) {
        this.payeeRate = payeeRate;
    }

    public double getEpfCompanyRate() {
        return epfCompanyRate;
    }

    public void setEpfCompanyRate(double epfCompanyRate) {
        this.epfCompanyRate = epfCompanyRate;
    }

    public double getEtfCompanyRate() {
        return etfCompanyRate;
    }

    public void setEtfCompanyRate(double etfCompanyRate) {
        this.etfCompanyRate = etfCompanyRate;
    }

    public double getPayeeCompanyRate() {
        return payeeCompanyRate;
    }

    public void setPayeeCompanyRate(double payeeCompanyRate) {
        this.payeeCompanyRate = payeeCompanyRate;
    }

    public double getPayeeLimit() {
        return payeeLimit;
    }

    public void setPayeeLimit(double payeeLimit) {
        this.payeeLimit = payeeLimit;
    }

    @XmlTransient
    public List<PayeeTaxRange> getTaxRanges() {
        if (taxRanges == null) {
            taxRanges = new ArrayList<>();
        }
        return taxRanges;
    }

    public void setTaxRanges(List<PayeeTaxRange> taxRanges) {
        this.taxRanges = taxRanges;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
