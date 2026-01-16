package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for Patient Deposit History reports.
 * Used for optimized queries without loading full entity graphs.
 */
public class PatientDepositHistoryDto implements Serializable {

    private Long id;
    private Long billId;
    private String billNumber;
    private BillTypeAtomic billTypeAtomic;
    private Date createdAt;
    private HistoryType historyType;

    // Patient info
    private Long patientId;
    private String patientName;
    private String patientPhn;

    // Balance info
    private Double balanceBeforeTransaction;
    private Double balanceAfterTransaction;
    private Double transactionValue;

    // Location info
    private Long departmentId;
    private String departmentName;
    private Long siteId;
    private String siteName;
    private Long institutionId;
    private String institutionName;

    // Aggregate balances (this patient)
    private Double patientDepositBalanceForSite;
    private Double patientDepositBalanceForInstitution;
    private Double patientDepositBalanceForAllInstitutions;

    // Aggregate balances (all patients)
    private Double allPatientsDepositBalanceForDepartment;
    private Double allPatientsDepositBalanceForSite;
    private Double allPatientsDepositBalanceForInstitution;
    private Double allPatientsDepositBalanceForAllInstitutions;

    // Bill status
    private boolean cancelled;
    private boolean refunded;

    // Creator info
    private String createrName;

    // Display-specific fields for list views
    private String paymentMethodLabel;
    private String billDeptId;
    private String billTypeAtomicLabel;

    public PatientDepositHistoryDto() {
    }

    // Constructor for JPQL projection - basic fields
    public PatientDepositHistoryDto(Long id, Long billId, String billNumber,
            BillTypeAtomic billTypeAtomic, Date createdAt, HistoryType historyType,
            Long patientId, String patientName, String patientPhn,
            Double balanceBeforeTransaction, Double balanceAfterTransaction, Double transactionValue,
            Long departmentId, String departmentName,
            Long siteId, String siteName,
            Long institutionId, String institutionName,
            boolean cancelled, boolean refunded, String createrName) {
        this.id = id;
        this.billId = billId;
        this.billNumber = billNumber;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.historyType = historyType;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.balanceBeforeTransaction = balanceBeforeTransaction;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.transactionValue = transactionValue;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.createrName = createrName;
    }

    // Constructor with aggregate balances
    public PatientDepositHistoryDto(Long id, Long billId, String billNumber,
            BillTypeAtomic billTypeAtomic, Date createdAt, HistoryType historyType,
            Long patientId, String patientName, String patientPhn,
            Double balanceBeforeTransaction, Double balanceAfterTransaction, Double transactionValue,
            Long departmentId, String departmentName,
            Long siteId, String siteName,
            Long institutionId, String institutionName,
            Double patientDepositBalanceForSite, Double patientDepositBalanceForInstitution,
            Double patientDepositBalanceForAllInstitutions,
            Double allPatientsDepositBalanceForDepartment, Double allPatientsDepositBalanceForSite,
            Double allPatientsDepositBalanceForInstitution, Double allPatientsDepositBalanceForAllInstitutions,
            boolean cancelled, boolean refunded, String createrName) {
        this.id = id;
        this.billId = billId;
        this.billNumber = billNumber;
        this.billTypeAtomic = billTypeAtomic;
        this.createdAt = createdAt;
        this.historyType = historyType;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientPhn = patientPhn;
        this.balanceBeforeTransaction = balanceBeforeTransaction;
        this.balanceAfterTransaction = balanceAfterTransaction;
        this.transactionValue = transactionValue;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.siteId = siteId;
        this.siteName = siteName;
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.patientDepositBalanceForSite = patientDepositBalanceForSite;
        this.patientDepositBalanceForInstitution = patientDepositBalanceForInstitution;
        this.patientDepositBalanceForAllInstitutions = patientDepositBalanceForAllInstitutions;
        this.allPatientsDepositBalanceForDepartment = allPatientsDepositBalanceForDepartment;
        this.allPatientsDepositBalanceForSite = allPatientsDepositBalanceForSite;
        this.allPatientsDepositBalanceForInstitution = allPatientsDepositBalanceForInstitution;
        this.allPatientsDepositBalanceForAllInstitutions = allPatientsDepositBalanceForAllInstitutions;
        this.cancelled = cancelled;
        this.refunded = refunded;
        this.createrName = createrName;
    }

