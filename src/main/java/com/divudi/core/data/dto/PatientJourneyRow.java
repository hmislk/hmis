package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

public class PatientJourneyRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Date transactionDate;
    private String visitNo;
    private String transactionName;
    private String transactionNumber;
    private String description;
    private String userName;

    public PatientJourneyRow() {
    }

    public PatientJourneyRow(Date transactionDate,
                             String visitNo,
                             String transactionName,
                             String transactionNumber,
                             String description,
                             String userName) {
        this.transactionDate = transactionDate;
        this.visitNo = visitNo;
        this.transactionName = transactionName;
        this.transactionNumber = transactionNumber;
        this.description = description;
        this.userName = userName;
    }

    public PatientJourneyRow(Date transactionDate,
                             String visitNo,
                             Object transactionTypeAtomic,
                             Object transactionType,
                             String transactionNumber,
                             String description,
                             String altDescription,
                             String userName) {
        this.transactionDate = transactionDate;
        this.visitNo = visitNo;
        this.transactionNumber = transactionNumber;
        this.transactionName = humanizeEnumName(firstNonEmpty(
                transactionTypeAtomic != null ? transactionTypeAtomic.toString() : null,
                transactionType != null ? transactionType.toString() : null));
        this.description = firstNonEmpty(description, altDescription);
        this.userName = userName != null ? userName : "";
    }

    private static String humanizeEnumName(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String[] parts = value.trim().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(part.substring(0, 1)).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getVisitNo() {
        return visitNo;
    }

    public void setVisitNo(String visitNo) {
        this.visitNo = visitNo;
    }

    public String getTransactionName() {
        return transactionName;
    }

    public void setTransactionName(String transactionName) {
        this.transactionName = transactionName;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
