package com.divudi.core.data.dto.channel;

import com.divudi.core.data.BillCategory;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.util.Date;

/**
 * One channelling bill (or one payment-method portion of a multiple payment
 * bill) shown in the Cashier Shift End Collection Summary (issue #24248).
 * Cancellation and refund rows always carry negative amounts.
 */
public class ChannelShiftCollectionRowDTO {

    private Long billId;
    private String billNo;
    private BillTypeAtomic billTypeAtomic;
    private PaymentMethod paymentMethod;
    private Date appointmentDate;
    private Date createdAt;
    private String doctorName;
    private String patientName;
    private double doctorFee;
    private double hospitalFee;
    private double totalAmount;
    private boolean cancelled;
    private boolean refunded;
    private String agentName;
    private String agentCode;
    private String agentRefNo;
    private Integer appointmentNo;
    private String paymentRemark;

    public ChannelShiftCollectionRowDTO() {
    }

    // JPQL constructor used by ChannelService.fetchChannelShiftCollectionRows
    public ChannelShiftCollectionRowDTO(Long billId, String billNo, BillTypeAtomic billTypeAtomic, PaymentMethod paymentMethod,
            Date appointmentDate, Date createdAt, String doctorName, String patientName,
            Double doctorFee, Double hospitalFee, Double totalAmount, Boolean cancelled, Boolean refunded,
            String agentName, String agentCode, String agentRefNo, Integer appointmentNo) {
        this.billId = billId;
        this.billNo = billNo;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentMethod = paymentMethod;
        this.appointmentDate = appointmentDate;
        this.createdAt = createdAt;
        this.doctorName = doctorName;
        this.patientName = patientName != null ? patientName : "N/A";
        this.cancelled = cancelled != null && cancelled;
        this.refunded = refunded != null && refunded;
        this.agentName = agentName;
        this.agentCode = agentCode;
        this.agentRefNo = agentRefNo;
        this.appointmentNo = appointmentNo;
        double doc = doctorFee != null ? doctorFee : 0.0;
        double hos = hospitalFee != null ? hospitalFee : 0.0;
        double total = totalAmount != null ? totalAmount : 0.0;
        if (isReversal()) {
            doc = -Math.abs(doc);
            hos = -Math.abs(hos);
            total = -Math.abs(total);
        }
        this.doctorFee = doc;
        this.hospitalFee = hos;
        this.totalAmount = total;
    }

    /**
     * Copy of this row carrying only one payment-method portion of the bill.
     */
    public ChannelShiftCollectionRowDTO portion(PaymentMethod portionMethod, double portionDoctorFee, double portionHospitalFee, double portionTotal) {
        ChannelShiftCollectionRowDTO r = new ChannelShiftCollectionRowDTO();
        r.billId = billId;
        r.billNo = billNo;
        r.billTypeAtomic = billTypeAtomic;
        r.paymentMethod = portionMethod;
        r.appointmentDate = appointmentDate;
        r.createdAt = createdAt;
        r.doctorName = doctorName;
        r.patientName = patientName;
        r.cancelled = cancelled;
        r.refunded = refunded;
        r.agentName = agentName;
        r.agentCode = agentCode;
        r.agentRefNo = agentRefNo;
        r.appointmentNo = appointmentNo;
        r.doctorFee = portionDoctorFee;
        r.hospitalFee = portionHospitalFee;
        r.totalAmount = portionTotal;
        r.paymentRemark = (paymentRemark == null || paymentRemark.isEmpty())
                ? "Part of multiple payment"
                : paymentRemark + " (part of multiple payment)";
        return r;
    }

    public boolean isReversal() {
        if (billTypeAtomic == null) {
            return false;
        }
        return billTypeAtomic.getBillCategory() == BillCategory.CANCELLATION
                || billTypeAtomic.getBillCategory() == BillCategory.REFUND;
    }

    public String getStatus() {
        if (billTypeAtomic != null && billTypeAtomic.getBillCategory() == BillCategory.CANCELLATION) {
            return "Cancellation";
        }
        if (billTypeAtomic != null && billTypeAtomic.getBillCategory() == BillCategory.REFUND) {
            return "Refund";
        }
        if (cancelled) {
            return "Cancelled";
        }
        if (refunded) {
            return "Refunded";
        }
        return "Active";
    }

    public String getAgentNameAndCode() {
        if (agentName == null) {
            return "";
        }
        if (agentCode == null || agentCode.trim().isEmpty()) {
            return agentName;
        }
        return agentName + " - " + agentCode;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Date getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(Date appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public double getDoctorFee() {
        return doctorFee;
    }

    public void setDoctorFee(double doctorFee) {
        this.doctorFee = doctorFee;
    }

    public double getHospitalFee() {
        return hospitalFee;
    }

    public void setHospitalFee(double hospitalFee) {
        this.hospitalFee = hospitalFee;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
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

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getAgentRefNo() {
        return agentRefNo;
    }

    public void setAgentRefNo(String agentRefNo) {
        this.agentRefNo = agentRefNo;
    }

    public Integer getAppointmentNo() {
        return appointmentNo;
    }

    public void setAppointmentNo(Integer appointmentNo) {
        this.appointmentNo = appointmentNo;
    }

    public String getPaymentRemark() {
        return paymentRemark;
    }

    public void setPaymentRemark(String paymentRemark) {
        this.paymentRemark = paymentRemark;
    }
}
