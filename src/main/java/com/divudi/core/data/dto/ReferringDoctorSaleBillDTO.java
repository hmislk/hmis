package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * One row per pharmacy retail sale / cancellation / return bill, with the
 * referring doctor traced back to the original sale. The five doctor id
 * arguments of the JPQL constructor are candidates in priority order; the
 * first non-null one becomes doctorId.
 */
public class ReferringDoctorSaleBillDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String NO_DOCTOR = "No referring doctor";

    private Long billId;
    private String billNo;
    private Date createdAt;
    private BillTypeAtomic billTypeAtomic;
    private String patientName;
    private PaymentMethod paymentMethod;
    private String departmentName;
    private Double grossTotal;
    private Double discount;
    private Double netTotal;
    private Long doctorId;
    private String doctorName = NO_DOCTOR;
    private String rowType;

    public ReferringDoctorSaleBillDTO() {
    }

    public ReferringDoctorSaleBillDTO(Long billId, String billNo, Date createdAt,
            BillTypeAtomic billTypeAtomic, String patientName, PaymentMethod paymentMethod,
            String departmentName, Double grossTotal, Double discount, Double netTotal,
            Long doctorId1, Long doctorId2, Long doctorId3, Long doctorId4, Long doctorId5) {
        this.billId = billId;
        this.billNo = billNo;
        this.createdAt = createdAt;
        this.billTypeAtomic = billTypeAtomic;
        this.patientName = patientName;
        this.paymentMethod = paymentMethod;
        this.departmentName = departmentName;
        this.grossTotal = grossTotal;
        this.discount = discount;
        this.netTotal = netTotal;
        this.doctorId = firstNonNull(doctorId1, doctorId2, doctorId3, doctorId4, doctorId5);
    }

    static Long firstNonNull(Long... ids) {
        for (Long id : ids) {
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    public String getBillTypeLabel() {
        return billTypeAtomic == null ? null : billTypeAtomic.getLabel();
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(Double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getRowType() {
        return rowType;
    }

    public void setRowType(String rowType) {
        this.rowType = rowType;
    }
}
