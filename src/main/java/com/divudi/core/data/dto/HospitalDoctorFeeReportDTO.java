package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;

import java.io.Serializable;
import java.util.Date;

public class HospitalDoctorFeeReportDTO implements Serializable {
    private Long billId;
    private String patientName;
    private String doctorName;
    private Double hospitalFee;
    private Double doctorFee;
    private Double netTotal;
    private Date billDate;
    private PaymentMethod paymentMethod;
    private BillTypeAtomic billTypeAtomic;

    public HospitalDoctorFeeReportDTO() {
    }

    // Constructor for JPQL
    public HospitalDoctorFeeReportDTO(Long billId, String patientName, String doctorName,
                                     Double hospitalFee, Double doctorFee, Double netTotal,
                                     Date billDate, PaymentMethod paymentMethod, BillTypeAtomic billTypeAtomic) {
        this.billId = billId;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.hospitalFee = hospitalFee;
        this.doctorFee = doctorFee;
        this.netTotal = netTotal;
        this.billDate = billDate;
        this.paymentMethod = paymentMethod;
        this.billTypeAtomic = billTypeAtomic;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(Double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public Double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(Double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }
}