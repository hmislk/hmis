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
    private String bhtNo;
    private String deptId;
    private BigDecimal serviceCharge;
    private String referenceBillNumber;

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

    // Constructor for the encounter-scoped Inward Service Bill list (issue #21247).
    // Deliberately omits billClass (enum) to avoid COALESCE(enum,'') projection
    // issues, and carries BHT number, printed bill number (deptId) and service
    // charge (margin) needed by the list. All parameter types map cleanly from
    // a JPQL NEW projection.
    // Numeric params are Double (not BigDecimal): Bill.total/discount/netTotal/margin
    // are primitive double on the entity, so a JPQL NEW projection over them yields
    // Double. Matching that here keeps EclipseLink's reflective constructor binding
    // unambiguous (a BigDecimal signature throws "argument type mismatch").
    public BillListReportDTO(Long billId, String billNumber,
                            BillTypeAtomic billTypeAtomicEnum, PaymentMethod paymentMethodEnum,
                            String patientName, Date createdAt,
                            String createdUserName, Boolean retired,
                            Boolean cancelled, Boolean refunded,
                            Double total, Double discount,
                            Double netTotal,
                            String bhtNo, String deptId, Double serviceCharge) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billTypeAtomic = billTypeAtomicEnum != null ? billTypeAtomicEnum.toString() : null;
        this.paymentMethod = paymentMethodEnum != null ? paymentMethodEnum.toString() : null;
        this.patientName = patientName;
        this.createdAt = createdAt;
        this.createdUserName = createdUserName;
        this.retired = retired;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.total = total != null ? BigDecimal.valueOf(total) : null;
        this.discount = discount != null ? BigDecimal.valueOf(discount) : null;
        this.netTotal = netTotal != null ? BigDecimal.valueOf(netTotal) : null;
        this.bhtNo = bhtNo;
        this.deptId = deptId;
        this.serviceCharge = serviceCharge != null ? BigDecimal.valueOf(serviceCharge) : null;
    }

    // Constructor for the encounter-scoped Inpatient Pharmacy Issue Returns list
    // (issue #21852). Adds referenceBillNumber (the original sale bill's printed
    // number, i.e. RefundBill.billedBill.deptId) on top of the #21247
    // encounter-scoped constructor above, without touching that existing
    // constructor's signature (per project rule: never modify existing
    // constructors, only add new ones).
    public BillListReportDTO(Long billId, String billNumber,
                            BillTypeAtomic billTypeAtomicEnum, PaymentMethod paymentMethodEnum,
                            String patientName, Date createdAt,
                            String createdUserName, Boolean retired,
                            Boolean cancelled, Boolean refunded,
                            Double total, Double discount,
                            Double netTotal,
                            String bhtNo, String deptId, Double serviceCharge,
                            String referenceBillNumber) {
        this(billId, billNumber, billTypeAtomicEnum, paymentMethodEnum, patientName, createdAt,
                createdUserName, retired, cancelled, refunded, total, discount, netTotal,
                bhtNo, deptId, serviceCharge);
        this.referenceBillNumber = referenceBillNumber;
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

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public BigDecimal getServiceCharge() {
        return serviceCharge;
    }

    public void setServiceCharge(BigDecimal serviceCharge) {
        this.serviceCharge = serviceCharge;
    }

    public String getReferenceBillNumber() {
        return referenceBillNumber;
    }

    public void setReferenceBillNumber(String referenceBillNumber) {
        this.referenceBillNumber = referenceBillNumber;
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
