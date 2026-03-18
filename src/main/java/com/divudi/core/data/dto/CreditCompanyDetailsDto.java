package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class CreditCompanyDetailsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long companyId;
    private String companyname;
    private String policyNo;
    private String referenceNo;
    
    public CreditCompanyDetailsDto() {
    }

    // Display the Previous Credit Company in OPD Billing 
    public CreditCompanyDetailsDto(Long companyId, String companyname, String policyNo, String referenceNo) {
        this.companyId = companyId;
        this.companyname = companyname;
        this.policyNo = policyNo;
        this.referenceNo = referenceNo;
    }

    public String getPolicyNo() {
        return policyNo;
    }

    public void setPolicyNo(String policyNo) {
        this.policyNo = policyNo;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyname() {
        return companyname;
    }

    public void setCompanyname(String companyname) {
        this.companyname = companyname;
    }

    @Override
    public String toString() {
        return "InstitutionDto{id=" + companyId + ", name='" + companyname + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditCompanyDetailsDto that = (CreditCompanyDetailsDto) o;
        return companyId != null && companyId.equals(that.companyId);
    }

    @Override
    public int hashCode() {
        return companyId != null ? companyId.hashCode() : 0;
    }

}
