package com.divudi.service.pharmacy;

import com.divudi.core.data.BillCategory;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFinanceDetailsFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillItemFinanceDetailsFacade;
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
 * EJB service that backfills missing BillItemFinanceDetails (BIFD) and
 * BillFinanceDetails (BFD) for historical Pharmacy GRN bills.
 *
 * GRN bills saved before the BillItemFinanceDetails system was introduced have
 * null billItemFinanceDetails on their BillItems. The GRN reprint view reads
 * from BIFD (lineGrossRate, lineNetTotal, totalCost, retailSaleRate, etc.), so
 * those columns appear blank until BIFD records are populated.
 *
 * This service reconstructs all BIFD/BFD values from the
 * PharmaceuticalBillItem records that were correctly persisted at GRN creation
 * time.
 *
 * Supported bill types (by default):
 *   PHARMACY_GRN, PHARMACY_GRN_CANCELLED, PHARMACY_GRN_REFUND,
 *   PHARMACY_GRN_RETURN, PHARMACY_GRN_WHOLESALE,
 *   PHARMACY_DIRECT_PURCHASE, PHARMACY_DIRECT_PURCHASE_CANCELLED
 *
 * The service is safe to re-run: it checks each BIFD field individually and
 * only writes values that are still null, so already-corrected records are not
 * overwritten.
 */
