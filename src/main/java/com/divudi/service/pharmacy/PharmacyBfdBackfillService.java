package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFinanceDetailsFacade;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.core.util.CommonFunctions;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * EJB service that backfills missing or zeroed BillFinanceDetails for
 * historical PHARMACY_STOCK_ADJUSTMENT and PHARMACY_RETAIL_RATE_ADJUSTMENT
 * bills.
 *
 * These bill types were saved without BFD records (or with bill.total = 0)
 * before the 2026-02-23 fix. This service reconstructs the BFD values from
 * the PharmaceuticalBillItem records that were correctly persisted.
 */
@Stateless
public class PharmacyBfdBackfillService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillFinanceDetailsFacade billFinanceDetailsFacade;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    /**
     * Backfills BillFinanceDetails for adjustment bills that are missing BFD
     * records or have bill.total = 0 with zeroed BFD values.
     *
     * Supported bill types:
     *   - PHARMACY_STOCK_ADJUSTMENT
     *   - PHARMACY_RETAIL_RATE_ADJUSTMENT
     *
     * @param billTypeAtomics list of bill types to backfill (null = both types)
     * @param departmentId    department ID filter (null = all departments)
     * @param fromDate        start date (inclusive)
     * @param toDate          end date (inclusive)
     * @param auditComment    audit trail comment
     * @param approvedBy      approver name
     * @param apiUser         the API user performing the backfill
     * @return summary map with backfilledBills, skipped, errors
     */
    public Map<String, Object> backfillAdjustmentBfds(
            List<String> billTypeAtomics,
            Long departmentId,
            Date fromDate,
            Date toDate,
            String auditComment,
            String approvedBy,
            WebUser apiUser) {

        List<BillTypeAtomic> types = resolveTypes(billTypeAtomics);

        // Ensure fromDate starts at 00:00:00.000 and toDate ends at 23:59:59.999
        // so the BETWEEN clause covers the full day when only a date string was given.
        Date from = CommonFunctions.getStartOfDay(fromDate);
        Date to = CommonFunctions.getEndOfDay(toDate);

        // Find bills that either:
        //  - have no BFD at all (PHARMACY_STOCK_ADJUSTMENT bills before the fix), or
        //  - have bill.total = 0 (PHARMACY_RETAIL_RATE_ADJUSTMENT bills where total was not persisted)
        // We use a LEFT JOIN so bills with no BFD are included (bfd IS NULL).
        // Use TemporalType.TIMESTAMP so the full datetime is compared, not just the date portion.
        String jpql = "SELECT b FROM Bill b LEFT JOIN b.billFinanceDetails bfd"
                + " WHERE b.retired = false"
                + " AND b.billTypeAtomic IN :types"
                + " AND b.createdAt BETWEEN :from AND :to"
                + " AND (bfd IS NULL OR b.total = 0)"
                + (departmentId != null ? " AND b.department.id = :deptId" : "");

        Map<String, Object> params = new HashMap<>();
        params.put("types", types);
        params.put("from", from);
        params.put("to", to);
        if (departmentId != null) {
            params.put("deptId", departmentId);
        }

        List<Bill> bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        int backfilled = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (Bill bill : bills) {
            try {
                boolean changed = backfillBill(bill, auditComment, approvedBy, apiUser);
                if (changed) {
                    backfilled++;
                } else {
                    skipped++;
                }
            } catch (Exception ex) {
                errors.add("Bill " + bill.getId() + ": " + ex.getMessage());
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("backfilledBills", backfilled);
        summary.put("skipped", skipped);
        summary.put("errors", errors);
        return summary;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean backfillBill(Bill bill, String auditComment, String approvedBy, WebUser apiUser) {
        // Load pharmaceutical bill items for this bill
        String pbiJpql = "SELECT pbi FROM PharmaceuticalBillItem pbi"
                + " JOIN pbi.billItem bi"
                + " WHERE bi.bill = :bill AND pbi.retired = false";
        List<PharmaceuticalBillItem> pbis = pharmaceuticalBillItemFacade.findByJpql(
                pbiJpql, Map.of("bill", bill));

        if (pbis == null || pbis.isEmpty()) {
            return false; // nothing to compute
        }

        BigDecimal totalRetailValue = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        BillTypeAtomic bta = bill.getBillTypeAtomic();

        for (PharmaceuticalBillItem pbi : pbis) {
            if (bta == BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT) {
                // pbi.qty = changingQty (signed), pbi.retailRate = rate at adjustment time
                double changingQty = pbi.getQty();
                double retailRate = pbi.getRetailRate();
                double costRate = pbi.getCostRate();

                double retailChange = changingQty * retailRate;
                double costChange = changingQty * costRate;

                totalRetailValue = totalRetailValue.add(BigDecimal.valueOf(retailChange));
                totalCostValue = totalCostValue.add(BigDecimal.valueOf(costChange));
                totalGross = totalGross.add(BigDecimal.valueOf(Math.abs(retailChange)));
                totalQty = totalQty.add(BigDecimal.valueOf(Math.abs(changingQty)));

            } else if (bta == BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT) {
                // pbi.beforeAdjustmentValue = oldRetailRate
                // pbi.afterAdjustmentValue  = newRetailRate
                // bi.qty                    = stockQty (units on hand at time of adjustment)
                double oldRate = pbi.getBeforeAdjustmentValue();
                double newRate = pbi.getAfterAdjustmentValue();
                BillItem bi = pbi.getBillItem();
                double stockQty = (bi != null) ? Math.abs(bi.getQty()) : 0.0;

                double delta = (newRate - oldRate) * stockQty;

                totalRetailValue = totalRetailValue.add(BigDecimal.valueOf(delta));
                totalGross = totalGross.add(BigDecimal.valueOf(Math.abs(delta)));
                totalQty = totalQty.add(BigDecimal.valueOf(stockQty));
            }
        }

        if (totalGross.compareTo(BigDecimal.ZERO) == 0) {
            return false; // computed values are still zero, skip
        }

        // Create or update BFD
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        boolean isNew = (bfd == null);
        if (isNew) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }

        bfd.setGrossTotal(totalGross);
        bfd.setNetTotal(totalRetailValue);
        bfd.setTotalRetailSaleValue(totalRetailValue);
        bfd.setTotalCostValue(totalCostValue);
        if (bfd.getTotalPurchaseValue() == null) {
            bfd.setTotalPurchaseValue(BigDecimal.ZERO);
        }
        if (bfd.getTotalWholesaleValue() == null) {
            bfd.setTotalWholesaleValue(BigDecimal.ZERO);
        }
        bfd.setTotalQuantity(totalQty);

        // Also update bill.total and bill.netTotal for consistent reporting
        bill.setTotal(totalGross.doubleValue());
        bill.setNetTotal(totalRetailValue.doubleValue());

        // Append audit log to bill comments
        appendAuditLog(bill, auditComment, approvedBy, apiUser, isNew, totalGross, totalRetailValue);

        if (isNew) {
            billFinanceDetailsFacade.create(bfd);
        } else {
            billFinanceDetailsFacade.edit(bfd);
        }
        billFacade.edit(bill);

        return true;
    }

    private List<BillTypeAtomic> resolveTypes(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) {
            return Arrays.asList(
                    BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT,
                    BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT);
        }
        List<BillTypeAtomic> result = new ArrayList<>();
        for (String name : typeNames) {
            try {
                BillTypeAtomic bta = BillTypeAtomic.valueOf(name.trim());
                if (bta != BillTypeAtomic.PHARMACY_STOCK_ADJUSTMENT
                        && bta != BillTypeAtomic.PHARMACY_RETAIL_RATE_ADJUSTMENT) {
                    throw new IllegalArgumentException(
                            "Unsupported billTypeAtomic for backfill: " + name
                            + ". Only PHARMACY_STOCK_ADJUSTMENT and PHARMACY_RETAIL_RATE_ADJUSTMENT are supported.");
                }
                result.add(bta);
            } catch (IllegalArgumentException ex) {
                if (ex.getMessage().startsWith("Unsupported")) {
                    throw ex;
                }
                throw new IllegalArgumentException("Unknown billTypeAtomic: " + name);
            }
        }
        return result;
    }

    private void appendAuditLog(Bill bill, String auditComment, String approvedBy, WebUser apiUser,
            boolean isNew, BigDecimal grossTotal, BigDecimal netTotal) {
        String existing = bill.getComments();
        String correctedBy = (apiUser != null) ? apiUser.getName() : "Unknown API User";
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        StringBuilder sb = new StringBuilder();
        if (existing != null && !existing.trim().isEmpty()) {
            sb.append(existing.trim()).append("\n\n");
        }
        sb.append("[BFD Backfill]")
                .append("\nTime: ").append(now)
                .append("\nBillType: ").append(bill.getBillTypeAtomic())
                .append("\nAction: ").append(isNew ? "Created new BFD" : "Updated existing BFD")
                .append("\nGrossTotal: ").append(grossTotal)
                .append("\nNetTotal: ").append(netTotal)
                .append("\nPerformedByApiUser: ").append(correctedBy)
                .append("\nApprovedBy: ").append(approvedBy)
                .append("\nAuditComment: ").append(auditComment);

        bill.setComments(sb.toString());
    }
}
