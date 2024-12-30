package com.divudi.entity.cashTransaction;

import com.divudi.bean.common.RetirableEntity;
import com.divudi.data.PaymentMethod;
import com.divudi.entity.Bill;
import com.divudi.entity.Payment;
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
 * @author M H Buddhika Ariyaratne
 * 
 */
@Entity
public class DrawerEntry implements Serializable, RetirableEntity  {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    
    @ManyToOne
    private Drawer drawer;
    @ManyToOne
    private WebUser webUser;
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    private double beforeBalance;
    private double afterBalance;
    private double beforeInHandValue;
    private double afterInHandValue;
    private double beforeShortageExcess;
    private double afterShortageExcess;
    @ManyToOne
    private Bill bill;
    @ManyToOne
    private Payment payment;
    
    
    @ManyToOne
    private WebUser creater;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;
    private boolean retired;
    @ManyToOne
    private WebUser retirer;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    private String retireComments;

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
        if (!(object instanceof DrawerEntry)) {
            return false;
        }
        DrawerEntry other = (DrawerEntry) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.divudi.entity.cashTransaction.DrawerEntry[ id=" + id + " ]";
    }

    public Drawer getDrawer() {
        return drawer;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getBeforeBalance() {
        return beforeBalance;
    }

    public void setBeforeBalance(double beforeBalance) {
        this.beforeBalance = beforeBalance;
    }

    public double getAfterBalance() {
        return afterBalance;
    }

    public void setAfterBalance(double afterBalance) {
        this.afterBalance = afterBalance;
    }

    public double getBeforeInHandValue() {
        return beforeInHandValue;
    }

    public void setBeforeInHandValue(double beforeInHandValue) {
        this.beforeInHandValue = beforeInHandValue;
    }

    public double getAfterInHandValue() {
        return afterInHandValue;
    }

    public void setAfterInHandValue(double afterInHandValue) {
        this.afterInHandValue = afterInHandValue;
    }

    public double getBeforeShortageExcess() {
        return beforeShortageExcess;
    }

    public void setBeforeShortageExcess(double beforeShortageExcess) {
        this.beforeShortageExcess = beforeShortageExcess;
    }

    public double getAfterShortageExcess() {
        return afterShortageExcess;
    }

    public void setAfterShortageExcess(double afterShortageExcess) {
        this.afterShortageExcess = afterShortageExcess;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
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
    
    
    
    
}
