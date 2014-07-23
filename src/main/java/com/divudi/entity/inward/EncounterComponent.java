/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.inward;

import com.divudi.entity.PatientEncounter;
import com.divudi.data.inward.PatientEncounterComponentType;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Buddhika
 */
@Entity
public class EncounterComponent implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

   
    @ManyToOne(cascade = CascadeType.REFRESH,fetch = FetchType.LAZY)
    PatientEncounter patientEncounter;
    @ManyToOne
    PatientEncounter childEncounter;
    @ManyToOne
    BillFee billFee;
    @ManyToOne
    BillItem billItem;
    PatientEncounterComponentType patientEncounterComponentType;
    @ManyToOne
    Staff staff;
    @ManyToOne
    Item item;
    @ManyToOne
    Category category;
    String name;
    @Lob
    String description;
    //Created Properties
    @ManyToOne
    WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date createdAt;
    //Retairing properties
    boolean retired;
    @ManyToOne
    WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date retiredAt;
    String retireComments;
    int orderNo;
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public PatientEncounter getChildEncounter() {
        return childEncounter;
    }

    public void setChildEncounter(PatientEncounter childEncounter) {
        this.childEncounter = childEncounter;
    }

    public BillFee getBillFee() {
        return billFee;
    }

    public void setBillFee(BillFee billFee) {
        this.billFee = billFee;
    }

    public BillItem getBillItem() {
        return billItem;
    }

    public void setBillItem(BillItem billItem) {
        this.billItem = billItem;
    }

    public PatientEncounterComponentType getPatientEncounterComponentType() {
        return patientEncounterComponentType;
    }

    public void setPatientEncounterComponentType(PatientEncounterComponentType patientEncounterComponentType) {
        this.patientEncounterComponentType = patientEncounterComponentType;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
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
        if (!(object instanceof EncounterComponent)) {
            return false;
        }
        EncounterComponent other = (EncounterComponent) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.inward.EncounterComponent[ id=" + id + " ]";
    }

}
