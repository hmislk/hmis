package com.divudi.core.data.dto;

import com.divudi.core.data.BillFinanceType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * One money movement on the Combined Inward Payments report: a deposit, a
 * payment, a post-final-bill payment, or a refund of any of those.
 *
 * The amount is signed by the direction the money moves, taken from the bill
 * type's own {@link BillFinanceType} rather than from the stored net total.
 * Cash-in types (deposits, payments, post-final payments) are positive and
 * cash-out types (refunds) are negative, so a column total is the true net for
 * the period. The sign is derived from the type because the stored sign on a
 * reversal bill is not consistent across the bill types gathered here.
 */
public class InwardCombinedPaymentRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long billId;
    private String billNo;
    private Date createdAt;
    private BillTypeAtomic billTypeAtomic;
    private String bhtNo;
    private String patientName;
    private PaymentMethod paymentMethod;
    private Double netTotal;
    private boolean cancelled;

    /**
     * Which group this row falls into for the selected view - the bill type
     * label, or the BHT number. Set by the controller after fetching, not by
     * the query.
     */
    private String groupKey;

    public InwardCombinedPaymentRowDto() {
    }

    /**
     * Constructor used by the report's JPQL constructor query. Do not change
     * this signature - add a new constructor instead if another shape is
     * needed, or the query breaks.
     */
    public InwardCombinedPaymentRowDto(Long billId,
            String billNo,
            Date createdAt,
            BillTypeAtomic billTypeAtomic,
            String bhtNo,
            String patientName,
            PaymentMethod paymentMethod,
            Double netTotal,
            boolean cancelled) {
        this.billId = billId;
        this.billNo = billNo;
        this.createdAt = createdAt;
        this.billTypeAtomic = billTypeAtomic;
        this.bhtNo = bhtNo;
        this.patientName = patientName;
        this.paymentMethod = paymentMethod;
        this.netTotal = netTotal;
        this.cancelled = cancelled;
    }

    /**
     * The bill type as printed on the report, e.g. "Inward Deposit".
     */
    public String getTypeLabel() {
        return billTypeAtomic != null ? billTypeAtomic.getLabel() : "";
    }

    /**
     * Signed amount: positive for money coming in, negative for a refund going
     * back out. A cancelled bill contributes nothing.
     */
    public double getSignedAmount() {
        if (cancelled || netTotal == null || billTypeAtomic == null) {
            return 0.0;
        }
        double magnitude = Math.abs(netTotal);
        return billTypeAtomic.getBillFinanceType() == BillFinanceType.CASH_OUT
                ? -magnitude : magnitude;
    }

    /**
     * True when this row is a refund, so the page can style it.
     */
    public boolean isRefund() {
        return billTypeAtomic != null
                && billTypeAtomic.getBillFinanceType() == BillFinanceType.CASH_OUT;
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

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
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

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