    // Constructor for latest transactions list display (minimal fields)
    public PatientDepositHistoryDto(Long id, Date createdAt, Double transactionValue,
            HistoryType historyType, String paymentMethodLabel,
            String billTypeAtomicLabel, String billDeptId) {
        this.id = id;
        this.createdAt = createdAt;
        this.transactionValue = transactionValue;
        this.historyType = historyType;
        this.paymentMethodLabel = paymentMethodLabel;
        this.billTypeAtomicLabel = billTypeAtomicLabel;
        this.billDeptId = billDeptId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public HistoryType getHistoryType() {
        return historyType;
    }

    public void setHistoryType(HistoryType historyType) {
        this.historyType = historyType;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhn() {
        return patientPhn;
    }

    public void setPatientPhn(String patientPhn) {
        this.patientPhn = patientPhn;
    }

    public Double getBalanceBeforeTransaction() {
        return balanceBeforeTransaction;
    }

    public void setBalanceBeforeTransaction(Double balanceBeforeTransaction) {
        this.balanceBeforeTransaction = balanceBeforeTransaction;
    }

    public Double getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(Double balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public Double getTransactionValue() {
        return transactionValue;
    }

    public void setTransactionValue(Double transactionValue) {
        this.transactionValue = transactionValue;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getSiteId() {
        return siteId;
    }

    public void setSiteId(Long siteId) {
        this.siteId = siteId;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public Double getPatientDepositBalanceForSite() {
        return patientDepositBalanceForSite;
    }

    public void setPatientDepositBalanceForSite(Double patientDepositBalanceForSite) {
        this.patientDepositBalanceForSite = patientDepositBalanceForSite;
    }

    public Double getPatientDepositBalanceForInstitution() {
        return patientDepositBalanceForInstitution;
    }

    public void setPatientDepositBalanceForInstitution(Double patientDepositBalanceForInstitution) {
        this.patientDepositBalanceForInstitution = patientDepositBalanceForInstitution;
    }

    public Double getPatientDepositBalanceForAllInstitutions() {
        return patientDepositBalanceForAllInstitutions;
    }

    public void setPatientDepositBalanceForAllInstitutions(Double patientDepositBalanceForAllInstitutions) {
        this.patientDepositBalanceForAllInstitutions = patientDepositBalanceForAllInstitutions;
    }

    public Double getAllPatientsDepositBalanceForDepartment() {
        return allPatientsDepositBalanceForDepartment;
    }

    public void setAllPatientsDepositBalanceForDepartment(Double allPatientsDepositBalanceForDepartment) {
        this.allPatientsDepositBalanceForDepartment = allPatientsDepositBalanceForDepartment;
    }

    public Double getAllPatientsDepositBalanceForSite() {
        return allPatientsDepositBalanceForSite;
    }

    public void setAllPatientsDepositBalanceForSite(Double allPatientsDepositBalanceForSite) {
        this.allPatientsDepositBalanceForSite = allPatientsDepositBalanceForSite;
    }

    public Double getAllPatientsDepositBalanceForInstitution() {
        return allPatientsDepositBalanceForInstitution;
    }

    public void setAllPatientsDepositBalanceForInstitution(Double allPatientsDepositBalanceForInstitution) {
        this.allPatientsDepositBalanceForInstitution = allPatientsDepositBalanceForInstitution;
    }

    public Double getAllPatientsDepositBalanceForAllInstitutions() {
        return allPatientsDepositBalanceForAllInstitutions;
    }

    public void setAllPatientsDepositBalanceForAllInstitutions(Double allPatientsDepositBalanceForAllInstitutions) {
        this.allPatientsDepositBalanceForAllInstitutions = allPatientsDepositBalanceForAllInstitutions;
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

    public String getCreaterName() {
        return createrName;
    }

    public void setCreaterName(String createrName) {
        this.createrName = createrName;
    }

    public String getPaymentMethodLabel() {
        return paymentMethodLabel;
    }

    public void setPaymentMethodLabel(String paymentMethodLabel) {
        this.paymentMethodLabel = paymentMethodLabel;
    }

    public String getBillDeptId() {
        return billDeptId;
    }

    public void setBillDeptId(String billDeptId) {
        this.billDeptId = billDeptId;
    }

    public String getBillTypeAtomicLabel() {
        return billTypeAtomicLabel;
    }

    public void setBillTypeAtomicLabel(String billTypeAtomicLabel) {
        this.billTypeAtomicLabel = billTypeAtomicLabel;
    }
}
