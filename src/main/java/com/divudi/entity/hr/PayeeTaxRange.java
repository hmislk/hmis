/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.hr;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author safrin
 */
@Entity
@XmlRootElement
public class PayeeTaxRange implements Serializable {
    @ManyToOne
    private HrmVariables hrmVariables;
 
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private double fromSalary;
    private double toSalary;
    private double taxRate;

    public double getFromSalary() {
        return fromSalary;
    }

    public void setFromSalary(double fromSalary) {
        this.fromSalary = fromSalary;
    }

    public double getToSalary() {
        return toSalary;
    }

    public void setToSalary(double toSalary) {
        this.toSalary = toSalary;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
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
        
        if (!(object instanceof PayeeTaxRange)) {
            return false;
        }
        PayeeTaxRange other = (PayeeTaxRange) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.hr.PayeeTaxRange[ id=" + id + " ]";
    }

}
