package com.divudi.core.entity;

import com.divudi.core.data.OnlineBookingStatus;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Chinthaka Prasad
 */
@Entity
public class OnlineBooking implements Serializable, RetirableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

//    class level variables
    private String patientName;
    private String nic;
    private String phoneNo;
    private String title;
    private String referenceNo;
    private String email;
    private String address;
    private boolean foreignStatus;
    private double onlineBookingPayment;
    private double appoinmentTotalAmount;
    private double netTotalForOnlineBooking;
    private boolean needSms;
    private boolean nsr;
    private boolean paid;
    private boolean edited;
    private String requestIp;
    private double hospitalFee;
    private double doctorFee;
    private boolean absent;
    private boolean canceled;
    private String cancelledBy;
    private boolean paidToHospital;
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidToHospitalDate;
    private WebUser paidToHospitalProcessedBy;

    @OneToOne(fetch = FetchType.EAGER)
    private Bill paidToHospitalBill;

    @OneToOne(fetch = FetchType.EAGER)
    private Bill paidToHospitalCancelledBill;

    private WebUser paidToHospitalBillCancelledBy;
    @Temporal(TemporalType.TIMESTAMP)
    private Date paidToHospitalBillCancelledAt;
    private String comment;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date editedAt;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date createdAt;

//    variables related to object retire
    private boolean retired;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date retiredAt;
    @ManyToOne
    private WebUser retirer;
    private String retireComments;

    @ManyToOne
    private Institution agency;

    @ManyToOne
    private Department department;

    @ManyToOne
    private Institution hospital;

    @OneToOne(mappedBy = "onlineBooking", fetch = FetchType.EAGER)
    private Bill bill;

    @Enumerated(EnumType.STRING)
    private OnlineBookingStatus onlineBookingStatus;

    public Bill getPaidToHospitalCancelledBill() {
        return paidToHospitalCancelledBill;
    }

    public void setPaidToHospitalCancelledBill(Bill paidToHospitalCancelledBill) {
        this.paidToHospitalCancelledBill = paidToHospitalCancelledBill;
    }

    public boolean isPaidToHospital() {
        return paidToHospital;
    }

    public void setPaidToHospital(boolean paidToHospital) {
        this.paidToHospital = paidToHospital;
    }

    public Date getPaidToHospitalDate() {
        return paidToHospitalDate;
    }

    public void setPaidToHospitalDate(Date paidToHospitalDate) {
        this.paidToHospitalDate = paidToHospitalDate;
    }

    public WebUser getPaidToHospitalProcessedBy() {
        return paidToHospitalProcessedBy;
    }

    public void setPaidToHospitalProcessedBy(WebUser paidToHospitalProcessedBy) {
        this.paidToHospitalProcessedBy = paidToHospitalProcessedBy;
    }

    public Bill getPaidToHospitalBill() {
        return paidToHospitalBill;
    }

    public void setPaidToHospitalBill(Bill paidToHospitalBill) {
        this.paidToHospitalBill = paidToHospitalBill;
    }

    public WebUser getPaidToHospitalBillCancelledBy() {
        return paidToHospitalBillCancelledBy;
    }

    public void setPaidToHospitalBillCancelledBy(WebUser paidToHospitalBillCancelledBy) {
        this.paidToHospitalBillCancelledBy = paidToHospitalBillCancelledBy;
    }

    public Date getPaidToHospitalBillCancelledAt() {
        return paidToHospitalBillCancelledAt;
    }

    public void setPaidToHospitalBillCancelledAt(Date paidToHospitalBillCancelledAt) {
        this.paidToHospitalBillCancelledAt = paidToHospitalBillCancelledAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public OnlineBookingStatus getOnlineBookingStatus() {
        return onlineBookingStatus;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public void setOnlineBookingStatus(OnlineBookingStatus onlineBookingStatus) {
        this.onlineBookingStatus = onlineBookingStatus;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public boolean isNeedSms() {
        return needSms;
    }

    public void setNeedSms(boolean needSms) {
        this.needSms = needSms;
    }

    public boolean isNsr() {
        return nsr;
    }

    public void setNsr(boolean nsr) {
        this.nsr = nsr;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public Date getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }

    public Institution getAgency() {
        return agency;
    }

    public void setAgency(Institution agency) {
        this.agency = agency;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getHospital() {
        return hospital;
    }

    public void setHospital(Institution hospital) {
        this.hospital = hospital;
    }

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

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public boolean isForeignStatus() {
        return foreignStatus;
    }

    public void setForeignStatus(boolean foreignStatus) {
        this.foreignStatus = foreignStatus;
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

    public boolean isAbsent() {
        return absent;
    }

    public void setAbsent(boolean absent) {
        this.absent = absent;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

}
