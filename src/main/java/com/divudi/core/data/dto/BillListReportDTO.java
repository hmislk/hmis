package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for All Bill List Report optimization
 * This class represents only the essential display fields needed for the report
 * to improve performance by avoiding entity relationship loading
 *
 * @author Claude Code
 */
public class BillListReportDTO implements Serializable {

    private Long billId;
    private String billNumber;
    private String billClass;
    private String billTypeAtomic;
    private String paymentMethod;
    private String patientName;
    private Date createdAt;
    private String createdUserName;
    private Boolean retired;
    private Boolean cancelled;
    private Boolean refunded;
    private BigDecimal total;
    private BigDecimal discount;
    private BigDecimal netTotal;

    // Default constructor
    public BillListReportDTO() {
    }

    // Original constructor for string-based parameters (preserved for backward compatibility)
    public BillListReportDTO(Long billId, String billNumber, String billClass,
                            String billTypeAtomic, String paymentMethod,
                            String patientName, Date createdAt,
                            String createdUserName, Boolean retired,
                            Boolean cancelled, Boolean refunded,
                            BigDecimal total, BigDecimal discount,
                            BigDecimal netTotal) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billClass = billClass;
        this.billTypeAtomic = billTypeAtomic;
        this.paymentMethod = paymentMethod;
        this.patientName = patientName;
        this.createdAt = createdAt;
        this.createdUserName = createdUserName;
        this.retired = retired;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
    }

    // Enhanced constructor accepting enums for JPQL DTO projection queries
    // This constructor accepts BillTypeAtomic and PaymentMethod enums and converts them to strings
    public BillListReportDTO(Long billId, String billNumber, String billClass,
                            BillTypeAtomic billTypeAtomicEnum, PaymentMethod paymentMethodEnum,
                            String patientName, Date createdAt,
                            String createdUserName, Boolean retired,
                            Boolean cancelled, Boolean refunded,
                            BigDecimal total, BigDecimal discount,
                            BigDecimal netTotal) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billClass = billClass;
        // Convert enums to string representations (null-safe)
        this.billTypeAtomic = billTypeAtomicEnum != null ? billTypeAtomicEnum.toString() : null;
        this.paymentMethod = paymentMethodEnum != null ? paymentMethodEnum.toString() : null;
        this.patientName = patientName;
        this.createdAt = createdAt;
        this.createdUserName = createdUserName;
        this.retired = retired;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.total = total;
        this.discount = discount;
        this.netTotal = netTotal;
    }

    // Getters and Setters
    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getBillClass() {
        return billClass;
    }

    public void setBillClass(String billClass) {
        this.billClass = billClass;
    }

    public String getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(String billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedUserName() {
        return createdUserName;
    }

    public void setCreatedUserName(String createdUserName) {
        this.createdUserName = createdUserName;
    }

    public Boolean getRetired() {
        return retired;
    }

    public void setRetired(Boolean retired) {
        this.retired = retired;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    @Override
    public String toString() {
        return "BillListReportDTO{" +
                "billId=" + billId +
                ", billNumber='" + billNumber + '\'' +
                ", billClass='" + billClass + '\'' +
                ", billTypeAtomic='" + billTypeAtomic + '\'' +
                ", patientName='" + patientName + '\'' +
                ", total=" + total +
                '}';
    }
}
