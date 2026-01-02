/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.adjustment.*;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Service for Pharmacy Adjustment API operations
 * Provides business logic for stock, rate, and expiry adjustments
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class PharmacyAdjustmentApiService implements Serializable {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private StockFacade stockFacade;

    @EJB
    private ItemBatchFacade itemBatchFacade;

    @EJB
    private DepartmentFacade departmentFacade;

    @EJB
    private PharmacyBean pharmacyBean;

    @EJB
    private BillNumberGenerator billNumberGenerator;

    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * Adjust stock quantity for a single stock item
     */
    @Transactional
    public AdjustmentResponseDTO adjustStockQuantity(StockQuantityAdjustmentDTO request, WebUser user) throws Exception {
        validateStockQuantityRequest(request);

        Stock stock = loadAndValidateStock(request.getStockId());
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        double currentQuantity = stock.getStock();
        double newQuantity = request.getNewQuantity();
        double quantityChange = newQuantity - currentQuantity;

        // Create adjustment bill
        Bill adjustmentBill = createStockAdjustmentBill(request.getComment(), user, department);

        // Create bill items for audit trail
        PharmaceuticalBillItem pharmaceuticalBillItem = createStockAdjustmentBillItem(
            adjustmentBill, stock, quantityChange, currentQuantity, newQuantity, user);

        // Update actual stock quantity
        pharmacyBean.resetStock(pharmaceuticalBillItem, stock, newQuantity, department);

        // Create response
        AdjustmentResponseDTO response = new AdjustmentResponseDTO();
        response.setBillId(adjustmentBill.getId());
        response.setBillNumber(adjustmentBill.getDeptId());
        response.setStockId(stock.getId());
        response.setStockType("QUANTITY");
        response.setBeforeValue(currentQuantity);
        response.setAfterValue(newQuantity);
        response.setComment(request.getComment());
        response.setAdjustmentDate(adjustmentBill.getBillDate());

        return response;
    }

    /**
     * Adjust retail rate for a single stock item
     */
    @Transactional
    public AdjustmentResponseDTO adjustRetailRate(RetailRateAdjustmentDTO request, WebUser user) throws Exception {
        validateRetailRateRequest(request);

        Stock stock = loadAndValidateStock(request.getStockId());
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        double currentRetailRate = stock.getItemBatch().getRetailsaleRate();
        double newRetailRate = request.getNewRetailRate();
        double rateChange = newRetailRate - currentRetailRate;
        double changeValue = stock.getStock() * rateChange;

        // Create adjustment bill
        Bill adjustmentBill = createRetailRateAdjustmentBill(request.getComment(), user, department);

        // Create bill items for audit trail
        PharmaceuticalBillItem pharmaceuticalBillItem = createRetailRateAdjustmentBillItem(
            adjustmentBill, stock, currentRetailRate, newRetailRate, rateChange, changeValue, user);

        // Update actual retail rate
        stock.getItemBatch().setRetailsaleRate(newRetailRate);
        itemBatchFacade.edit(stock.getItemBatch());

        // Add to stock history for audit trail
        pharmacyBean.addToStockHistory(pharmaceuticalBillItem, stock, department);

        // Create response
        AdjustmentResponseDTO response = new AdjustmentResponseDTO();
        response.setBillId(adjustmentBill.getId());
        response.setBillNumber(adjustmentBill.getDeptId());
        response.setStockId(stock.getId());
        response.setStockType("RETAIL_RATE");
        response.setBeforeValue(currentRetailRate);
        response.setAfterValue(newRetailRate);
        response.setComment(request.getComment());
        response.setAdjustmentDate(adjustmentBill.getBillDate());

        return response;
    }

    /**
     * Adjust expiry date for a single stock item
     */
    @Transactional
    public AdjustmentResponseDTO adjustExpiryDate(ExpiryDateAdjustmentDTO request, WebUser user) throws Exception {
        validateExpiryDateRequest(request);

        Stock stock = loadAndValidateStock(request.getStockId());
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        Date currentExpiryDate = stock.getItemBatch().getDateOfExpire();
        Date newExpiryDate = parseDate(request.getNewExpiryDate());

        // Normalize to end-of-month if configured
        boolean expiryIsAlwaysMonthEnd = configOptionApplicationController.getBooleanValueByKey("Always Set Expiry Date to Month End", false);
        if (expiryIsAlwaysMonthEnd) {
            newExpiryDate = normalizeToEndOfMonth(newExpiryDate);
        }

        // Create adjustment bill
        Bill adjustmentBill = createExpiryDateAdjustmentBill(request.getComment(), user, department);

        // Create bill items for audit trail
        PharmaceuticalBillItem pharmaceuticalBillItem = createExpiryDateAdjustmentBillItem(
            adjustmentBill, stock, currentExpiryDate, newExpiryDate, user);

        // Update actual expiry date
        stock.getItemBatch().setDateOfExpire(newExpiryDate);
        itemBatchFacade.edit(stock.getItemBatch());

        // Add to stock history for audit trail
        pharmacyBean.addToStockHistory(pharmaceuticalBillItem, stock, department);

        // Create response
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        AdjustmentResponseDTO response = new AdjustmentResponseDTO();
        response.setBillId(adjustmentBill.getId());
        response.setBillNumber(adjustmentBill.getDeptId());
        response.setStockId(stock.getId());
        response.setStockType("EXPIRY_DATE");
        response.setBeforeText(currentExpiryDate != null ? dateFormat.format(currentExpiryDate) : null);
        response.setAfterText(dateFormat.format(newExpiryDate));
        response.setComment(request.getComment());
        response.setAdjustmentDate(adjustmentBill.getBillDate());

        return response;
    }

    // Private helper methods

    private void validateStockQuantityRequest(StockQuantityAdjustmentDTO request) throws Exception {
        if (request.getStockId() == null) {
            throw new Exception("Stock ID is required");
        }
        if (request.getNewQuantity() == null) {
            throw new Exception("New quantity is required");
        }
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new Exception("Comment is required");
        }
        if (request.getDepartmentId() == null) {
            throw new Exception("Department ID is required");
        }
    }

    private void validateRetailRateRequest(RetailRateAdjustmentDTO request) throws Exception {
        if (request.getStockId() == null) {
            throw new Exception("Stock ID is required");
        }
        if (request.getNewRetailRate() == null) {
            throw new Exception("New retail rate is required");
        }
        if (request.getNewRetailRate() < 0) {
            throw new Exception("Retail rate cannot be negative");
        }
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new Exception("Comment is required");
        }
        if (request.getDepartmentId() == null) {
            throw new Exception("Department ID is required");
        }
    }

    private void validateExpiryDateRequest(ExpiryDateAdjustmentDTO request) throws Exception {
        if (request.getStockId() == null) {
            throw new Exception("Stock ID is required");
        }
        if (request.getNewExpiryDate() == null || request.getNewExpiryDate().trim().isEmpty()) {
            throw new Exception("New expiry date is required");
        }
        if (request.getComment() == null || request.getComment().trim().isEmpty()) {
            throw new Exception("Comment is required");
        }
        if (request.getDepartmentId() == null) {
            throw new Exception("Department ID is required");
        }
    }

    private Stock loadAndValidateStock(Long stockId) throws Exception {
        Stock stock = stockFacade.find(stockId);
        if (stock == null) {
            throw new Exception("Stock not found with ID: " + stockId);
        }
        if (stock.getItemBatch() == null) {
            throw new Exception("Stock does not have associated item batch");
        }
        return stock;
    }

    private Department loadAndValidateDepartment(Long departmentId) throws Exception {
        Department department = departmentFacade.find(departmentId);
        if (department == null) {
            throw new Exception("Department not found with ID: " + departmentId);
        }
        return department;
    }

    private Date parseDate(String dateString) throws Exception {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new Exception("Invalid date format. Expected yyyy-MM-dd");
        }
    }

    private Date normalizeToEndOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        return cal.getTime();
    }

    private Bill createStockAdjustmentBill(String comment, WebUser user, Department department) {
        Bill bill = new Bill();
        bill.setBillDate(Calendar.getInstance().getTime());
        bill.setBillTime(Calendar.getInstance().getTime());
        bill.setCreatedAt(Calendar.getInstance().getTime());
        bill.setCreater(user);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);
        bill.setBillType(BillType.PharmacyAdjustmentDepartmentStock);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);

        bill.setDepartment(department);
        bill.setInstitution(department.getInstitution());
        bill.setFromDepartment(department);
        bill.setFromInstitution(department.getInstitution());
        bill.setComments(comment);

        billFacade.create(bill);
        return bill;
    }

    private Bill createRetailRateAdjustmentBill(String comment, WebUser user, Department department) {
        Bill bill = new Bill();
        bill.setBillDate(Calendar.getInstance().getTime());
        bill.setBillTime(Calendar.getInstance().getTime());
        bill.setCreatedAt(Calendar.getInstance().getTime());
        bill.setCreater(user);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);
        bill.setBillType(BillType.PharmacyAdjustmentSaleRate);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);

        bill.setDepartment(department);
        bill.setInstitution(department.getInstitution());
        bill.setFromDepartment(department);
        bill.setFromInstitution(department.getInstitution());
        bill.setComments(comment);

        billFacade.create(bill);
        return bill;
    }

    private Bill createExpiryDateAdjustmentBill(String comment, WebUser user, Department department) {
        Bill bill = new Bill();
        bill.setBillDate(Calendar.getInstance().getTime());
        bill.setBillTime(Calendar.getInstance().getTime());
        bill.setCreatedAt(Calendar.getInstance().getTime());
        bill.setCreater(user);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);
        bill.setBillType(BillType.PharmacyAdjustmentExpiryDate);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_EXPIRY_DATE_AJUSTMENT);

        bill.setDepartment(department);
        bill.setInstitution(department.getInstitution());
        bill.setFromDepartment(department);
        bill.setFromInstitution(department.getInstitution());
        bill.setComments(comment);

        billFacade.create(bill);
        return bill;
    }

    private PharmaceuticalBillItem createStockAdjustmentBillItem(Bill bill, Stock stock, double quantityChange,
                                                               double beforeQty, double afterQty, WebUser user) {
        BillItem billItem = new BillItem();
        billItem.setItem(stock.getItemBatch().getItem());
        billItem.setQty(quantityChange);
        billItem.setGrossValue(stock.getItemBatch().getRetailsaleRate() * afterQty);
        billItem.setNetValue(afterQty * billItem.getNetRate());
        billItem.setDiscount(billItem.getGrossValue() - billItem.getNetValue());
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setBill(bill);
        billItem.setSearialNo(1);
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setCreater(user);

        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setStock(stock);
        pharmaceuticalBillItem.setDoe(stock.getItemBatch().getDateOfExpire());
        pharmaceuticalBillItem.setFreeQty(0.0f);
        pharmaceuticalBillItem.setItemBatch(stock.getItemBatch());
        pharmaceuticalBillItem.setBeforeAdjustmentValue(beforeQty);
        pharmaceuticalBillItem.setQty(quantityChange);
        pharmaceuticalBillItem.setAfterAdjustmentValue(afterQty);

        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);
        pharmaceuticalBillItem.setBillItem(billItem);

        billItemFacade.create(billItem);
        return pharmaceuticalBillItem;
    }

    private PharmaceuticalBillItem createRetailRateAdjustmentBillItem(Bill bill, Stock stock, double oldRetailRate,
                                                                    double newRetailRate, double rateChange, double changeValue, WebUser user) {
        BillItem billItem = new BillItem();
        billItem.setItem(stock.getItemBatch().getItem());
        billItem.setQty(stock.getStock());
        billItem.setGrossValue(changeValue);
        billItem.setNetValue(changeValue);
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setBill(bill);
        billItem.setSearialNo(1);
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setCreater(user);

        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setStock(stock);
        pharmaceuticalBillItem.setDoe(stock.getItemBatch().getDateOfExpire());
        pharmaceuticalBillItem.setItemBatch(stock.getItemBatch());
        pharmaceuticalBillItem.setQty(stock.getStock());
        pharmaceuticalBillItem.setRetailRate(oldRetailRate);
        pharmaceuticalBillItem.setLastPurchaseRate(newRetailRate); // Store new rate in this field
        pharmaceuticalBillItem.setFreeQty((float) rateChange); // Store rate change in this field
        pharmaceuticalBillItem.setBeforeAdjustmentValue(stock.getStock() * oldRetailRate);
        pharmaceuticalBillItem.setAfterAdjustmentValue(stock.getStock() * newRetailRate);

        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);
        pharmaceuticalBillItem.setBillItem(billItem);

        billItemFacade.create(billItem);
        return pharmaceuticalBillItem;
    }

    private PharmaceuticalBillItem createExpiryDateAdjustmentBillItem(Bill bill, Stock stock, Date oldExpiryDate,
                                                                    Date newExpiryDate, WebUser user) {
        BillItem billItem = new BillItem();
        billItem.setItem(stock.getItemBatch().getItem());
        billItem.setQty(0.0); // No quantity change for expiry adjustment
        billItem.setGrossValue(0.0);
        billItem.setNetValue(0.0);
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setBill(bill);
        billItem.setSearialNo(1);
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setCreater(user);

        PharmaceuticalBillItem pharmaceuticalBillItem = new PharmaceuticalBillItem();
        pharmaceuticalBillItem.setStock(stock);
        pharmaceuticalBillItem.setDoe(newExpiryDate);
        pharmaceuticalBillItem.setItemBatch(stock.getItemBatch());
        pharmaceuticalBillItem.setQty(0.0);
        pharmaceuticalBillItem.setBeforeAdjustmentExpiry(oldExpiryDate);
        pharmaceuticalBillItem.setAfterAdjustmentExpiry(newExpiryDate);

        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);
        pharmaceuticalBillItem.setBillItem(billItem);

        billItemFacade.create(billItem);
        return pharmaceuticalBillItem;
    }
}