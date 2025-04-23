package com.divudi.core.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;

/**
 *
 * @author Chinthaka Prasad
 */
@Entity
public class OnlineBooking implements Serializable, RetirableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String patientName;
    private String nic;
    private String phoneNo;
    private String title;
    private Long referenceNo;
    private boolean foreign;
    private double onlineBookingPayment;
    private double appoinmentTotalAmount;
    private double netTotalForOnlineBooking;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;

    private boolean retired;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    @ManyToOne
    private WebUser retirer;
    private String retireComments;

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof OnlineBooking)) {
            return false;
        }
        OnlineBooking ob = (OnlineBooking) object;

        return id != null && id.equals(ob.id);
    }

    @Override
    public String toString() {
        return "com.divudi.core.entity.OnlineBooking[ id=" + id + " ]";
    }

    @Override
    public void setRetired(boolean retired) {
        this.retired = retired;
    }

    @Override
    public boolean isRetired() {
        return retired;
    }

    @Override
    public void setRetiredAt(Date retiredAt) {
        this.retiredAt = retiredAt;
    }

    @Override
    public Date getRetiredAt() {
        return retiredAt;
    }

    @Override
    public void setRetirer(WebUser retirer) {
        this.retirer = retirer;
    }

    @Override
    public WebUser getRetirer() {
        return retirer;
    }

    @Override
    public void setRetireComments(String comments) {
        this.retireComments = comments;
    }

    @Override
    public String getRetireComments() {
        return retireComments;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(Long referenceNo) {
        this.referenceNo = referenceNo;
    }

    public boolean isForeign() {
        return foreign;
    }

    public void setForeign(boolean foreign) {
        this.foreign = foreign;
    }

    public double getOnlineBookingPayment() {
        return onlineBookingPayment;
    }

    public void setOnlineBookingPayment(double onlineBookingPayment) {
        this.onlineBookingPayment = onlineBookingPayment;
    }

    public double getAppoinmentTotalAmount() {
        return appoinmentTotalAmount;
    }

    public void setAppoinmentTotalAmount(double appoinmentTotalAmount) {
        this.appoinmentTotalAmount = appoinmentTotalAmount;
    }

    public double getNetTotalForOnlineBooking() {
        return netTotalForOnlineBooking;
    }

    public void setNetTotalForOnlineBooking(double netTotalForOnlineBooking) {
        this.netTotalForOnlineBooking = netTotalForOnlineBooking;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

}
