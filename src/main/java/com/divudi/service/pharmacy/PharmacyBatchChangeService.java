/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.ItemBatchFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.PharmacyBean;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Moves one department's quantity of an item from one batch to another batch of
 * the same item (#17799, Option A): the shared ItemBatch rows are never edited
 * and no other department's stock is touched.
 *
 * The whole operation (bill, two bill items, both stock movements, both stock
 * history rows) runs in the one container transaction of
 * {@link #changeBatch}, so it commits or rolls back together.
 *
 * Recorded as a quantity-adjustment family bill
 * ({@link BillTypeAtomic#PHARMACY_STOCK_ADJUSTMENT}, bill type
 * {@link BillType#PharmacyAdjustmentBatchChange}) with two lines: the From
 * batch (negative quantity) and the To batch (positive quantity). The value
 * difference between the two batches is recorded in the BillFinanceDetails
 * exactly as the quantity adjustment does; nothing is posted to any payment,
 * drawer or ledger.
 */
@Stateless
public class PharmacyBatchChangeService {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private BillNumberGenerator billNumberGenerator;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    /**
     * @param sourceStockId the department Stock row to move quantity out of
     * @param targetItemBatchId the batch (of the same item) to move it into
     * @param qty quantity to move, greater than zero and at most the source
     * stock
     * @param comment reason, required
     * @param user the logged-in user
     * @param department the logged-in department; the source stock must be
     * this department's
     * @return the saved adjustment bill, reloaded
     * @throws PharmacyBatchChangeException on any business-rule failure (the
     * transaction is rolled back)
     */
    public Bill changeBatch(Long sourceStockId, Long targetItemBatchId, double qty,
            String comment, WebUser user, Department department) {
        if (user == null || department == null || department.getId() == null) {
            throw new PharmacyBatchChangeException("No logged-in user or department.");
        }
        if (comment == null || comment.trim().isEmpty()) {
            throw new PharmacyBatchChangeException("Add the comment.");
        }
        if (sourceStockId == null) {
            throw new PharmacyBatchChangeException("Select the stock (batch) to move.");
        }
        if (targetItemBatchId == null) {
            throw new PharmacyBatchChangeException("Select the batch to move to.");
        }
        if (Double.isNaN(qty) || Double.isInfinite(qty) || qty <= 0) {
            throw new PharmacyBatchChangeException("Quantity to move must be greater than zero.");
        }

        // Row lock on the source stock, held to the end of this transaction: concurrent moves out of the
        // same row queue up here, so the before/after quantities recorded below are the real ones.
        Stock sourceStock = stockFacade.findWithLock(sourceStockId);
        if (sourceStock == null || sourceStock.isRetired() || sourceStock.getItemBatch() == null) {
            throw new PharmacyBatchChangeException("The selected stock no longer exists.");
        }
        if (sourceStock.getStaff() != null || sourceStock.getDepartment() == null
                || !department.getId().equals(sourceStock.getDepartment().getId())) {
            throw new PharmacyBatchChangeException("The selected stock does not belong to your department.");
        }
        ItemBatch sourceBatch = sourceStock.getItemBatch();
        Item item = sourceBatch.getItem();
        if (item == null) {
            throw new PharmacyBatchChangeException("The selected batch has no item.");
        }
        if (item.getDepartmentType() == DepartmentType.Inventry) {
            throw new PharmacyBatchChangeException("Batch change is not available for inventory items.");
        }
        if (!item.isAllowFractions() && qty % 1 != 0) {
            throw new PharmacyBatchChangeException("This item does not allow fractions; enter a whole number.");
        }

        // Row lock on the target batch: serialises the creation of its department stock row (Stock has no
        // unique key on batch + department), so two concurrent changes cannot both create one.
        ItemBatch targetBatch = itemBatchFacade.findWithLock(targetItemBatchId);
        if (targetBatch == null || targetBatch.isRetired() || targetBatch.getItem() == null) {
            throw new PharmacyBatchChangeException("The batch to move to no longer exists.");
        }
        if (targetBatch.getId().equals(sourceBatch.getId())) {
            throw new PharmacyBatchChangeException("The batch to move to must be a different batch.");
        }
        if (!targetBatch.getItem().getId().equals(item.getId())) {
            throw new PharmacyBatchChangeException("The batch to move to must belong to the same item.");
        }

        double sourceBefore = sourceStock.getStock() == null ? 0.0 : sourceStock.getStock();
        if (sourceBefore < qty) {
            throw new PharmacyBatchChangeException("Quantity to move is more than the available stock of the batch.");
        }
        double sourceAfter = sourceBefore - qty;

        // The department's stock row of the target batch; made here (create(), not edit()) when it has none.
        Stock targetStock = findDepartmentStock(targetBatch, department);
        if (targetStock == null) {
            targetStock = new Stock();
            targetStock.setItemBatch(targetBatch);
            targetStock.setDepartment(department);
            targetStock.setStock(0.0);
            stockFacade.create(targetStock);
        } else {
            targetStock = stockFacade.findWithLock(targetStock.getId());
            if (targetStock == null) {
                throw new PharmacyBatchChangeException("The stock of the batch to move to changed; nothing was moved. Please try again.");
            }
        }
        double targetBefore = targetStock.getStock() == null ? 0.0 : targetStock.getStock();
        double targetAfter = targetBefore + qty;

        Bill bill = createBill(item, comment.trim(), user, department);
        accumulateFinanceDetails(bill, sourceBatch, sourceBefore, -qty, sourceAfter);
        accumulateFinanceDetails(bill, targetBatch, targetBefore, qty, targetAfter);
        billFacade.create(bill);

        // Line 1: From batch (stock out)
        PharmaceuticalBillItem fromLine = createLine(bill, sourceStock, sourceBatch, -qty, sourceBefore, sourceAfter,
                "Batch change: moved out to batch " + targetBatch.getBatchNo(), 1, user);
        if (!pharmacyBean.deductFromStock(sourceStock, qty, fromLine, department)) {
            throw new PharmacyBatchChangeException("Stock of the batch changed while saving; nothing was moved. Please try again.");
        }

        // Line 2: To batch (stock in)
        PharmaceuticalBillItem toLine = createLine(bill, targetStock, targetBatch, qty, targetBefore, targetAfter,
                "Batch change: moved in from batch " + sourceBatch.getBatchNo(), 2, user);
        // Credit the same row the bill line points to (not a second, unfiltered lookup by batch and department).
        if (!pharmacyBean.addToStock(targetStock, qty, toLine, department)) {
            throw new PharmacyBatchChangeException("The stock of the batch to move to could not be updated; nothing was moved. Please try again.");
        }

        return billFacade.findWithoutCache(bill.getId());
    }

    private Stock findDepartmentStock(ItemBatch batch, Department department) {
        String jpql = "select s from Stock s where s.itemBatch.id = :bid and s.department.id = :did "
                + "and s.staff is null and s.retired = false order by s.id";
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("bid", batch.getId());
        params.put("did", department.getId());
        return stockFacade.findFirstByJpql(jpql, params, true);
    }

    private Bill createBill(Item item, String comment, WebUser user, Department department) {
        Date now = Calendar.getInstance().getTime();
        Bill bill = new Bill();
        bill.setBillDate(now);
        bill.setBillTime(now);
        bill.setCreatedAt(now);
        bill.setCreater(user);
        bill.setBillType(BillType.PharmacyAdjustmentBatchChange);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        bill.setDepartment(department);
        bill.setInstitution(department.getInstitution());
        bill.setFromDepartment(department);
        bill.setFromInstitution(department.getInstitution());
        bill.setToDepartment(null);
        bill.setToInstitution(null);
        bill.setComments(comment);
        bill.setDepartmentType(item.getDepartmentType() != null ? item.getDepartmentType() : DepartmentType.Pharmacy);
        bill.setBillFinanceDetails(new BillFinanceDetails(bill));
        bill.setTotal(0.0);
        bill.setNetTotal(0.0);
        assignBillNumbers(bill, department);
        return bill;
    }

    /**
     * Same numbering choices as the quantity adjustment (the "Bill Number
     * Generation Strategy for Stock Adjustments ..." options), so a batch
     * change takes its number from the hospital's stock-adjustment series.
     */
    private void assignBillNumbers(Bill bill, Department dept) {
        boolean deptPrefixDeptIns = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Stock Adjustments - Prefix + Department Code + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean deptPrefixInsDept = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Stock Adjustments - Prefix + Institution Code + Department Code + Year + Yearly Number and Yearly Number", false);
        boolean deptPrefixIns = configOptionApplicationController.getBooleanValueByKey("Bill Number Generation Strategy for Stock Adjustments - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);
        boolean insPrefixIns = configOptionApplicationController.getBooleanValueByKey("Institution Number Generation Strategy for Stock Adjustments - Prefix + Institution Code + Year + Yearly Number and Yearly Number", false);

        String deptId;
        if (deptPrefixDeptIns) {
            deptId = billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixDeptInsYearCount(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        } else if (deptPrefixInsDept) {
            deptId = billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsDeptYearCount(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        } else if (deptPrefixIns) {
            deptId = billNumberGenerator.departmentBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        } else {
            deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT);
        }
        bill.setDeptId(deptId);
        if (insPrefixIns) {
            bill.setInsId(billNumberGenerator.institutionBillNumberGeneratorYearlyWithPrefixInsYearCountInstitutionWide(dept, BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT));
        } else {
            bill.setInsId(deptId);
        }
    }

    /**
     * Adds one line's stock value change to the bill totals and its
     * BillFinanceDetails with the same arithmetic as the quantity adjustment
     * (retail, cost and purchase value change, before/after retail value). The
     * two lines of a batch change net to the value difference between the two
     * batches.
     */
    private void accumulateFinanceDetails(Bill bill, ItemBatch batch, double beforeQty, double changingQty, double afterQty) {
        double retailRate = batch.getRetailsaleRate();
        double purchaseRate = batch.getPurcahseRate();
        Double costRateObj = batch.getCostRate();
        double costRate = costRateObj != null ? costRateObj : purchaseRate;

        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        BigDecimal retailChange = BigDecimal.valueOf(changingQty).multiply(BigDecimal.valueOf(retailRate));
        BigDecimal costChange = BigDecimal.valueOf(changingQty).multiply(BigDecimal.valueOf(costRate));
        BigDecimal purchaseChange = BigDecimal.valueOf(changingQty).multiply(BigDecimal.valueOf(purchaseRate));

        bfd.setTotalRetailSaleValue(nz(bfd.getTotalRetailSaleValue()).add(retailChange));
        bfd.setTotalCostValue(nz(bfd.getTotalCostValue()).add(costChange));
        bfd.setTotalPurchaseValue(nz(bfd.getTotalPurchaseValue()).add(purchaseChange));
        bfd.setGrossTotal(nz(bfd.getGrossTotal()).add(retailChange.abs()));
        bfd.setNetTotal(nz(bfd.getNetTotal()).add(retailChange));
        bfd.setTotalQuantity(nz(bfd.getTotalQuantity()).add(BigDecimal.valueOf(Math.abs(changingQty))));
        bfd.setTotalBeforeAdjustmentValue(nz(bfd.getTotalBeforeAdjustmentValue())
                .add(BigDecimal.valueOf(beforeQty).multiply(BigDecimal.valueOf(retailRate))));
        bfd.setTotalAfterAdjustmentValue(nz(bfd.getTotalAfterAdjustmentValue())
                .add(BigDecimal.valueOf(afterQty).multiply(BigDecimal.valueOf(retailRate))));
        bfd.setTotalWholesaleValue(BigDecimal.ZERO);

        bill.setTotal(bill.getTotal() + retailChange.abs().doubleValue());
        bill.setNetTotal(bill.getNetTotal() + retailChange.doubleValue());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private PharmaceuticalBillItem createLine(Bill bill, Stock stock, ItemBatch batch, double changeQty,
            double beforeQty, double afterQty, String description, int serialNo, WebUser user) {
        double retailRate = batch.getRetailsaleRate();
        double valueChange = changeQty * retailRate;

        BillItem billItem = new BillItem();
        billItem.setBill(bill);
        billItem.setItem(batch.getItem());
        billItem.setQty(changeQty);
        billItem.setNetRate(retailRate);
        billItem.setGrossValue(Math.abs(valueChange));
        billItem.setNetValue(valueChange);
        billItem.setDiscount(billItem.getGrossValue() - billItem.getNetValue());
        billItem.setInwardChargeType(InwardChargeType.Medicine);
        billItem.setDescreption(description);
        billItem.setSearialNo(serialNo);
        billItem.setCreatedAt(Calendar.getInstance().getTime());
        billItem.setCreater(user);

        PharmaceuticalBillItem pbi = new PharmaceuticalBillItem();
        pbi.setStock(stock);
        pbi.setItemBatch(batch);
        pbi.setDoe(batch.getDateOfExpire());
        pbi.setFreeQty(0.0f);
        pbi.setBeforeAdjustmentValue(beforeQty);
        pbi.setQty(changeQty);
        pbi.setAfterAdjustmentValue(afterQty);
        billItem.setPharmaceuticalBillItem(pbi);
        pbi.setBillItem(billItem);

        billItemFacade.create(billItem);
        return pbi;
    }
}
