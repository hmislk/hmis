/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.pharmacy;

import java.io.Serializable;
import java.util.Date;

/**
 * Flat DTO for the GRN bill header — primitives, String, and Date only.
 * Populated by PharmacyGrnNativeSqlService from two native SQL queries.
 * No JPA entity references; safe for long-lived @SessionScoped storage.
 */
public class GrnPrintHeaderDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private long   billId;
    private String grnNumber;
    private String invoiceNumber;
    private Date   createdAt;
    private String paymentMethod;
    private int    creditDuration;
    private boolean consignment;
    private double total;
    private double expenseTotal;
    private double expensesConsideredForCosting;
    private double expensesNotConsideredForCosting;
    private double tax;
    private double discount;
    private double netTotal;
    private boolean cancelled;

    // Department
    private String departmentPrintingName;
    private String departmentName;
    private String departmentAddress;
    private String departmentTelephone1;
    private String departmentTelephone2;
    private String departmentFax;
    private String departmentEmail;

    // Reference PO bill
    private String poNumber;
    private Date   poCreatedAt;
    private String poApproverName;

    // Supplier (fromInstitution)
    private String supplierName;

    // Created by person
    private String createdByName;

    // BillFinanceDetails aggregates
    private double totalBillValue;
    private double totalCostValue;
    private double totalRetailSaleValue;

    // -----------------------------------------------------------------------
    // Accessors
    // -----------------------------------------------------------------------

    public long getBillId() { return billId; }
    public void setBillId(long billId) { this.billId = billId; }

    public String getGrnNumber() { return grnNumber; }
    public void setGrnNumber(String grnNumber) { this.grnNumber = grnNumber; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public int getCreditDuration() { return creditDuration; }
    public void setCreditDuration(int creditDuration) { this.creditDuration = creditDuration; }

    public boolean isConsignment() { return consignment; }
    public void setConsignment(boolean consignment) { this.consignment = consignment; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public double getExpenseTotal() { return expenseTotal; }
    public void setExpenseTotal(double expenseTotal) { this.expenseTotal = expenseTotal; }

    public double getExpensesConsideredForCosting() { return expensesConsideredForCosting; }
    public void setExpensesConsideredForCosting(double v) { this.expensesConsideredForCosting = v; }

    public double getExpensesNotConsideredForCosting() { return expensesNotConsideredForCosting; }
    public void setExpensesNotConsideredForCosting(double v) { this.expensesNotConsideredForCosting = v; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getNetTotal() { return netTotal; }
    public void setNetTotal(double netTotal) { this.netTotal = netTotal; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public String getDepartmentPrintingName() { return departmentPrintingName; }
    public void setDepartmentPrintingName(String v) { this.departmentPrintingName = v; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentAddress() { return departmentAddress; }
    public void setDepartmentAddress(String departmentAddress) { this.departmentAddress = departmentAddress; }

    public String getDepartmentTelephone1() { return departmentTelephone1; }
    public void setDepartmentTelephone1(String v) { this.departmentTelephone1 = v; }

    public String getDepartmentTelephone2() { return departmentTelephone2; }
    public void setDepartmentTelephone2(String v) { this.departmentTelephone2 = v; }

    public String getDepartmentFax() { return departmentFax; }
    public void setDepartmentFax(String departmentFax) { this.departmentFax = departmentFax; }

    public String getDepartmentEmail() { return departmentEmail; }
    public void setDepartmentEmail(String departmentEmail) { this.departmentEmail = departmentEmail; }

    public String getPoNumber() { return poNumber; }
    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public Date getPoCreatedAt() { return poCreatedAt; }
    public void setPoCreatedAt(Date poCreatedAt) { this.poCreatedAt = poCreatedAt; }

    public String getPoApproverName() { return poApproverName; }
    public void setPoApproverName(String poApproverName) { this.poApproverName = poApproverName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public double getTotalBillValue() { return totalBillValue; }
    public void setTotalBillValue(double totalBillValue) { this.totalBillValue = totalBillValue; }

    public double getTotalCostValue() { return totalCostValue; }
    public void setTotalCostValue(double totalCostValue) { this.totalCostValue = totalCostValue; }

    public double getTotalRetailSaleValue() { return totalRetailSaleValue; }
    public void setTotalRetailSaleValue(double totalRetailSaleValue) { this.totalRetailSaleValue = totalRetailSaleValue; }
}
