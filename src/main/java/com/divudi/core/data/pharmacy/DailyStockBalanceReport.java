package com.divudi.core.data.pharmacy;

import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.entity.Department;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Dr M H B Ariyaratne
 */
public class DailyStockBalanceReport {

    private Date date;
    private Department department;

    private double openingStockValue;
    private double stockTransactionValue;
    private double closingStockValue;

    private PharmacyBundle pharmacySalesByAdmissionTypeAndDiscountSchemeBundle;
    private PharmacyBundle pharmacyPurchaseByBillTypeBundle;
    private PharmacyBundle pharmacyTransferByBillTypeBundle;
    private PharmacyBundle pharmacyAdjustmentsByBillTypeBundle;
    
    private double purchasesOfDrugsPrevious;
    private double purchasesOfDrugsTransaction;
    private double purchasesOfDrugsClosing;

    private double internalReceiptsPrevious;
    private double internalReceiptsTransaction;
    private double internalReceiptsClosing;

    private double purchaseReturnsPrevious;
    private double purchaseReturnsTransaction;
    private double purchaseReturnsClosing;

    private double totalStockPrevious;
    private double totalStockTransaction;
    private double totalStockClosing;

    private double drugSalesCashPrevious;
    private double drugSalesCashTransaction;
    private double drugSalesCashClosing;

    private double memberSalesCashPrevious;
    private double memberSalesCashTransaction;
    private double memberSalesCashClosing;

    private double staffSalesCashPrevious;
    private double staffSalesCashTransaction;
    private double staffSalesCashClosing;

    private double patientSalesCashPrevious;
    private double patientSalesCashTransaction;
    private double patientSalesCashClosing;

    private double drugSalesCardPrevious;
    private double drugSalesCardTransaction;
    private double drugSalesCardClosing;

    private double memberSalesCardPrevious;
    private double memberSalesCardTransaction;
    private double memberSalesCardClosing;

    private double staffSalesCardPrevious;
    private double staffSalesCardTransaction;
    private double staffSalesCardClosing;

    private double patientSalesCardPrevious;
    private double patientSalesCardTransaction;
    private double patientSalesCardClosing;

    private double drugSalesCreditPrevious;
    private double drugSalesCreditTransaction;
    private double drugSalesCreditClosing;

    private double staffSalesCreditPrevious;
    private double staffSalesCreditTransaction;
    private double staffSalesCreditClosing;

    private double inwardSalesPrevious;
    private double inwardSalesTransaction;
    private double inwardSalesClosing;

    private double opdCardSalesPrevious;
    private double opdCardSalesTransaction;
    private double opdCardSalesClosing;

    private double totalSalesPrevious;
    private double totalSalesTransaction;
    private double totalSalesClosing;

    private double internalTransfersPrevious;
    private double internalTransfersTransaction;
    private double internalTransfersClosing;

    private double stockAdjustmentsPrevious;
    private double stockAdjustmentsTransaction;
    private double stockAdjustmentsClosing;

    private double netDrugStockPrevious;
    private double netDrugStockTransaction;
    private double netDrugStockClosing;

    private List<DepartmentTransaction> internalReceiptsList;
    private List<DepartmentTransaction> internalTransfersList;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public double getOpeningStockValue() {
        return openingStockValue;
    }

    public void setOpeningStockValue(double openingStockValue) {
        this.openingStockValue = openingStockValue;
    }

    public double getStockTransactionValue() {
        return stockTransactionValue;
    }

    public void setStockTransactionValue(double stockTransactionValue) {
        this.stockTransactionValue = stockTransactionValue;
    }

    public double getClosingStockValue() {
        return closingStockValue;
    }

    public void setClosingStockValue(double closingStockValue) {
        this.closingStockValue = closingStockValue;
    }

    public double getPurchasesOfDrugsPrevious() {
        return purchasesOfDrugsPrevious;
    }

    public void setPurchasesOfDrugsPrevious(double purchasesOfDrugsPrevious) {
        this.purchasesOfDrugsPrevious = purchasesOfDrugsPrevious;
    }

    public double getPurchasesOfDrugsTransaction() {
        return purchasesOfDrugsTransaction;
    }

