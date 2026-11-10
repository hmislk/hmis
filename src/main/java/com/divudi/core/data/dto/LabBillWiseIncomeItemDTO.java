package com.divudi.core.data.dto;

import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * One bill item row of the Lab Bill-wise Income (A4) report, filled by a JPQL
 * constructor query.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class LabBillWiseIncomeItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private String billNo;
    private Date billCreatedAt;
    private Boolean billCancelled;
    private PaymentMethod paymentMethod;
    private Long patientEncounterId;
    private Long collectingCentreId;
    private Long billItemId;
    private String itemName;
    private Double grossValue;
    private Double discount;
    private Double netValue;
    private Boolean refunded;

    public LabBillWiseIncomeItemDTO() {
    }

    /**
     * Used by LabBillWiseIncomeReportController#processReport. Keep the
     * parameter order in step with that JPQL SELECT.
     */
    public LabBillWiseIncomeItemDTO(Long billId, String billNo, Date billCreatedAt,
            Boolean billCancelled, PaymentMethod paymentMethod,
            Long patientEncounterId, Long collectingCentreId,
            Long billItemId, String itemName,
            Double grossValue, Double discount, Double netValue,
            Boolean refunded) {
        this.billId = billId;
        this.billNo = billNo;
        this.billCreatedAt = billCreatedAt;
        this.billCancelled = billCancelled;
        this.paymentMethod = paymentMethod;
        this.patientEncounterId = patientEncounterId;
        this.collectingCentreId = collectingCentreId;
        this.billItemId = billItemId;
        this.itemName = itemName;
        this.grossValue = grossValue;
        this.discount = discount;
        this.netValue = netValue;
        this.refunded = refunded;
    }

    /**
     * Same rule as Bill#getIpOpOrCc().
     */
    public String getVisitType() {
        if (patientEncounterId != null) {
            return "IP";
        }
        if (collectingCentreId != null) {
            return "CC";
        }
        return "OP";
    }

    public boolean isCancelled() {
        return Boolean.TRUE.equals(billCancelled);
    }

    public boolean isItemRefunded() {
        return Boolean.TRUE.equals(refunded);
    }

    public double getGross() {
        return grossValue == null ? 0.0 : grossValue;
    }

    public double getDiscountValue() {
        return discount == null ? 0.0 : discount;
    }

    public double getNet() {
        return netValue == null ? 0.0 : netValue;
    }

    public String getPaymentMethodLabel() {
        return paymentMethod == null ? "" : paymentMethod.getLabel();
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

    public Date getBillCreatedAt() {
        return billCreatedAt;
    }

    public void setBillCreatedAt(Date billCreatedAt) {
        this.billCreatedAt = billCreatedAt;
    }

    public Boolean getBillCancelled() {
        return billCancelled;
    }

    public void setBillCancelled(Boolean billCancelled) {
        this.billCancelled = billCancelled;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getPatientEncounterId() {
        return patientEncounterId;
    }

    public void setPatientEncounterId(Long patientEncounterId) {
        this.patientEncounterId = patientEncounterId;
    }

    public Long getCollectingCentreId() {
        return collectingCentreId;
    }

    public void setCollectingCentreId(Long collectingCentreId) {
        this.collectingCentreId = collectingCentreId;
    }

    public Long getBillItemId() {
        return billItemId;
    }

    public void setBillItemId(Long billItemId) {
        this.billItemId = billItemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Double getGrossValue() {
        return grossValue;
    }

    public void setGrossValue(Double grossValue) {
        this.grossValue = grossValue;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getNetValue() {
        return netValue;
    }

    public void setNetValue(Double netValue) {
        this.netValue = netValue;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }
}