@Stateless
public class PharmacyGrnBifdBackfillService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private BillFinanceDetailsFacade billFinanceDetailsFacade;

    @EJB
    private BillItemFinanceDetailsFacade billItemFinanceDetailsFacade;

    // Default GRN bill types – mirrors getPharmacyProcurementBillTypes()
    private static final List<BillTypeAtomic> DEFAULT_TYPES = Arrays.asList(
            BillTypeAtomic.PHARMACY_GRN,
            BillTypeAtomic.PHARMACY_GRN_CANCELLED,
            BillTypeAtomic.PHARMACY_GRN_REFUND,
            BillTypeAtomic.PHARMACY_GRN_RETURN,
            BillTypeAtomic.PHARMACY_GRN_WHOLESALE,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
            BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED
    );

    /**
     * Backfills BIFD/BFD for GRN bills whose bill items have null
     * BillItemFinanceDetails.
     *
     * @param billTypeAtomics list of bill type atomic names to process (null =
     *                        all default GRN types)
     * @param departmentId    optional department filter (null = all departments)
     * @param fromDate        start date (inclusive)
     * @param toDate          end date (inclusive)
     * @param auditComment    audit trail comment appended to bill.comments
     * @param approvedBy      name of the approver
     * @param apiUser         WebUser performing the backfill
     * @return summary map with processedBills, skippedBills, processedItems,
     *         skippedItems, errors
     */
    public Map<String, Object> backfillGrnBifds(
            List<String> billTypeAtomics,
            Long departmentId,
            Date fromDate,
            Date toDate,
            String auditComment,
            String approvedBy,
            WebUser apiUser) {

        List<BillTypeAtomic> types = resolveTypes(billTypeAtomics);

        Date from = CommonFunctions.getStartOfDay(fromDate);
        Date to = CommonFunctions.getEndOfDay(toDate);

        // Find GRN bills that have at least one BillItem with null BIFD.
        // We use a subquery to target only bills that still need work so the
        // query set stays small when re-run after a partial correction.
        String jpql = "SELECT DISTINCT b FROM Bill b"
                + " JOIN b.billItems bi"
                + " WHERE b.retired = false"
                + " AND b.billTypeAtomic IN :types"
                + " AND b.createdAt BETWEEN :from AND :to"
                + " AND bi.billItemFinanceDetails IS NULL"
                + (departmentId != null ? " AND b.department.id = :deptId" : "");

        Map<String, Object> params = new HashMap<>();
        params.put("types", types);
        params.put("from", from);
        params.put("to", to);
        if (departmentId != null) {
            params.put("deptId", departmentId);
        }

        List<Bill> bills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);

        int processedBills = 0;
        int skippedBills = 0;
        int processedItems = 0;
        int skippedItems = 0;
        List<String> errors = new ArrayList<>();

        for (Bill bill : bills) {
            try {
                int[] counts = backfillBill(bill, auditComment, approvedBy, apiUser);
                if (counts[0] > 0) {
                    processedBills++;
                    processedItems += counts[0];
                    skippedItems += counts[1];
                } else {
                    skippedBills++;
                    skippedItems += counts[1];
                }
            } catch (Exception ex) {
                errors.add("Bill " + bill.getId() + " (" + bill.getDeptId() + "): " + ex.getMessage());
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("processedBills", processedBills);
        summary.put("skippedBills", skippedBills);
        summary.put("processedItems", processedItems);
        summary.put("skippedItems", skippedItems);
        summary.put("totalBillsFound", bills.size());
        summary.put("errors", errors);
        return summary;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Backfills one bill. Returns int[]{processedItems, skippedItems}.
     */
    private int[] backfillBill(Bill bill, String auditComment, String approvedBy, WebUser apiUser) {
        BillTypeAtomic bta = bill.getBillTypeAtomic();
        if (bta == null || bta.getBillCategory() == null) {
            return new int[]{0, 0};
        }
        BillCategory bc = bta.getBillCategory();

        List<BillItem> items = bill.getBillItems();
        if (items == null || items.isEmpty()) {
            return new int[]{0, 0};
        }

        // For GRNs: CANCELLATION/REFUND = money back (positive sign)
        //           BILL (normal GRN)   = expense, stock in (negative sign per system convention)
        double factor = (bc == BillCategory.CANCELLATION || bc == BillCategory.REFUND) ? 1.0 : -1.0;

        double saleValue = 0.0;
        double purchaseValue = 0.0;
        double costValue = 0.0;

        int processed = 0;
        int skipped = 0;

        for (BillItem bi : items) {
            PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
            if (pbi == null || pbi.getItemBatch() == null) {
                skipped++;
                continue;
            }

            double qty = Math.abs(pbi.getQty());
            double freeQty = Math.abs(pbi.getFreeQty());
            double totalQty = qty + freeQty;

            double retailRate = Math.abs(pbi.getRetailRate());
            double purchaseRate = Math.abs(pbi.getItemBatch().getPurcahseRate());
            double cRate = Math.abs(pbi.getItemBatch().getCostRate());

            double lineDiscount = Math.abs(bi.getDiscount());
            double biGrossValue = bi.getGrossValue();
            double biNetValue = bi.getNetValue();

            double itemSaleValue = factor * retailRate * qty;
            double itemPurchaseValue = factor * purchaseRate * qty;
            double itemCostValue = factor * cRate * totalQty;

            BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();
            boolean isNewBifd = (bifd == null);
            if (isNewBifd) {
                bifd = new BillItemFinanceDetails();
                bifd.setBillItem(bi);
                bi.setBillItemFinanceDetails(bifd);
            }

            // Only write fields that are still null (idempotent)
            if (bifd.getGrossRate() == null) {
                bifd.setGrossRate(BigDecimal.valueOf(purchaseRate));
            }
            if (bifd.getQuantity() == null) {
                bifd.setQuantity(BigDecimal.valueOf(qty));
            }
            if (bifd.getFreeQuantity() == null) {
                bifd.setFreeQuantity(BigDecimal.valueOf(freeQty));
            }
            if (bifd.getQuantityByUnits() == null) {
                bifd.setQuantityByUnits(BigDecimal.valueOf(qty));
            }
            if (bifd.getTotalQuantityByUnits() == null) {
                bifd.setTotalQuantityByUnits(BigDecimal.valueOf(totalQty));
            }
            if (bifd.getValueAtCostRate() == null) {
                bifd.setValueAtCostRate(BigDecimal.valueOf(itemCostValue));
            }
            if (bifd.getValueAtPurchaseRate() == null) {
                bifd.setValueAtPurchaseRate(BigDecimal.valueOf(itemPurchaseValue));
            }
            if (bifd.getValueAtRetailRate() == null) {
                bifd.setValueAtRetailRate(BigDecimal.valueOf(itemSaleValue));
            }
            if (bifd.getLineGrossRate() == null) {
                bifd.setLineGrossRate(BigDecimal.valueOf(purchaseRate));
            }
            if (bifd.getLineDiscountRate() == null) {
                double discountRate = (qty > 0) ? lineDiscount / qty : 0.0;
                bifd.setLineDiscountRate(BigDecimal.valueOf(discountRate));
            }
            if (bifd.getRetailSaleRate() == null) {
                bifd.setRetailSaleRate(BigDecimal.valueOf(retailRate));
            }
            if (bifd.getRetailSaleRatePerUnit() == null) {
                bifd.setRetailSaleRatePerUnit(BigDecimal.valueOf(retailRate));
            }
            if (bifd.getLineGrossTotal() == null) {
                double lineTotal = (Math.abs(biGrossValue) > 0.01) ? biGrossValue : factor * purchaseRate * qty;
                bifd.setLineGrossTotal(BigDecimal.valueOf(lineTotal));
            }
            if (bifd.getLineNetTotal() == null) {
                double lineNet = (Math.abs(biNetValue) > 0.01) ? biNetValue : factor * purchaseRate * qty;
                bifd.setLineNetTotal(BigDecimal.valueOf(lineNet));
            }
            if (bifd.getTotalCost() == null) {
                bifd.setTotalCost(BigDecimal.valueOf(Math.abs(itemCostValue)));
            }

            if (isNewBifd) {
                billItemFinanceDetailsFacade.create(bifd);
            } else {
                billItemFinanceDetailsFacade.edit(bifd);
            }
            billItemFacade.edit(bi);

            saleValue += Math.abs(itemSaleValue);
            purchaseValue += Math.abs(itemPurchaseValue);
            costValue += Math.abs(itemCostValue);
            processed++;
        }

        if (processed == 0) {
            return new int[]{0, skipped};
        }

        // Create or update BillFinanceDetails
        BillFinanceDetails bfd = bill.getBillFinanceDetails();
        boolean isNewBfd = (bfd == null);
        if (isNewBfd) {
            bfd = new BillFinanceDetails(bill);
            bill.setBillFinanceDetails(bfd);
        }

        bfd.setTotalCostValue(BigDecimal.valueOf(costValue));
        bfd.setTotalRetailSaleValue(BigDecimal.valueOf(saleValue));
        bfd.setTotalPurchaseValue(BigDecimal.valueOf(purchaseValue));
        if (bfd.getBillGrossTotal() == null) {
            bfd.setBillGrossTotal(BigDecimal.valueOf(purchaseValue));
        }

        appendAuditLog(bill, auditComment, approvedBy, apiUser, isNewBfd, saleValue, purchaseValue, costValue);

        if (isNewBfd) {
            billFinanceDetailsFacade.create(bfd);
        } else {
            billFinanceDetailsFacade.edit(bfd);
        }
        billFacade.edit(bill);

        return new int[]{processed, skipped};
    }

    private List<BillTypeAtomic> resolveTypes(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) {
            return new ArrayList<>(DEFAULT_TYPES);
        }
        List<BillTypeAtomic> result = new ArrayList<>();
        for (String name : typeNames) {
            BillTypeAtomic bta;
            try {
                bta = BillTypeAtomic.valueOf(name.trim());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unknown billTypeAtomic: " + name);
            }
            if (!DEFAULT_TYPES.contains(bta)) {
                throw new IllegalArgumentException(
                        "Unsupported billTypeAtomic for GRN backfill: " + name
                        + ". Supported: " + DEFAULT_TYPES);
            }
            result.add(bta);
        }
        return result;
    }

    private void appendAuditLog(Bill bill, String auditComment, String approvedBy,
            WebUser apiUser, boolean isNewBfd,
            double saleValue, double purchaseValue, double costValue) {
        String existing = bill.getComments();
        String performer = (apiUser != null) ? apiUser.getName() : "API";
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        StringBuilder sb = new StringBuilder();
        if (existing != null && !existing.trim().isEmpty()) {
            sb.append(existing.trim()).append("\n\n");
        }
        sb.append("[GRN BIFD Backfill]")
                .append("\nTime: ").append(now)
                .append("\nBillType: ").append(bill.getBillTypeAtomic())
                .append("\nAction: ").append(isNewBfd ? "Created BFD + BIFDs" : "Updated BFD + BIFDs")
                .append("\nSaleValue: ").append(saleValue)
                .append("\nPurchaseValue: ").append(purchaseValue)
                .append("\nCostValue: ").append(costValue)
                .append("\nPerformedBy: ").append(performer)
                .append("\nApprovedBy: ").append(approvedBy)
                .append("\nComment: ").append(auditComment);

        bill.setComments(sb.toString());
    }
}
