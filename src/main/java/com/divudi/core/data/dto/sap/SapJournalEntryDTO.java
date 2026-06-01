/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.sap;

import java.util.List;
import java.util.Locale;

/**
 * SAP S/4HANA Cloud Journal Entry request payload.
 * Maps to POST /API_JOURNALENTRYITEMBASIC/JournalEntry OData entity.
 */
public class SapJournalEntryDTO {

    private String CompanyCode;
    private String DocumentDate;
    private String PostingDate;
    private String DocumentHeaderText;
    private String Reference;
    private List<SapJournalEntryItemDTO> to_JournalEntryItem;

    public static class SapJournalEntryItemDTO {
        private String GLAccount;
        private String AmountInTransactionCurrency;
        private String TransactionCurrency;
        private String DebitCreditCode;
        private String DocumentItemText;

        public SapJournalEntryItemDTO() {
        }

        public SapJournalEntryItemDTO(String glAccount, double amount, String currency,
                String debitCreditCode, String text) {
            this.GLAccount = glAccount;
            this.AmountInTransactionCurrency = String.format(Locale.ROOT, "%.2f", amount);
            this.TransactionCurrency = currency;
            this.DebitCreditCode = debitCreditCode;
            this.DocumentItemText = text;
        }

        public String getGLAccount() { return GLAccount; }
        public void setGLAccount(String gLAccount) { GLAccount = gLAccount; }
        public String getAmountInTransactionCurrency() { return AmountInTransactionCurrency; }
        public void setAmountInTransactionCurrency(String amount) { AmountInTransactionCurrency = amount; }
        public String getTransactionCurrency() { return TransactionCurrency; }
        public void setTransactionCurrency(String transactionCurrency) { TransactionCurrency = transactionCurrency; }
        public String getDebitCreditCode() { return DebitCreditCode; }
        public void setDebitCreditCode(String debitCreditCode) { DebitCreditCode = debitCreditCode; }
        public String getDocumentItemText() { return DocumentItemText; }
        public void setDocumentItemText(String documentItemText) { DocumentItemText = documentItemText; }
    }

    public SapJournalEntryDTO() {
    }

    public String getCompanyCode() { return CompanyCode; }
    public void setCompanyCode(String companyCode) { CompanyCode = companyCode; }
    public String getDocumentDate() { return DocumentDate; }
    public void setDocumentDate(String documentDate) { DocumentDate = documentDate; }
    public String getPostingDate() { return PostingDate; }
    public void setPostingDate(String postingDate) { PostingDate = postingDate; }
    public String getDocumentHeaderText() { return DocumentHeaderText; }
    public void setDocumentHeaderText(String documentHeaderText) { DocumentHeaderText = documentHeaderText; }
    public String getReference() { return Reference; }
    public void setReference(String reference) { Reference = reference; }
    public List<SapJournalEntryItemDTO> getTo_JournalEntryItem() { return to_JournalEntryItem; }
    public void setTo_JournalEntryItem(List<SapJournalEntryItemDTO> items) { to_JournalEntryItem = items; }
}
