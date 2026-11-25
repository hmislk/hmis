package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * One row of the OPD Quotation search list (#25885).
 */
public class OpdQuotationRowDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String quotationNo;
    private final String customerName;
    private final String customerPhone;
    private final Date createdAt;
    private final Date validUntil;
    private final double netTotal;
    private final String createdBy;

    public OpdQuotationRowDto(Long id, String quotationNo, String customerName, String customerPhone,
            Date createdAt, Date validUntil, double netTotal, String createdBy) {
        this.id = id;
        this.quotationNo = quotationNo;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.createdAt = createdAt;
        this.validUntil = validUntil;
        this.netTotal = netTotal;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public String getQuotationNo() {
        return quotationNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getValidUntil() {
        return validUntil;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
