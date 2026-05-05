package com.divudi.core.data.dto.channel;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;

public class ChannelAbsentPatientsDTO {
    private Long id;
    private String deptId;
    private BillTypeAtomic billTypeAtomic;

    private String serviceSessionName;
    private int billSessionSerialNo;

    private String patientName;
    private String cashierName;
    private PaymentMethod paymentMethod;
    private String doctorName;

    private double staffFee;
    private double hospitalFee;
    private double netTotal;

    private boolean cancelled;
    private boolean refunded;
    private String cancelledDeptId;
    private String refundedDeptId;
    private Long referenceId;
    private Long paidId;


    public ChannelAbsentPatientsDTO(){
        this.staffFee = 0.0;
        this.hospitalFee = 0.0;
        this.netTotal = 0.0;
    }

    public ChannelAbsentPatientsDTO(
            Long id, String deptId, BillTypeAtomic billTypeAtomic, String serviceSessionName, int billSessionSerialNo, String patientName, String cashierName,
            PaymentMethod paymentMethod, String doctorName, double staffFee, double hospitalFee, double netTotal, boolean cancelled, boolean refunded, String cancelledDeptId, String refundedDeptId, Long referenceId, Long paidId) {
        
        this.id = id;
        this.deptId = deptId;
        this.billTypeAtomic = billTypeAtomic;
        this.serviceSessionName = serviceSessionName;
        this.billSessionSerialNo = billSessionSerialNo;
        this.patientName = patientName;
        this.cashierName = cashierName;
        this.paymentMethod = paymentMethod;
        this.doctorName = doctorName;
        this.staffFee = staffFee;
        this.hospitalFee = hospitalFee;
        this.netTotal = netTotal;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.cancelledDeptId = cancelledDeptId;
        this.refundedDeptId = refundedDeptId;
        this.referenceId = referenceId;
        this.paidId = paidId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getServiceSessionName() {
        return serviceSessionName;
    }

    public void setServiceSessionName(String serviceSessionName) {
        this.serviceSessionName = serviceSessionName;
    }

    public int getBillSessionSerialNo() {
        return billSessionSerialNo;
    }

    public void setBillSessionSerialNo(int billSessionSerialNo) {
        this.billSessionSerialNo = billSessionSerialNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public double getStaffFee() {
        return staffFee;
    }

    public void setStaffFee(double staffFee) {
        this.staffFee = staffFee;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isRefunded() {
        return refunded;
    }

    public void setRefunded(boolean refunded) {
        this.refunded = refunded;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public Long getPaidId() {
        return paidId;
    }

    public void setPaidId(Long paidId) {
        this.paidId = paidId;
    }

    public String getCancelledDeptId() {
        return cancelledDeptId;
    }

    public void setCancelledDeptId(String cancelledDeptId) {
        this.cancelledDeptId = cancelledDeptId;
    }

    public String getRefundedDeptId() {
        return refundedDeptId;
    }

    public void setRefundedDeptId(String refundedDeptId) {
        this.refundedDeptId = refundedDeptId;
    }
}

