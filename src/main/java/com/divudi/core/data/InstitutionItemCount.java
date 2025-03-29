package com.divudi.core.data;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class InstitutionItemCount {
    private Long institutionId;
    private String institutionName;
    private Long itemCount;
    private String institutionCode;

    public InstitutionItemCount() {
    }

    public InstitutionItemCount(Long institutionId, String institutionName, String institutionCode, Long itemCount) {
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.itemCount = itemCount;
        this.institutionCode = institutionCode;
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

    public Long getItemCount() {
        return itemCount;
    }

    public void setItemCount(Long itemCount) {
        this.itemCount = itemCount;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }




}
