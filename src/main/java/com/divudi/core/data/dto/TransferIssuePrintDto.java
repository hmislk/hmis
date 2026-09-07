/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Print DTO for a native transfer issue bill header.
 * Populated by TransferIssueNativeSqlService.settle() and enriched with
 * session/configuration data by TransferIssueNativeSqlController.
 *
 * Used by all DTO-based composite print components (transferIssueNativeA4, etc.)
 * instead of a JPA Bill entity so that no lazy-loading occurs during render.
 *
 * Related issue: #20583
 */
public class TransferIssuePrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- Bill identifiers ----
    /** bill.deptId of the issue bill. */
    private String issueNo;
    /** backwardReferenceBill.deptId (the request bill). */
    private String requestNo;

    // ---- Dates ----
    private Date issuedAt;
    private Date requestedAt;

    // ---- Parties ----
    /** Issuing department (the logged-in dept). */
    private String fromDepartmentName;
    /** Receiving department (the requesting dept). */
    private String toDepartmentName;
    private String issuedByName;
    private String issuedByStaffCode;
    /** The staff member being issued to (issuedBill.toStaff). */
    private String toStaffName;
    private String toStaffCode;

    private boolean cancelled;

    // ---- Institution details ----
    private String institutionName;
    private String institutionAddress;
    private String institutionPhone;
    private String institutionFax;
    private String institutionEmail;

    // ---- Issuing department details ----
    private String departmentPrintingName;
    private String departmentAddress;
    private String departmentPhone1;
    private String departmentPhone2;
    private String departmentFax;
    private String departmentEmail;

    // ---- Financial totals ----
    private double netTotal;
    private double totalPurchaseValue;
    private double totalRetailSaleValue;
    private double totalWholesaleValue;
    private double totalCostValue;

    // ---- Misc ----
    private String comments;

    // ---- Template / footer ----
    private String footerCss;
    private String footerText;

    // ---- Line items ----
    private List<TransferIssueItemPrintDto> items = new ArrayList<>();

    public TransferIssuePrintDto() {
    }

    public String getIssueNo() { return issueNo; }
    public void setIssueNo(String issueNo) { this.issueNo = issueNo; }

    public String getRequestNo() { return requestNo; }
    public void setRequestNo(String requestNo) { this.requestNo = requestNo; }

    public Date getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }

    public Date getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Date requestedAt) { this.requestedAt = requestedAt; }

    public String getFromDepartmentName() { return fromDepartmentName; }
    public void setFromDepartmentName(String fromDepartmentName) { this.fromDepartmentName = fromDepartmentName; }

    public String getToDepartmentName() { return toDepartmentName; }
    public void setToDepartmentName(String toDepartmentName) { this.toDepartmentName = toDepartmentName; }

    public String getIssuedByName() { return issuedByName; }
    public void setIssuedByName(String issuedByName) { this.issuedByName = issuedByName; }

    public String getIssuedByStaffCode() { return issuedByStaffCode; }
    public void setIssuedByStaffCode(String issuedByStaffCode) { this.issuedByStaffCode = issuedByStaffCode; }

    public String getToStaffName() { return toStaffName; }
    public void setToStaffName(String toStaffName) { this.toStaffName = toStaffName; }

    public String getToStaffCode() { return toStaffCode; }
    public void setToStaffCode(String toStaffCode) { this.toStaffCode = toStaffCode; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getInstitutionAddress() { return institutionAddress; }
    public void setInstitutionAddress(String institutionAddress) { this.institutionAddress = institutionAddress; }

    public String getInstitutionPhone() { return institutionPhone; }
    public void setInstitutionPhone(String institutionPhone) { this.institutionPhone = institutionPhone; }

    public String getInstitutionFax() { return institutionFax; }
    public void setInstitutionFax(String institutionFax) { this.institutionFax = institutionFax; }

    public String getInstitutionEmail() { return institutionEmail; }
    public void setInstitutionEmail(String institutionEmail) { this.institutionEmail = institutionEmail; }

    public String getDepartmentPrintingName() { return departmentPrintingName; }
    public void setDepartmentPrintingName(String departmentPrintingName) { this.departmentPrintingName = departmentPrintingName; }

    public String getDepartmentAddress() { return departmentAddress; }
    public void setDepartmentAddress(String departmentAddress) { this.departmentAddress = departmentAddress; }

    public String getDepartmentPhone1() { return departmentPhone1; }
    public void setDepartmentPhone1(String departmentPhone1) { this.departmentPhone1 = departmentPhone1; }

    public String getDepartmentPhone2() { return departmentPhone2; }
    public void setDepartmentPhone2(String departmentPhone2) { this.departmentPhone2 = departmentPhone2; }

    public String getDepartmentFax() { return departmentFax; }
    public void setDepartmentFax(String departmentFax) { this.departmentFax = departmentFax; }

    public String getDepartmentEmail() { return departmentEmail; }
    public void setDepartmentEmail(String departmentEmail) { this.departmentEmail = departmentEmail; }

    public double getNetTotal() { return netTotal; }
    public void setNetTotal(double netTotal) { this.netTotal = netTotal; }

    public double getTotalPurchaseValue() { return totalPurchaseValue; }
    public void setTotalPurchaseValue(double totalPurchaseValue) { this.totalPurchaseValue = totalPurchaseValue; }

    public double getTotalRetailSaleValue() { return totalRetailSaleValue; }
    public void setTotalRetailSaleValue(double totalRetailSaleValue) { this.totalRetailSaleValue = totalRetailSaleValue; }

    public double getTotalWholesaleValue() { return totalWholesaleValue; }
    public void setTotalWholesaleValue(double totalWholesaleValue) { this.totalWholesaleValue = totalWholesaleValue; }

    public double getTotalCostValue() { return totalCostValue; }
    public void setTotalCostValue(double totalCostValue) { this.totalCostValue = totalCostValue; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getFooterCss() { return footerCss; }
    public void setFooterCss(String footerCss) { this.footerCss = footerCss; }

    public String getFooterText() { return footerText; }
    public void setFooterText(String footerText) { this.footerText = footerText; }

    public List<TransferIssueItemPrintDto> getItems() { return items; }
    public void setItems(List<TransferIssueItemPrintDto> items) { this.items = items; }
}
