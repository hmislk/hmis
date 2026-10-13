package com.divudi.core.data.dto;

import com.divudi.core.data.clinical.CoveringLetterRecordType;
import java.io.Serializable;
import java.util.Date;

/**
 * One selectable record (admission or OPD credit bill) for a covering letter.
 */
public class CoveringLetterRowDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private CoveringLetterRecordType recordType;
    private String refNo;
    private Date date;
    private String patientName;
    private String policyNo;
    private Double amount;
    private String alreadyInLetterRef;
    /** The credit company's share of the amount (set after the query). */
    private Double companyAmount;

    /**
     * Used by JPQL constructor queries. The record type is set afterwards,
     * because EclipseLink passes an enum literal in a constructor expression
     * as its ordinal.
     */
    public CoveringLetterRowDTO(Long id, String refNo, Date date,
            String patientName, String policyNo, Double amount) {
        this.id = id;
        this.refNo = refNo;
        this.date = date;
        this.patientName = patientName;
        this.policyNo = policyNo;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CoveringLetterRecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(CoveringLetterRecordType recordType) {
        this.recordType = recordType;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getAlreadyInLetterRef() {
        return alreadyInLetterRef;
    }

    public void setAlreadyInLetterRef(String alreadyInLetterRef) {
        this.alreadyInLetterRef = alreadyInLetterRef;
    }

    public Double getCompanyAmount() {
        return companyAmount;
    }

    public void setCompanyAmount(Double companyAmount) {
        this.companyAmount = companyAmount;
    }
}