    public void setPurchasesOfDrugsTransaction(double purchasesOfDrugsTransaction) {
        this.purchasesOfDrugsTransaction = purchasesOfDrugsTransaction;
    }

    public double getPurchasesOfDrugsClosing() {
        return purchasesOfDrugsClosing;
    }

    public void setPurchasesOfDrugsClosing(double purchasesOfDrugsClosing) {
        this.purchasesOfDrugsClosing = purchasesOfDrugsClosing;
    }

    public double getInternalReceiptsPrevious() {
        return internalReceiptsPrevious;
    }

    public void setInternalReceiptsPrevious(double internalReceiptsPrevious) {
        this.internalReceiptsPrevious = internalReceiptsPrevious;
    }

    public double getInternalReceiptsTransaction() {
        return internalReceiptsTransaction;
    }

    public void setInternalReceiptsTransaction(double internalReceiptsTransaction) {
        this.internalReceiptsTransaction = internalReceiptsTransaction;
    }

    public double getInternalReceiptsClosing() {
        return internalReceiptsClosing;
    }

    public void setInternalReceiptsClosing(double internalReceiptsClosing) {
        this.internalReceiptsClosing = internalReceiptsClosing;
    }

    public double getPurchaseReturnsPrevious() {
        return purchaseReturnsPrevious;
    }

    public void setPurchaseReturnsPrevious(double purchaseReturnsPrevious) {
        this.purchaseReturnsPrevious = purchaseReturnsPrevious;
    }

    public double getPurchaseReturnsTransaction() {
        return purchaseReturnsTransaction;
    }

    public void setPurchaseReturnsTransaction(double purchaseReturnsTransaction) {
        this.purchaseReturnsTransaction = purchaseReturnsTransaction;
    }

    public double getPurchaseReturnsClosing() {
        return purchaseReturnsClosing;
    }

    public void setPurchaseReturnsClosing(double purchaseReturnsClosing) {
        this.purchaseReturnsClosing = purchaseReturnsClosing;
    }

    public double getTotalStockPrevious() {
        return totalStockPrevious;
    }

    public void setTotalStockPrevious(double totalStockPrevious) {
        this.totalStockPrevious = totalStockPrevious;
    }

    public double getTotalStockTransaction() {
        return totalStockTransaction;
    }

    public void setTotalStockTransaction(double totalStockTransaction) {
        this.totalStockTransaction = totalStockTransaction;
    }

    public double getTotalStockClosing() {
        return totalStockClosing;
    }

    public void setTotalStockClosing(double totalStockClosing) {
        this.totalStockClosing = totalStockClosing;
    }

    public double getDrugSalesCashPrevious() {
        return drugSalesCashPrevious;
    }

    public void setDrugSalesCashPrevious(double drugSalesCashPrevious) {
        this.drugSalesCashPrevious = drugSalesCashPrevious;
    }

    public double getDrugSalesCashTransaction() {
        return drugSalesCashTransaction;
    }

    public void setDrugSalesCashTransaction(double drugSalesCashTransaction) {
        this.drugSalesCashTransaction = drugSalesCashTransaction;
    }

    public double getDrugSalesCashClosing() {
        return drugSalesCashClosing;
    }

    public void setDrugSalesCashClosing(double drugSalesCashClosing) {
        this.drugSalesCashClosing = drugSalesCashClosing;
    }

    public double getMemberSalesCashPrevious() {
        return memberSalesCashPrevious;
    }

    public void setMemberSalesCashPrevious(double memberSalesCashPrevious) {
        this.memberSalesCashPrevious = memberSalesCashPrevious;
    }

    public double getMemberSalesCashTransaction() {
        return memberSalesCashTransaction;
    }

    public void setMemberSalesCashTransaction(double memberSalesCashTransaction) {
        this.memberSalesCashTransaction = memberSalesCashTransaction;
    }

    public double getMemberSalesCashClosing() {
        return memberSalesCashClosing;
    }

    public void setMemberSalesCashClosing(double memberSalesCashClosing) {
        this.memberSalesCashClosing = memberSalesCashClosing;
    }

    public double getStaffSalesCashPrevious() {
        return staffSalesCashPrevious;
    }

