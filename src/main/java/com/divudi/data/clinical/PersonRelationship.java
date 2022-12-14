/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.data.clinical;

import com.divudi.data.hr.Relationship;
import com.divudi.entity.Person;
import com.divudi.entity.WebUser;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author buddhika
 */
@Entity
public class PersonRelationship implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    Person aperson;
    @ManyToOne
    Person bperson;
    @Enumerated(EnumType.STRING)
    Relationship apToBpRelationship;
    int orderNo;
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

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
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

    public Person getAperson() {
        return aperson;
    }

    public void setAperson(Person aperson) {
        this.aperson = aperson;
    }

    public Person getBperson() {
        return bperson;
    }

    public void setBperson(Person bperson) {
        this.bperson = bperson;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Relationship getApToBpRelationship() {
        return apToBpRelationship;
    }

    public void setApToBpRelationship(Relationship apToBpRelationship) {
        this.apToBpRelationship = apToBpRelationship;
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
        if (!(object instanceof PersonRelationship)) {
            return false;
        }
        PersonRelationship other = (PersonRelationship) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.PersonRelationship[ id=" + id + " ]";
    }

}
