package com.divudi.core.data.dto;

import java.io.Serializable;

/**
 * DTO for listing bank branches safely without lazy-loading risks.
 *
 * @author Dr M H B Ariyaratne
 */
public class BankBranchDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String institutionCode;
    private String bankName;

    public BankBranchDto() {
    }

    public BankBranchDto(Long id, String name, String institutionCode, String bankName) {
        this.id = id;
        this.name = name;
        this.institutionCode = institutionCode;
        this.bankName = bankName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public void setInstitutionCode(String institutionCode) {
        this.institutionCode = institutionCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getDisplayLabel() {
        if (bankName != null && !bankName.trim().isEmpty()) {
            return name + " - " + bankName;
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BankBranchDto that = (BankBranchDto) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BankBranchDto{id=" + id + ", name='" + name + "'}";
    }
}