    public void setStaffSalesCashPrevious(double staffSalesCashPrevious) {
        this.staffSalesCashPrevious = staffSalesCashPrevious;
    }

    public double getStaffSalesCashTransaction() {
        return staffSalesCashTransaction;
    }

    public void setStaffSalesCashTransaction(double staffSalesCashTransaction) {
        this.staffSalesCashTransaction = staffSalesCashTransaction;
    }

    public double getStaffSalesCashClosing() {
        return staffSalesCashClosing;
    }

    public void setStaffSalesCashClosing(double staffSalesCashClosing) {
        this.staffSalesCashClosing = staffSalesCashClosing;
    }

    public double getPatientSalesCashPrevious() {
        return patientSalesCashPrevious;
    }

    public void setPatientSalesCashPrevious(double patientSalesCashPrevious) {
        this.patientSalesCashPrevious = patientSalesCashPrevious;
    }

    public double getPatientSalesCashTransaction() {
        return patientSalesCashTransaction;
    }

    public void setPatientSalesCashTransaction(double patientSalesCashTransaction) {
        this.patientSalesCashTransaction = patientSalesCashTransaction;
    }

    public double getPatientSalesCashClosing() {
        return patientSalesCashClosing;
    }

    public void setPatientSalesCashClosing(double patientSalesCashClosing) {
        this.patientSalesCashClosing = patientSalesCashClosing;
    }

    public double getDrugSalesCardPrevious() {
        return drugSalesCardPrevious;
    }

    public void setDrugSalesCardPrevious(double drugSalesCardPrevious) {
        this.drugSalesCardPrevious = drugSalesCardPrevious;
    }

    public double getDrugSalesCardTransaction() {
        return drugSalesCardTransaction;
    }

    public void setDrugSalesCardTransaction(double drugSalesCardTransaction) {
        this.drugSalesCardTransaction = drugSalesCardTransaction;
    }

    public double getDrugSalesCardClosing() {
        return drugSalesCardClosing;
    }

    public void setDrugSalesCardClosing(double drugSalesCardClosing) {
        this.drugSalesCardClosing = drugSalesCardClosing;
    }

    public double getMemberSalesCardPrevious() {
        return memberSalesCardPrevious;
    }

    public void setMemberSalesCardPrevious(double memberSalesCardPrevious) {
        this.memberSalesCardPrevious = memberSalesCardPrevious;
    }

    public double getMemberSalesCardTransaction() {
        return memberSalesCardTransaction;
    }

    public void setMemberSalesCardTransaction(double memberSalesCardTransaction) {
        this.memberSalesCardTransaction = memberSalesCardTransaction;
    }

    public double getMemberSalesCardClosing() {
        return memberSalesCardClosing;
    }

    public void setMemberSalesCardClosing(double memberSalesCardClosing) {
        this.memberSalesCardClosing = memberSalesCardClosing;
    }

    public double getStaffSalesCardPrevious() {
        return staffSalesCardPrevious;
    }

    public void setStaffSalesCardPrevious(double staffSalesCardPrevious) {
        this.staffSalesCardPrevious = staffSalesCardPrevious;
    }

    public double getStaffSalesCardTransaction() {
        return staffSalesCardTransaction;
    }

    public void setStaffSalesCardTransaction(double staffSalesCardTransaction) {
        this.staffSalesCardTransaction = staffSalesCardTransaction;
    }

    public double getStaffSalesCardClosing() {
        return staffSalesCardClosing;
    }

    public void setStaffSalesCardClosing(double staffSalesCardClosing) {
        this.staffSalesCardClosing = staffSalesCardClosing;
    }

    public double getPatientSalesCardPrevious() {
        return patientSalesCardPrevious;
    }

    public void setPatientSalesCardPrevious(double patientSalesCardPrevious) {
        this.patientSalesCardPrevious = patientSalesCardPrevious;
    }

    public double getPatientSalesCardTransaction() {
        return patientSalesCardTransaction;
    }

    public void setPatientSalesCardTransaction(double patientSalesCardTransaction) {
        this.patientSalesCardTransaction = patientSalesCardTransaction;
    }

