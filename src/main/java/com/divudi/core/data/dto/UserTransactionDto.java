package com.divudi.core.data.dto;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import java.io.Serializable;
import java.util.Date;

/**
 * DTO for User Transaction History
 * This class provides a unified view of transactions across multiple entity types
 * (Bills, Payments, Patient Registrations, Investigations) for chronological display
 *
 * @author Claude Code
 */
public class UserTransactionDto implements Serializable {

    private String transactionType;  // "BILL", "PAYMENT", "PATIENT_REG", "INVESTIGATION"
    private Long transactionId;
    private String transactionNumber; // Bill number, Payment ref, Patient code, etc.
    private Date transactionDate;
    private String patientName;
    private String description;       // Bill type, Payment type, etc.
    private Double amount;
    private String status;
    private String additionalInfo;    // Payment method, investigation name, etc.

    // Default constructor
    public UserTransactionDto() {
    }

    // Constructor for Bill transactions - String types (9 params)
    public UserTransactionDto(String transactionType, Long billId,
                            String billNumber, Date createdAt,
                            String patientName, String billTypeAtomic,
                            Double netTotal, String paymentMethod,
                            boolean cancelled, boolean refunded) {
        this.transactionType = transactionType;
        this.transactionId = billId;
        this.transactionNumber = billNumber;
        this.transactionDate = createdAt;
        this.patientName = patientName;
        this.description = billTypeAtomic;
        this.amount = netTotal;
        this.additionalInfo = paymentMethod;

        if (cancelled) {
            this.status = "CANCELLED";
        } else if (refunded) {
            this.status = "REFUNDED";
        } else {
            this.status = "ACTIVE";
        }
    }

    // Constructor for Bill transactions - Enum types (10 params)
    // This constructor is used by JPQL queries that pass enums directly
    public UserTransactionDto(String transactionType, Long billId,
                            String billNumber, Date createdAt,
                            String patientName, BillTypeAtomic billTypeAtomic,
                            Double netTotal, PaymentMethod paymentMethod,
                            boolean cancelled, boolean refunded) {
        this.transactionType = transactionType;
        this.transactionId = billId;
        this.transactionNumber = billNumber;
        this.transactionDate = createdAt;
        this.patientName = patientName;
        this.description = billTypeAtomic != null ? billTypeAtomic.toString() : "";
        this.amount = netTotal;
        this.additionalInfo = paymentMethod != null ? paymentMethod.toString() : "";

        if (cancelled) {
            this.status = "CANCELLED";
        } else if (refunded) {
            this.status = "REFUNDED";
        } else {
            this.status = "ACTIVE";
        }
    }

    // Constructor for Payment transactions (8 params)
    public UserTransactionDto(String transactionType, Long paymentId,
                            String referenceNo, Date paymentDate,
                            String patientName, String paymentMethod,
                            Double paidValue, boolean retired) {
        this.transactionType = transactionType;
        this.transactionId = paymentId;
        this.transactionNumber = referenceNo;
        this.transactionDate = paymentDate;
        this.patientName = patientName;
        this.description = paymentMethod;
        this.amount = paidValue;
        this.status = retired ? "RETIRED" : "ACTIVE";
    }

    // Constructor for Patient registration (5 params)
    public UserTransactionDto(String transactionType, Long patientId,
                            String patientCode, Date createdAt,
                            String patientName) {
        this.transactionType = transactionType;
        this.transactionId = patientId;
        this.transactionNumber = patientCode;
        this.transactionDate = createdAt;
        this.patientName = patientName;
        this.description = "Patient Registration";
        this.status = "REGISTERED";
    }

    // Getters and Setters
    public String getTransactionType() {
        return transactionType != null ? transactionType : "";
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionNumber() {
        return transactionNumber != null ? transactionNumber : "";
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getPatientName() {
        return patientName != null ? patientName : "";
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getAmount() {
        return amount != null ? amount : 0.0;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status != null ? status : "";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdditionalInfo() {
        return additionalInfo != null ? additionalInfo : "";
    }

    public void setAdditionalInfo(String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    @Override
    public String toString() {
        return "UserTransactionDto{" +
                "transactionType='" + transactionType + '\'' +
                ", transactionNumber='" + transactionNumber + '\'' +
                ", transactionDate=" + transactionDate +
                ", patientName='" + patientName + '\'' +
                ", amount=" + amount +
                '}';
    }
}
