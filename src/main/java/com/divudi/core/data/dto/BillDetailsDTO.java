package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BillDetailsDTO implements Serializable {

    private Long id;
    private String dtype;
    private Date createdAt;
    private Long fromDepartmentId;
    private Long toDepartmentId;
    private Long departmentId;
    private Long fromInstitutionId;
    private Long toInstitutionId;
    private Long institutionId;
    private String billTypeAtomic;
    private String billType;
    private Double discount;
    private Double tax;
    private Double expenseTotal;
    private Double netTotal;
    private Double total;
    private Long billFinanceDetailsId;

    // Bill Finance Details
    private BillFinanceDetailsDTO billFinanceDetails;

    // Bill Items
    private List<BillItemDetailsDTO> billItems;

    public BillDetailsDTO() {
        billItems = new ArrayList<>();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDtype() {
        return dtype;
    }

    public void setDtype(String dtype) {
        this.dtype = dtype;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Long getFromDepartmentId() {
        return fromDepartmentId;
    }

    public void setFromDepartmentId(Long fromDepartmentId) {
        this.fromDepartmentId = fromDepartmentId;
    }

    public Long getToDepartmentId() {
        return toDepartmentId;
    }

    public void setToDepartmentId(Long toDepartmentId) {
        this.toDepartmentId = toDepartmentId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getFromInstitutionId() {
        return fromInstitutionId;
    }

    public void setFromInstitutionId(Long fromInstitutionId) {
        this.fromInstitutionId = fromInstitutionId;
    }

    public Long getToInstitutionId() {
        return toInstitutionId;
    }

    public void setToInstitutionId(Long toInstitutionId) {
        this.toInstitutionId = toInstitutionId;
    }

    public Long getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Long institutionId) {
        this.institutionId = institutionId;
    }

    public String getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(String billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getTax() {
        return tax;
    }

    public void setTax(Double tax) {
        this.tax = tax;
    }

    public Double getExpenseTotal() {
        return expenseTotal;
    }

    public void setExpenseTotal(Double expenseTotal) {
        this.expenseTotal = expenseTotal;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Long getBillFinanceDetailsId() {
        return billFinanceDetailsId;
    }

    public void setBillFinanceDetailsId(Long billFinanceDetailsId) {
        this.billFinanceDetailsId = billFinanceDetailsId;
    }

    public BillFinanceDetailsDTO getBillFinanceDetails() {
        return billFinanceDetails;
    }

    public void setBillFinanceDetails(BillFinanceDetailsDTO billFinanceDetails) {
        this.billFinanceDetails = billFinanceDetails;
    }

    public List<BillItemDetailsDTO> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItemDetailsDTO> billItems) {
        this.billItems = billItems;
    }
}
