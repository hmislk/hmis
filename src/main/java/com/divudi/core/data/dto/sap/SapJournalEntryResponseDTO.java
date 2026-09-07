/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.sap;

/**
 * SAP S/4HANA Cloud Journal Entry creation response (success path).
 */
public class SapJournalEntryResponseDTO {

    private String companyCode;
    private String fiscalYear;
    private String accountingDocument;

    public SapJournalEntryResponseDTO() {
    }

    public String getCompanyCode() { return companyCode; }
    public void setCompanyCode(String companyCode) { this.companyCode = companyCode; }
    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }
    public String getAccountingDocument() { return accountingDocument; }
    public void setAccountingDocument(String accountingDocument) { this.accountingDocument = accountingDocument; }
}
