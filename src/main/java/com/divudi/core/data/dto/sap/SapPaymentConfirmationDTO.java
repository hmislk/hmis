/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.sap;

/**
 * Inbound SAP payment confirmation payload.
 * SAP Integration Suite / BTP posts this to HMIS when a payment is posted in SAP FI.
 */
public class SapPaymentConfirmationDTO {

    /** SAP FI accounting document number (e.g. "1800000012"). */
    private String sapDocumentNumber;

    /** HMIS bill reference — matches {@code Bill.deptId}. */
    private String hmsBillReference;

    /** Confirmed payment amount. */
    private double amount;

    /** Currency code (e.g. "LKR"). */
    private String currency;

    /** SAP posting date (yyyy-MM-dd). */
    private String postingDate;

    /** Optional free-text note from SAP. */
    private String note;

    public SapPaymentConfirmationDTO() {
    }

    public String getSapDocumentNumber() { return sapDocumentNumber; }
    public void setSapDocumentNumber(String sapDocumentNumber) { this.sapDocumentNumber = sapDocumentNumber; }
    public String getHmsBillReference() { return hmsBillReference; }
    public void setHmsBillReference(String hmsBillReference) { this.hmsBillReference = hmsBillReference; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getPostingDate() { return postingDate; }
    public void setPostingDate(String postingDate) { this.postingDate = postingDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