    public double getPatientSalesCardClosing() {
        return patientSalesCardClosing;
    }

    public void setPatientSalesCardClosing(double patientSalesCardClosing) {
        this.patientSalesCardClosing = patientSalesCardClosing;
    }

    public double getDrugSalesCreditPrevious() {
        return drugSalesCreditPrevious;
    }

    public void setDrugSalesCreditPrevious(double drugSalesCreditPrevious) {
        this.drugSalesCreditPrevious = drugSalesCreditPrevious;
    }

    public double getDrugSalesCreditTransaction() {
        return drugSalesCreditTransaction;
    }

    public void setDrugSalesCreditTransaction(double drugSalesCreditTransaction) {
        this.drugSalesCreditTransaction = drugSalesCreditTransaction;
    }

    public double getDrugSalesCreditClosing() {
        return drugSalesCreditClosing;
    }

    public void setDrugSalesCreditClosing(double drugSalesCreditClosing) {
        this.drugSalesCreditClosing = drugSalesCreditClosing;
    }

    public double getStaffSalesCreditPrevious() {
        return staffSalesCreditPrevious;
    }

    public void setStaffSalesCreditPrevious(double staffSalesCreditPrevious) {
        this.staffSalesCreditPrevious = staffSalesCreditPrevious;
    }

    public double getStaffSalesCreditTransaction() {
        return staffSalesCreditTransaction;
    }

    public void setStaffSalesCreditTransaction(double staffSalesCreditTransaction) {
        this.staffSalesCreditTransaction = staffSalesCreditTransaction;
    }

    public double getStaffSalesCreditClosing() {
        return staffSalesCreditClosing;
    }

    public void setStaffSalesCreditClosing(double staffSalesCreditClosing) {
        this.staffSalesCreditClosing = staffSalesCreditClosing;
    }

    public double getInwardSalesPrevious() {
        return inwardSalesPrevious;
    }

    public void setInwardSalesPrevious(double inwardSalesPrevious) {
        this.inwardSalesPrevious = inwardSalesPrevious;
    }

    public double getInwardSalesTransaction() {
        return inwardSalesTransaction;
    }

    public void setInwardSalesTransaction(double inwardSalesTransaction) {
        this.inwardSalesTransaction = inwardSalesTransaction;
    }

    public double getInwardSalesClosing() {
        return inwardSalesClosing;
    }

    public void setInwardSalesClosing(double inwardSalesClosing) {
        this.inwardSalesClosing = inwardSalesClosing;
    }

    public double getOpdCardSalesPrevious() {
        return opdCardSalesPrevious;
    }

    public void setOpdCardSalesPrevious(double opdCardSalesPrevious) {
        this.opdCardSalesPrevious = opdCardSalesPrevious;
    }

    public double getOpdCardSalesTransaction() {
        return opdCardSalesTransaction;
    }

    public void setOpdCardSalesTransaction(double opdCardSalesTransaction) {
        this.opdCardSalesTransaction = opdCardSalesTransaction;
    }

    public double getOpdCardSalesClosing() {
        return opdCardSalesClosing;
    }

    public void setOpdCardSalesClosing(double opdCardSalesClosing) {
        this.opdCardSalesClosing = opdCardSalesClosing;
    }

    public double getTotalSalesPrevious() {
        return totalSalesPrevious;
    }

    public void setTotalSalesPrevious(double totalSalesPrevious) {
        this.totalSalesPrevious = totalSalesPrevious;
    }

    public double getTotalSalesTransaction() {
        return totalSalesTransaction;
    }

    public void setTotalSalesTransaction(double totalSalesTransaction) {
        this.totalSalesTransaction = totalSalesTransaction;
    }

    public double getTotalSalesClosing() {
        return totalSalesClosing;
    }

    public void setTotalSalesClosing(double totalSalesClosing) {
        this.totalSalesClosing = totalSalesClosing;
    }

    public double getInternalTransfersPrevious() {
        return internalTransfersPrevious;
    }

    public void setInternalTransfersPrevious(double internalTransfersPrevious) {
        this.internalTransfersPrevious = internalTransfersPrevious;
    }

    public double getInternalTransfersTransaction() {
        return internalTransfersTransaction;
    }

