/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Print DTO for a native Purchase Order print page.
 * Populated by PurchaseOrderNativeSqlService.loadPrintDtoByBillId().
 * Related issue: #20923
 */
public class PurchaseOrderPrintDto implements Serializable {

    private static final long serialVersionUID = 1L;

    // ---- Approval bill fields ----
    private String poNumber;
    private String paymentMethod;
    private int creditDuration;
    private Date poTime;
    private boolean consignment;
    private String approveComments;
    private Date approvedAt;
    private String approvedByName;
    private double netTotal;
    private boolean cancelled;

    // ---- Request (pre-bill) fields ----
    private Date poDate;
    private String requestComments;
    private String preparedByName;
    private Date preparedAt;

    // ---- Institution / department (from request creater) ----
    private String institutionName;
    private String institutionPhone;
    private String institutionFax;
    private String departmentTelephone1;
    private String departmentEmail;

    // ---- Approval bill department ----
    private String orderDepartmentName;
    private String orderDepartmentAddress;
    private String orderDepartmentTelephone1;
    private String orderDepartmentTelephone2;
    private String orderDepartmentFax;
    private String orderInstitutionName;
    private String orderInstitutionEmail;
    private String orderSiteName;

    // ---- Supplier (toInstitution) ----
    private String supplierName;
    private String supplierCode;

    // ---- Line items ----
    private List<PurchaseOrderItemPrintDto> items = new ArrayList<>();

    // ---- Getters / setters ----

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public int getCreditDuration() { return creditDuration; }
    public void setCreditDuration(int creditDuration) { this.creditDuration = creditDuration; }

    public Date getPoTime() { return poTime; }
    public void setPoTime(Date poTime) { this.poTime = poTime; }

    public boolean isConsignment() { return consignment; }
    public void setConsignment(boolean consignment) { this.consignment = consignment; }

    public String getApproveComments() { return approveComments; }
    public void setApproveComments(String approveComments) { this.approveComments = approveComments; }

    public Date getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Date approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public double getNetTotal() { return netTotal; }
    public void setNetTotal(double netTotal) { this.netTotal = netTotal; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public Date getPoDate() { return poDate; }
    public void setPoDate(Date poDate) { this.poDate = poDate; }

    public String getRequestComments() { return requestComments; }
    public void setRequestComments(String requestComments) { this.requestComments = requestComments; }

    public String getPreparedByName() { return preparedByName; }
    public void setPreparedByName(String preparedByName) { this.preparedByName = preparedByName; }

    public Date getPreparedAt() { return preparedAt; }
    public void setPreparedAt(Date preparedAt) { this.preparedAt = preparedAt; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getInstitutionPhone() { return institutionPhone; }
    public void setInstitutionPhone(String institutionPhone) { this.institutionPhone = institutionPhone; }

    public String getInstitutionFax() { return institutionFax; }
    public void setInstitutionFax(String institutionFax) { this.institutionFax = institutionFax; }

    public String getDepartmentTelephone1() { return departmentTelephone1; }
    public void setDepartmentTelephone1(String departmentTelephone1) { this.departmentTelephone1 = departmentTelephone1; }

    public String getDepartmentEmail() { return departmentEmail; }
    public void setDepartmentEmail(String departmentEmail) { this.departmentEmail = departmentEmail; }

    public String getOrderDepartmentName() { return orderDepartmentName; }
    public void setOrderDepartmentName(String orderDepartmentName) { this.orderDepartmentName = orderDepartmentName; }

    public String getOrderDepartmentAddress() { return orderDepartmentAddress; }
    public void setOrderDepartmentAddress(String orderDepartmentAddress) { this.orderDepartmentAddress = orderDepartmentAddress; }

    public String getOrderDepartmentTelephone1() { return orderDepartmentTelephone1; }
    public void setOrderDepartmentTelephone1(String orderDepartmentTelephone1) { this.orderDepartmentTelephone1 = orderDepartmentTelephone1; }

    public String getOrderDepartmentTelephone2() { return orderDepartmentTelephone2; }
    public void setOrderDepartmentTelephone2(String orderDepartmentTelephone2) { this.orderDepartmentTelephone2 = orderDepartmentTelephone2; }

    public String getOrderDepartmentFax() { return orderDepartmentFax; }
    public void setOrderDepartmentFax(String orderDepartmentFax) { this.orderDepartmentFax = orderDepartmentFax; }

    public String getOrderInstitutionName() { return orderInstitutionName; }
    public void setOrderInstitutionName(String orderInstitutionName) { this.orderInstitutionName = orderInstitutionName; }

    public String getOrderInstitutionEmail() { return orderInstitutionEmail; }
    public void setOrderInstitutionEmail(String orderInstitutionEmail) { this.orderInstitutionEmail = orderInstitutionEmail; }

    public String getOrderSiteName() { return orderSiteName; }
    public void setOrderSiteName(String orderSiteName) { this.orderSiteName = orderSiteName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getSupplierCode() { return supplierCode; }
    public void setSupplierCode(String supplierCode) { this.supplierCode = supplierCode; }

    public List<PurchaseOrderItemPrintDto> getItems() { return items; }
    public void setItems(List<PurchaseOrderItemPrintDto> items) { this.items = items; }
}
