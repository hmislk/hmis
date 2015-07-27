/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.pharmacy;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Person;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 *
 * @author Buddhika
 */
@Entity
public class Reorder implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @ManyToOne
    Item item;
    @ManyToOne
    Department department;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Person person;
    double rol;
    double roq;

    /**
     *
     * Demand the amount of items consumed by customers, on average, per unit
     * time.
     *
     */
        double demandInUnitsPerDay;

    /**
     *
     * Lead Time
     *
     * The delay between the time the reorder point (inventory level which
     * initiates an order) is reached and renewed availability.
     *
     */
    int leadTimeInDays;

    /**
     *
     *
     *
     */
    double bufferStocks;

    /**
     * Service Level The desired probability that a chosen level of safety stock
     * will not lead to a stockout. Naturally, when the desired service level is
     * increased, the required safety stock increases as well.
     *     
*/
    double serviceLevel;
    int purchaseCycleDurationInDays;
    
    int monthsConsideredForShortTermAnalysis;
    int yearsConsideredForLognTermAnalysis;

    @ManyToOne
    Institution supplier;
    
    @Transient
    Double transientStock;
    @Transient
    Double transientOrderingQty;
    boolean genericOrdering;

    public Double getTransientOrderingQty() {
        return transientOrderingQty;
    }

    public void setTransientOrderingQty(Double transientOrderingQty) {
        this.transientOrderingQty = transientOrderingQty;
    }
    
    public Double getTransientStock() {
        return transientStock;
    }

    public void setTransientStock(Double transientStock) {
        this.transientStock = transientStock;
    }
    
    public int getMonthsConsideredForShortTermAnalysis() {
        return monthsConsideredForShortTermAnalysis;
    }

    public void setMonthsConsideredForShortTermAnalysis(int monthsConsideredForShortTermAnalysis) {
        this.monthsConsideredForShortTermAnalysis = monthsConsideredForShortTermAnalysis;
    }

    public int getYearsConsideredForLognTermAnalysis() {
        return yearsConsideredForLognTermAnalysis;
    }

    public void setYearsConsideredForLognTermAnalysis(int yearsConsideredForLognTermAnalysis) {
        this.yearsConsideredForLognTermAnalysis = yearsConsideredForLognTermAnalysis;
    }
    
    public double getDemandInUnitsPerDay() {
        return demandInUnitsPerDay;
    }

    public void setDemandInUnitsPerDay(double demandInUnitsPerDay) {
        this.demandInUnitsPerDay = demandInUnitsPerDay;
    }

    public int getLeadTimeInDays() {
        return leadTimeInDays;
    }

    public void setLeadTimeInDays(int leadTimeInDays) {
        this.leadTimeInDays = leadTimeInDays;
    }

    public double getBufferStocks() {
        return bufferStocks;
    }

    public void setBufferStocks(double bufferStocks) {
        this.bufferStocks = bufferStocks;
    }

    public double getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(double serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    public int getPurchaseCycleDurationInDays() {
        return purchaseCycleDurationInDays;
    }

    public void setPurchaseCycleDurationInDays(int purchaseCycleDurationInDays) {
        this.purchaseCycleDurationInDays = purchaseCycleDurationInDays;
    }

    public Institution getSupplier() {
        return supplier;
    }

    public void setSupplier(Institution supplier) {
        this.supplier = supplier;
    }
    
    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public double getRol() {
        return rol;
    }

    public void setRol(double rol) {
        this.rol = rol;
    }

    public double getRoq() {
        return roq;
    }

    public void setRoq(double roq) {
        this.roq = roq;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isGenericOrdering() {
        return genericOrdering;
    }

    public void setGenericOrdering(boolean genericOrdering) {
        this.genericOrdering = genericOrdering;
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
        if (!(object instanceof Reorder)) {
            return false;
        }
        Reorder other = (Reorder) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.pharmacy.Reorder[ id=" + id + " ]";
    }

}
