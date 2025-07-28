package com.divudi.core.data.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * DTO for Laboratory Income Report optimization
 */
public class LabIncomeReportDTO implements Serializable {

    private Long billId;
    private String billNumber;
    private Date billDate;
    private String departmentName;
    private String institutionName;
    private BigDecimal netTotal;
    private BigDecimal total;

    public LabIncomeReportDTO() {
    }

    public LabIncomeReportDTO(Long billId, String billNumber, Date billDate,
                              String departmentName, String institutionName,
                              BigDecimal netTotal, BigDecimal total) {
        this.billId = billId;
        this.billNumber = billNumber;
        this.billDate = billDate;
        this.departmentName = departmentName;
        this.institutionName = institutionName;
        this.netTotal = netTotal;
        this.total = total;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public BigDecimal getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(BigDecimal netTotal) {
        this.netTotal = netTotal;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