    public void setInternalTransfersTransaction(double internalTransfersTransaction) {
        this.internalTransfersTransaction = internalTransfersTransaction;
    }

    public double getInternalTransfersClosing() {
        return internalTransfersClosing;
    }

    public void setInternalTransfersClosing(double internalTransfersClosing) {
        this.internalTransfersClosing = internalTransfersClosing;
    }

    public double getStockAdjustmentsPrevious() {
        return stockAdjustmentsPrevious;
    }

    public void setStockAdjustmentsPrevious(double stockAdjustmentsPrevious) {
        this.stockAdjustmentsPrevious = stockAdjustmentsPrevious;
    }

    public double getStockAdjustmentsTransaction() {
        return stockAdjustmentsTransaction;
    }

    public void setStockAdjustmentsTransaction(double stockAdjustmentsTransaction) {
        this.stockAdjustmentsTransaction = stockAdjustmentsTransaction;
    }

    public double getStockAdjustmentsClosing() {
        return stockAdjustmentsClosing;
    }

    public void setStockAdjustmentsClosing(double stockAdjustmentsClosing) {
        this.stockAdjustmentsClosing = stockAdjustmentsClosing;
    }

    public double getNetDrugStockPrevious() {
        return netDrugStockPrevious;
    }

    public void setNetDrugStockPrevious(double netDrugStockPrevious) {
        this.netDrugStockPrevious = netDrugStockPrevious;
    }

    public double getNetDrugStockTransaction() {
        return netDrugStockTransaction;
    }

    public void setNetDrugStockTransaction(double netDrugStockTransaction) {
        this.netDrugStockTransaction = netDrugStockTransaction;
    }

    public double getNetDrugStockClosing() {
        return netDrugStockClosing;
    }

    public void setNetDrugStockClosing(double netDrugStockClosing) {
        this.netDrugStockClosing = netDrugStockClosing;
    }

    public List<DepartmentTransaction> getInternalReceiptsList() {
        return internalReceiptsList;
    }

    public void setInternalReceiptsList(List<DepartmentTransaction> internalReceiptsList) {
        this.internalReceiptsList = internalReceiptsList;
    }

    public List<DepartmentTransaction> getInternalTransfersList() {
        return internalTransfersList;
    }

    public void setInternalTransfersList(List<DepartmentTransaction> internalTransfersList) {
        this.internalTransfersList = internalTransfersList;
    }

    public PharmacyBundle getPharmacySalesByAdmissionTypeAndDiscountSchemeBundle() {
        return pharmacySalesByAdmissionTypeAndDiscountSchemeBundle;
    }

    public void setPharmacySalesByAdmissionTypeAndDiscountSchemeBundle(PharmacyBundle pharmacySalesByAdmissionTypeAndDiscountSchemeBundle) {
        this.pharmacySalesByAdmissionTypeAndDiscountSchemeBundle = pharmacySalesByAdmissionTypeAndDiscountSchemeBundle;
    }

    public PharmacyBundle getPharmacyPurchaseByBillTypeBundle() {
        return pharmacyPurchaseByBillTypeBundle;
    }

    public void setPharmacyPurchaseByBillTypeBundle(PharmacyBundle pharmacyPurchaseByBillTypeBundle) {
        this.pharmacyPurchaseByBillTypeBundle = pharmacyPurchaseByBillTypeBundle;
    }

    public PharmacyBundle getPharmacyTransferByBillTypeBundle() {
        return pharmacyTransferByBillTypeBundle;
    }

    public void setPharmacyTransferByBillTypeBundle(PharmacyBundle pharmacyTransferByBillTypeBundle) {
        this.pharmacyTransferByBillTypeBundle = pharmacyTransferByBillTypeBundle;
    }

    public PharmacyBundle getPharmacyAdjustmentsByBillTypeBundle() {
        return pharmacyAdjustmentsByBillTypeBundle;
    }

    public void setPharmacyAdjustmentsByBillTypeBundle(PharmacyBundle pharmacyAdjustmentsByBillTypeBundle) {
        this.pharmacyAdjustmentsByBillTypeBundle = pharmacyAdjustmentsByBillTypeBundle;
    }

    
    

}
