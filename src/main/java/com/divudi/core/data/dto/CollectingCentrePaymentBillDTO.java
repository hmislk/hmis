package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * DTO for the Collecting Centre Payment Bill search table
 * (collecting_centre_repayment_bill_search.xhtml). Carries only the fields
 * needed for that table, avoiding full Bill entity/relationship loading.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
public class CollectingCentrePaymentBillDTO implements Serializable {

    private Long id;
    private String deptId;
    private Date fromDate;
    private Date toDate;
    private Date createdAt;
    private Boolean cancelled;
    private Date cancelledAt;
    private String createdByName;
    private String cancelledByName;
    private String toInstitutionCode;
    private String toInstitutionName;
    private Double netTotal;

    public CollectingCentrePaymentBillDTO() {
    }

    public CollectingCentrePaymentBillDTO(Long id, String deptId, Date fromDate, Date toDate,
            Date createdAt, Boolean cancelled, Date cancelledAt,
            String createdByName, String cancelledByName,
            String toInstitutionCode, String toInstitutionName, Double netTotal) {
        this.id = id;
        this.deptId = deptId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.createdAt = createdAt;
        this.cancelled = cancelled;
        this.cancelledAt = cancelledAt;
        this.createdByName = createdByName;
        this.cancelledByName = cancelledByName;
        this.toInstitutionCode = toInstitutionCode;
        this.toInstitutionName = toInstitutionName;
        this.netTotal = netTotal;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCancelledByName() {
        return cancelledByName;
    }

    public void setCancelledByName(String cancelledByName) {
        this.cancelledByName = cancelledByName;
    }

    public String getToInstitutionCode() {
        return toInstitutionCode;
    }

    public void setToInstitutionCode(String toInstitutionCode) {
        this.toInstitutionCode = toInstitutionCode;
    }

    public String getToInstitutionName() {
        return toInstitutionName;
    }

    public void setToInstitutionName(String toInstitutionName) {
        this.toInstitutionName = toInstitutionName;
    }

    public Double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(Double netTotal) {
        this.netTotal = netTotal;
    }

}
