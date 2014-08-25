/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity.clinical;

import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Person;
import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;



/**
 *
 * @author Buddhika
 */
@Entity
public class ClinicalFindingValue implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    
    @ManyToOne
    Person person;
   
    @ManyToOne(cascade = CascadeType.REFRESH)
    PatientEncounter encounter;
    @ManyToOne
    ClinicalFindingItem clinicalFindingItem;
    
    double doubleValue;
    @Lob
    String lobValue;
    String stringValue;
    long longValue;
    Byte[] imageValue;
    @ManyToOne
    Item itemValue;
    @ManyToOne
    Category categoryValue;

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public PatientEncounter getEncounter() {
        return encounter;
    }

    public void setEncounter(PatientEncounter encounter) {
        this.encounter = encounter;
    }

    public ClinicalFindingItem getClinicalFindingItem() {
        return clinicalFindingItem;
    }

    public void setClinicalFindingItem(ClinicalFindingItem clinicalFindingItem) {
        this.clinicalFindingItem = clinicalFindingItem;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public String getLobValue() {
        return lobValue;
    }

    public void setLobValue(String lobValue) {
        this.lobValue = lobValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public void setLongValue(long longValue) {
        this.longValue = longValue;
    }

    public Byte[] getImageValue() {
        return imageValue;
    }

    public void setImageValue(Byte[] imageValue) {
        this.imageValue = imageValue;
    }

    public Item getItemValue() {
        return itemValue;
    }

    public void setItemValue(Item itemValue) {
        this.itemValue = itemValue;
    }

    public Category getCategoryValue() {
        return categoryValue;
    }

    public void setCategoryValue(Category categoryValue) {
        this.categoryValue = categoryValue;
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
        
        if (!(object instanceof ClinicalFindingValue)) {
            return false;
        }
        ClinicalFindingValue other = (ClinicalFindingValue) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.clinical.ClinicalFindingValue[ id=" + id + " ]";
    }
    
}
