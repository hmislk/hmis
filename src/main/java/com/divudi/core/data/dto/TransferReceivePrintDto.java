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
 * Print DTO for a transfer receive bill header, populated by
 * TransferReceiveNativeSqlService.settle() and enriched with
 * session/configuration data by TransferReceiveNativeSqlController.
 *
 * Used by all DTO-based composite print components (transferReceiveNativeA4, etc.)
 * instead of a JPA Bill entity so that no lazy-loading occurs during render.
 */
public class TransferReceivePrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- Bill identifiers ----
    /** bill.deptId of the receive bill. */
    private String receiveNo;
    /** backwardReferenceBill.deptId (the issued bill). */
    private String issueNo;

    // ---- Dates ----
    private Date receivedAt;
    /** backwardReferenceBill.createdAt */
    private Date issuedAt;

    // ---- Parties ----
    private String fromDepartmentName;
    private String toDepartmentName;
    private String receivedByName;
    private String receivedByStaffCode;
    private String issuedByName;
    private String issuedByStaffCode;
    /** The staff member who transported/delivered the goods (issuedBill.toStaff). */
    private String transporterName;
    private boolean cancelled;

    // ---- Institution details ----
    private String institutionName;
    private String institutionAddress;
    /** Combined phone1 / phone2. */
    private String institutionPhone;
    private String institutionFax;
    private String institutionEmail;

    // ---- Receiving department details ----
    private String departmentPrintingName;
    private String departmentAddress;
    private String departmentPhone1;
    private String departmentPhone2;
    private String departmentFax;
    private String departmentEmail;

    // ---- Financial totals (absolute values) ----
    private double netTotal;
    private double totalPurchaseValue;
    private double totalRetailSaleValue;
    private double totalWholesaleValue;
    private double totalCostValue;
    private double issuedNetTotal;

    // ---- Approval ----
    private String approvedByName;
    private Date approvedAt;

    // ---- Misc ----
    private String comments;

    // ---- Template / footer (from ConfigOptionApplication) ----
    private String footerCss;
    private String footerText;
    /** Pre-filled value from 'Transfer Receive Note Header' config key. */
    private String templateHeader;

    // ---- Line items ----
    private List<TransferReceiveItemPrintDto> items = new ArrayList<>();

    public TransferReceivePrintDto() {
    }

    public String getReceiveNo() {
        return receiveNo;
    }

    public void setReceiveNo(String receiveNo) {
        this.receiveNo = receiveNo;
    }

    public String getIssueNo() {
        return issueNo;
    }

    public void setIssueNo(String issueNo) {
        this.issueNo = issueNo;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getFromDepartmentName() {
        return fromDepartmentName;
    }

    public void setFromDepartmentName(String fromDepartmentName) {
        this.fromDepartmentName = fromDepartmentName;
    }

    public String getToDepartmentName() {
        return toDepartmentName;
    }

    public void setToDepartmentName(String toDepartmentName) {
        this.toDepartmentName = toDepartmentName;
    }

    public String getReceivedByName() {
        return receivedByName;
    }

    public void setReceivedByName(String receivedByName) {
        this.receivedByName = receivedByName;
    }

    public String getReceivedByStaffCode() {
        return receivedByStaffCode;
    }

    public void setReceivedByStaffCode(String receivedByStaffCode) {
        this.receivedByStaffCode = receivedByStaffCode;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public void setIssuedByName(String issuedByName) {
        this.issuedByName = issuedByName;
    }

    public String getIssuedByStaffCode() {
        return issuedByStaffCode;
    }

    public void setIssuedByStaffCode(String issuedByStaffCode) {
        this.issuedByStaffCode = issuedByStaffCode;
    }

    public String getTransporterName() {
        return transporterName;
    }

    public void setTransporterName(String transporterName) {
        this.transporterName = transporterName;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getInstitutionAddress() {
        return institutionAddress;
    }

    public void setInstitutionAddress(String institutionAddress) {
        this.institutionAddress = institutionAddress;
    }

    public String getInstitutionPhone() {
        return institutionPhone;
    }

    public void setInstitutionPhone(String institutionPhone) {
        this.institutionPhone = institutionPhone;
    }

    public String getInstitutionFax() {
        return institutionFax;
    }

    public void setInstitutionFax(String institutionFax) {
        this.institutionFax = institutionFax;
    }

    public String getInstitutionEmail() {
        return institutionEmail;
    }

    public void setInstitutionEmail(String institutionEmail) {
        this.institutionEmail = institutionEmail;
    }

    public String getDepartmentPrintingName() {
        return departmentPrintingName;
    }

    public void setDepartmentPrintingName(String departmentPrintingName) {
        this.departmentPrintingName = departmentPrintingName;
    }

    public String getDepartmentAddress() {
        return departmentAddress;
    }

    public void setDepartmentAddress(String departmentAddress) {
        this.departmentAddress = departmentAddress;
    }

    public String getDepartmentPhone1() {
        return departmentPhone1;
    }

    public void setDepartmentPhone1(String departmentPhone1) {
        this.departmentPhone1 = departmentPhone1;
    }

    public String getDepartmentPhone2() {
        return departmentPhone2;
    }

    public void setDepartmentPhone2(String departmentPhone2) {
        this.departmentPhone2 = departmentPhone2;
    }

    public String getDepartmentFax() {
        return departmentFax;
    }

    public void setDepartmentFax(String departmentFax) {
        this.departmentFax = departmentFax;
    }

    public String getDepartmentEmail() {
        return departmentEmail;
    }

    public void setDepartmentEmail(String departmentEmail) {
        this.departmentEmail = departmentEmail;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getTotalPurchaseValue() {
        return totalPurchaseValue;
    }

    public void setTotalPurchaseValue(double totalPurchaseValue) {
        this.totalPurchaseValue = totalPurchaseValue;
    }

    public double getTotalRetailSaleValue() {
        return totalRetailSaleValue;
    }

    public void setTotalRetailSaleValue(double totalRetailSaleValue) {
        this.totalRetailSaleValue = totalRetailSaleValue;
    }

    public double getTotalWholesaleValue() {
        return totalWholesaleValue;
    }

    public void setTotalWholesaleValue(double totalWholesaleValue) {
        this.totalWholesaleValue = totalWholesaleValue;
    }

    public double getTotalCostValue() {
        return totalCostValue;
    }

    public void setTotalCostValue(double totalCostValue) {
        this.totalCostValue = totalCostValue;
    }

    public double getIssuedNetTotal() {
        return issuedNetTotal;
    }

    public void setIssuedNetTotal(double issuedNetTotal) {
        this.issuedNetTotal = issuedNetTotal;
    }

    public String getApprovedByName() {
        return approvedByName;
    }

    public void setApprovedByName(String approvedByName) {
        this.approvedByName = approvedByName;
    }

    public Date getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getFooterCss() {
        return footerCss;
    }

    public void setFooterCss(String footerCss) {
        this.footerCss = footerCss;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public String getTemplateHeader() {
        return templateHeader;
    }

    public void setTemplateHeader(String templateHeader) {
        this.templateHeader = templateHeader;
    }

    public List<TransferReceiveItemPrintDto> getItems() {
        return items;
    }

    public void setItems(List<TransferReceiveItemPrintDto> items) {
        this.items = items;
    }
}
