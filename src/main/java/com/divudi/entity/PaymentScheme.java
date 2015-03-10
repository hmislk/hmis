/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.entity;

import com.divudi.data.CliantType;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.memberShip.MembershipScheme;
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
public class PaymentScheme implements Serializable {

    static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    //Main Properties
    Long id;
    String name;
    String printingName;
  //  @Enumerated(EnumType.STRING)
    //PaymentMethod paymentMethod;
    @Enumerated(EnumType.STRING)
    CliantType cliantType;
    //double discountPercent = 0.0;
 //   double discountPercentForPharmacy = 0.0;
    @ManyToOne
    Institution institution;
    @ManyToOne
    Person person;
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
    boolean validForPayments;
    boolean validForBilledBills;
    boolean validForCrBills;
    
//    @ManyToOne
//    MembershipScheme membershipScheme;
//
//    public MembershipScheme getMembershipScheme() {
//        return membershipScheme;
//    }
//
//    public void setMembershipScheme(MembershipScheme membershipScheme) {
//        this.membershipScheme = membershipScheme;
//    }
//    
//    public double getDiscountPercentForPharmacy() {
//        return discountPercentForPharmacy;
//    }
//
//    public void setDiscountPercentForPharmacy(double discountPercentForPharmacy) {
//        this.discountPercentForPharmacy = discountPercentForPharmacy;
//    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public boolean isValidForPayments() {
        return validForPayments;
    }

    public void setValidForPayments(boolean validForPayments) {
        this.validForPayments = validForPayments;
    }

    public boolean isValidForBilledBills() {
        return validForBilledBills;
    }

    public void setValidForBilledBills(boolean validForBilledBills) {
        this.validForBilledBills = validForBilledBills;
    }

    public boolean isValidForCrBills() {
        return validForCrBills;
    }

    public void setValidForCrBills(boolean validForCrBills) {
        this.validForCrBills = validForCrBills;
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

        if (!(object instanceof PaymentScheme)) {
            return false;
        }
        PaymentScheme other = (PaymentScheme) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.PaymentSheme[ id=" + id + " ]";
    }

//    public PaymentMethod getPaymentMethod() {
//        return paymentMethod;
//    }
//
//    public void setPaymentMethod(PaymentMethod paymentMethod) {
//        this.paymentMethod = paymentMethod;
//    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CliantType getCliantType() {
        return cliantType;
    }

    public void setCliantType(CliantType cliantType) {
        this.cliantType = cliantType;
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

    public String getPrintingName() {
        return printingName;
    }

    public void setPrintingName(String printingName) {
        this.printingName = printingName;
    }
    
    

//    public double getDiscountPercent() {
//        return discountPercent;
//    }
//
//    public void setDiscountPercent(double discountPercent) {
//        this.discountPercent = discountPercent;
//    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }
}
