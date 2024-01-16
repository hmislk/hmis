package com.divudi.data;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
public class InstitutionItemCount {
    private Long institutionId;
    private String institutionName;
    private Long itemCount;

    public InstitutionItemCount() {
    }

    public InstitutionItemCount(Long institutionId, String institutionName, Long itemCount) {
        this.institutionId = institutionId;
        this.institutionName = institutionName;
        this.itemCount = itemCount;
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
    
    
            
    
}
