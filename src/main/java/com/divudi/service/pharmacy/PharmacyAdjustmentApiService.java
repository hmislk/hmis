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
import com.divudi.core.entity.BillFinanceDetails;
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
import java.math.BigDecimal;
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

    /**
     * Adjust purchase rate for a single stock item
     */
    @Transactional
    public AdjustmentResponseDTO adjustPurchaseRate(PurchaseRateAdjustmentDTO request, WebUser user) throws Exception {
        validatePurchaseRateRequest(request);

        Stock stock = loadAndValidateStock(request.getStockId());
        Department department = loadAndValidateDepartment(request.getDepartmentId());

        double currentPurchaseRate = stock.getItemBatch().getPurcahseRate(); // Note: typo is intentional for database compatibility
        double newPurchaseRate = request.getNewPurchaseRate();
        double rateChange = newPurchaseRate - currentPurchaseRate;
        double changeValue = stock.getStock() * rateChange;

        // Create adjustment bill
        Bill adjustmentBill = createPurchaseRateAdjustmentBill(request.getComment(), user, department);

        // Create bill items for audit trail
        PharmaceuticalBillItem pharmaceuticalBillItem = createPurchaseRateAdjustmentBillItem(
            adjustmentBill, stock, currentPurchaseRate, newPurchaseRate, rateChange, changeValue, user);

        // Update actual purchase rate
        stock.getItemBatch().setPurcahseRate(newPurchaseRate); // Note: typo is intentional for database compatibility
        itemBatchFacade.edit(stock.getItemBatch());

        // Add to stock history for audit trail
        pharmacyBean.addToStockHistory(pharmaceuticalBillItem, stock, department);

        // Create response
        AdjustmentResponseDTO response = new AdjustmentResponseDTO();
        response.setBillId(adjustmentBill.getId());
        response.setBillNumber(adjustmentBill.getDeptId());
        response.setStockId(stock.getId());
        response.setStockType("PURCHASE_RATE");
        response.setBeforeValue(currentPurchaseRate);
        response.setAfterValue(newPurchaseRate);
        response.setComment(request.getComment());
        response.setAdjustmentDate(adjustmentBill.getBillDate());

        return response;
    }

    /**
     * Recomputes BillFinanceDetails + bill totals for a single pre-fix adjustment
     * bill, using the before/after audit values already stored on its bill items.
     * Skips (no-op) any bill that already has BillFinanceDetails, so this is safe
     * to re-run over the same set of bills repeatedly.
     */
    @Transactional
    public BackfillResultDTO backfillFinanceDetails(Bill bill, boolean apply) {
        BackfillResultDTO result = new BackfillResultDTO();
        result.setBillId(bill.getId());
        result.setBillTypeAtomic(bill.getBillTypeAtomic() != null ? bill.getBillTypeAtomic().name() : null);

        if (bill.hasBillFinanceDetails()) {
            result.setApplied(false);
            result.setNote("Skipped: BillFinanceDetails already present");
            return result;
        }

        double deltaRetailValue = 0.0;
        double deltaCostValue = 0.0;
        double deltaPurchaseValue = 0.0;
        double deltaQty = 0.0;
        double totalBeforeValue = 0.0;
        double totalAfterValue = 0.0;
        boolean costValueApproximatedFromCurrentRate = false;

        for (BillItem item : bill.getBillItems()) {
            PharmaceuticalBillItem ph = item.getPharmaceuticalBillItem();
            if (ph == null) {
                continue;
            }
            double before = ph.getBeforeAdjustmentValue();
            double after = ph.getAfterAdjustmentValue();

            if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT) {
                // before/after are quantities; item.getNetRate() is the retail rate
                // captured at adjustment time.
                double qtyDelta = after - before;
                deltaRetailValue += qtyDelta * item.getNetRate();
                // NOTE: no historical cost-rate snapshot exists on PharmaceuticalBillItem for
                // this bill type (costRate is never set by the adjustment-creation code), so
                // we fall back to the item batch's CURRENT cost/purchase rate. This is an
                // approximation, not the rate in effect at the time of the original adjustment.
                // Disclosed to the caller via result.note below.
                Double costRateObj = ph.getItemBatch() != null ? ph.getItemBatch().getCostRate() : null;
                double costRate = costRateObj != null ? costRateObj
                        : (ph.getItemBatch() != null ? ph.getItemBatch().getPurcahseRate() : item.getNetRate());
                deltaCostValue += qtyDelta * costRate;
                // Same approximation caveat as cost: no historical purchase-rate snapshot
                // exists for this bill type, so fall back to the item batch's current rate.
                double purchaseRate = ph.getItemBatch() != null ? ph.getItemBatch().getPurcahseRate() : item.getNetRate();
                deltaPurchaseValue += qtyDelta * purchaseRate;
                deltaQty += Math.abs(qtyDelta);
                costValueApproximatedFromCurrentRate = true;
                // before/after are quantities here; convert to values using the retail rate
                // captured at adjustment time (item.getNetRate()), matching createStockAdjustmentBillItem.
                totalBeforeValue += before * item.getNetRate();
                totalAfterValue += after * item.getNetRate();
            } else if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT) {
                // before/after are total values (qty * rate) already, per createRetailRateAdjustmentBillItem
                deltaRetailValue += (after - before);
                deltaQty += item.getQty();
                totalBeforeValue += before;
                totalAfterValue += after;
            } else if (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT) {
                deltaPurchaseValue += (after - before);
                deltaQty += item.getQty();
                totalBeforeValue += before;
                totalAfterValue += after;
            }
        }

        // Purchase-rate-adjustment bills carry their value in deltaPurchaseValue only;
        // stock-quantity and retail-rate adjustments carry it in deltaRetailValue. A
        // stock-quantity bill now also populates deltaPurchaseValue (see above) so the
        // BFD's totalPurchaseValue reconciles, but that must not double-count into the
        // bill's own total/netTotal - only one dimension is ever the bill's "primary" value.
        double netTotal = (bill.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT)
                ? deltaPurchaseValue
                : deltaRetailValue;
        result.setComputedNetTotal(netTotal);
        result.setComputedTotal(Math.abs(netTotal));

        // Build disclosure text (applies to both dry-run and apply paths)
        String disclosureSuffix = "";
        if (costValueApproximatedFromCurrentRate) {
            disclosureSuffix = ". Cost and purchase values approximated using current item batch rates "
                    + "(no historical rate snapshot exists for this bill type)";
        }

        if (!apply) {
            result.setApplied(false);
            result.setNote("Dry run: not persisted" + disclosureSuffix);
            return result;
        }

        BillFinanceDetails bfd = new BillFinanceDetails(bill);
        bfd.setTotalRetailSaleValue(BigDecimal.valueOf(deltaRetailValue));
        bfd.setTotalCostValue(BigDecimal.valueOf(deltaCostValue));
        bfd.setTotalPurchaseValue(BigDecimal.valueOf(deltaPurchaseValue));
        bfd.setTotalWholesaleValue(BigDecimal.ZERO);
        bfd.setGrossTotal(BigDecimal.valueOf(Math.abs(netTotal)));
        bfd.setNetTotal(BigDecimal.valueOf(netTotal));
        bfd.setTotalQuantity(BigDecimal.valueOf(deltaQty));
        bfd.setTotalBeforeAdjustmentValue(BigDecimal.valueOf(totalBeforeValue));
        bfd.setTotalAfterAdjustmentValue(BigDecimal.valueOf(totalAfterValue));
        bill.setBillFinanceDetails(bfd);
        bill.setTotal(Math.abs(netTotal));
        bill.setNetTotal(netTotal);

        billFacade.edit(bill);

        result.setApplied(true);
        String note = "Backfilled from stored before/after audit values" + disclosureSuffix;
        result.setNote(note);
        return result;
    }

    /**
     * Finds pre-fix adjustment bills for a department in a date range (BillFinanceDetails
     * IS NULL is the fingerprint of "created before this fix went live") and runs the
     * backfill over each, in dry-run or apply mode.
     */
    @Transactional
    public java.util.List<BackfillResultDTO> backfillFinanceDetailsForDepartment(
            Department department, java.util.Date fromDate, java.util.Date toDate, boolean apply) {
        java.util.List<BillTypeAtomic> types = java.util.Arrays.asList(
                BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT,
                BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT,
                BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);

        String jpql = "select b from Bill b where b.department=:dep "
                + "and b.billTypeAtomic in :types and b.createdAt between :from and :to "
                + "and b.billFinanceDetails is null and b.retired=:ret order by b.createdAt asc";

        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("dep", department);
        params.put("types", types);
        params.put("from", fromDate);
        params.put("to", toDate);
        params.put("ret", false);

        // Bill.createdAt is @Temporal(TIMESTAMP). The 2-arg findByJpql(jpql, params)
        // overload binds every Date parameter as TemporalType.DATE, which silently
        // truncates the time-of-day - toDate's end-of-day precision (23:59:59.999)
        // would be dropped and the query would compare against midnight instead,
        // silently excluding same-day bills. Bind from/to explicitly as TIMESTAMP.
        java.util.Map<String, javax.persistence.TemporalType> temporalTypes = new java.util.HashMap<>();
        temporalTypes.put("from", javax.persistence.TemporalType.TIMESTAMP);
        temporalTypes.put("to", javax.persistence.TemporalType.TIMESTAMP);

        java.util.List<Bill> bills = billFacade.findByJpql(jpql, params, temporalTypes);

        java.util.List<BackfillResultDTO> results = new java.util.ArrayList<>();
        for (Bill bill : bills) {
            results.add(backfillFinanceDetails(bill, apply));
        }
        return results;
    }

    /**
     * REST-facing overload: resolves departmentId -> Department and parses
     * yyyy-MM-dd date strings before delegating to
     * {@link #backfillFinanceDetailsForDepartment(Department, Date, Date, boolean)}.
     */
    public java.util.List<BackfillResultDTO> backfillFinanceDetailsForDepartment(
            Long departmentId, String fromDateStr, String toDateStr, boolean apply) throws Exception {
        Department department = loadAndValidateDepartment(departmentId);
        Date fromDate = parseDate(fromDateStr);
        // toDate is parsed to midnight (00:00:00) by parseDate; advance it to the end
        // of that day so bills created later on the same calendar day are not silently
        // excluded from the JPQL "between :from and :to" range below.
        Date toDate = endOfDay(parseDate(toDateStr));
        return backfillFinanceDetailsForDepartment(department, fromDate, toDate, apply);
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

    private void validatePurchaseRateRequest(PurchaseRateAdjustmentDTO request) throws Exception {
        if (request.getStockId() == null) {
            throw new Exception("Stock ID is required");
        }
        if (request.getNewPurchaseRate() == null) {
            throw new Exception("New purchase rate is required");
        }
        if (request.getNewPurchaseRate() < 0) {
            throw new Exception("Purchase rate cannot be negative");
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

    /**
     * Advances the given date to the last instant of that calendar day
     * (23:59:59.999), so it can be used as an inclusive upper bound in a
     * "between" range query without excluding bills created later that day.
     */
    Date endOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
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

    private Bill createPurchaseRateAdjustmentBill(String comment, WebUser user, Department department) {
        Bill bill = new Bill();
        bill.setBillDate(Calendar.getInstance().getTime());
        bill.setBillTime(Calendar.getInstance().getTime());
        bill.setCreatedAt(Calendar.getInstance().getTime());
        bill.setCreater(user);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);
        bill.setDeptId(deptId);
        bill.setInsId(deptId);
        bill.setBillType(BillType.PharmacyAdjustmentPurchaseRate);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_PURCHASE_RATE_ADJUSTMENT);

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
        double retailsaleRate = stock.getItemBatch().getRetailsaleRate();
        Double costRateObj = stock.getItemBatch().getCostRate();
        double costRate = (costRateObj != null) ? costRateObj : stock.getItemBatch().getPurcahseRate();
        double purchaseRate = stock.getItemBatch().getPurcahseRate();

        double deltaRetailValue = quantityChange * retailsaleRate;
        double deltaCostValue = quantityChange * costRate;
        double deltaPurchaseValue = quantityChange * purchaseRate;

        BillItem billItem = new BillItem();
        billItem.setItem(stock.getItemBatch().getItem());
        billItem.setQty(quantityChange);
        billItem.setGrossValue(Math.abs(deltaRetailValue));
        billItem.setNetRate(retailsaleRate);
        billItem.setNetValue(deltaRetailValue);
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

        // Populate BillFinanceDetails + bill totals so the F15 report's Adjustment
        // Transactions section reconciles Opening + ... + Adjustments = Closing.
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }
        bfd.setTotalRetailSaleValue(BigDecimal.valueOf(deltaRetailValue));
        bfd.setTotalCostValue(BigDecimal.valueOf(deltaCostValue));
        bfd.setGrossTotal(BigDecimal.valueOf(Math.abs(deltaRetailValue)));
        bfd.setNetTotal(BigDecimal.valueOf(deltaRetailValue));
        bfd.setTotalQuantity(BigDecimal.valueOf(Math.abs(quantityChange)));
        bfd.setTotalBeforeAdjustmentValue(BigDecimal.valueOf(beforeQty * retailsaleRate));
        bfd.setTotalAfterAdjustmentValue(BigDecimal.valueOf(afterQty * retailsaleRate));
        // A quantity change moves stock value at cost, purchase, AND retail rates
        // simultaneously - populate all three so each F15 report column reconciles.
        bfd.setTotalPurchaseValue(BigDecimal.valueOf(deltaPurchaseValue));
        bfd.setTotalWholesaleValue(BigDecimal.ZERO);

        bill.setTotal(Math.abs(deltaRetailValue));
        bill.setNetTotal(deltaRetailValue);
        billFacade.edit(bill);

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
        pharmaceuticalBillItem.setLastPurchaseRate(newRetailRate);
        pharmaceuticalBillItem.setFreeQty((float) rateChange);
        pharmaceuticalBillItem.setBeforeAdjustmentValue(stock.getStock() * oldRetailRate);
        pharmaceuticalBillItem.setAfterAdjustmentValue(stock.getStock() * newRetailRate);

        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);
        pharmaceuticalBillItem.setBillItem(billItem);

        billItemFacade.create(billItem);

        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }
        bfd.setTotalRetailSaleValue(BigDecimal.valueOf(changeValue));
        bfd.setGrossTotal(BigDecimal.valueOf(Math.abs(changeValue)));
        bfd.setNetTotal(BigDecimal.valueOf(changeValue));
        bfd.setTotalQuantity(BigDecimal.valueOf(stock.getStock()));
        bfd.setTotalBeforeAdjustmentValue(BigDecimal.valueOf(stock.getStock() * oldRetailRate));
        bfd.setTotalAfterAdjustmentValue(BigDecimal.valueOf(stock.getStock() * newRetailRate));
        bfd.setTotalCostValue(BigDecimal.ZERO);
        bfd.setTotalPurchaseValue(BigDecimal.ZERO);
        bfd.setTotalWholesaleValue(BigDecimal.ZERO);

        bill.setTotal(Math.abs(changeValue));
        bill.setNetTotal(changeValue);
        billFacade.edit(bill);

        return pharmaceuticalBillItem;
    }

    private PharmaceuticalBillItem createExpiryDateAdjustmentBillItem(Bill bill, Stock stock, Date oldExpiryDate,
                                                                    Date newExpiryDate, WebUser user) {
        BillItem billItem = new BillItem();
        billItem.setItem(stock.getItemBatch().getItem());
        billItem.setQty(0.0);
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

        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }
        bfd.setTotalRetailSaleValue(BigDecimal.ZERO);
        bfd.setTotalCostValue(BigDecimal.ZERO);
        bfd.setTotalPurchaseValue(BigDecimal.ZERO);
        bfd.setTotalWholesaleValue(BigDecimal.ZERO);
        bfd.setGrossTotal(BigDecimal.ZERO);
        bfd.setNetTotal(BigDecimal.ZERO);
        bfd.setTotalQuantity(BigDecimal.ZERO);
        bfd.setTotalBeforeAdjustmentValue(BigDecimal.ZERO);
        bfd.setTotalAfterAdjustmentValue(BigDecimal.ZERO);

        bill.setTotal(0.0);
        bill.setNetTotal(0.0);
        billFacade.edit(bill);

        return pharmaceuticalBillItem;
    }

    private PharmaceuticalBillItem createPurchaseRateAdjustmentBillItem(Bill bill, Stock stock, double oldPurchaseRate,
                                                                       double newPurchaseRate, double rateChange, double changeValue, WebUser user) {
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
        pharmaceuticalBillItem.setPurchaseRate(oldPurchaseRate);
        pharmaceuticalBillItem.setLastPurchaseRate(newPurchaseRate);
        pharmaceuticalBillItem.setFreeQty((float) rateChange);
        pharmaceuticalBillItem.setBeforeAdjustmentValue(stock.getStock() * oldPurchaseRate);
        pharmaceuticalBillItem.setAfterAdjustmentValue(stock.getStock() * newPurchaseRate);

        billItem.setPharmaceuticalBillItem(pharmaceuticalBillItem);
        pharmaceuticalBillItem.setBillItem(billItem);

        billItemFacade.create(billItem);

        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        if (bfd == null) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }
        bfd.setTotalPurchaseValue(BigDecimal.valueOf(changeValue));
        bfd.setGrossTotal(BigDecimal.valueOf(Math.abs(changeValue)));
        bfd.setNetTotal(BigDecimal.valueOf(changeValue));
        bfd.setTotalQuantity(BigDecimal.valueOf(stock.getStock()));
        bfd.setTotalBeforeAdjustmentValue(BigDecimal.valueOf(stock.getStock() * oldPurchaseRate));
        bfd.setTotalAfterAdjustmentValue(BigDecimal.valueOf(stock.getStock() * newPurchaseRate));
        bfd.setTotalCostValue(BigDecimal.ZERO);
        bfd.setTotalRetailSaleValue(BigDecimal.ZERO);
        bfd.setTotalWholesaleValue(BigDecimal.ZERO);

        bill.setTotal(Math.abs(changeValue));
        bill.setNetTotal(changeValue);
        billFacade.edit(bill);

        return pharmaceuticalBillItem;
    }
}