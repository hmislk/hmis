package com.divudi.service.pharmacy;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFinanceDetailsFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.BillItemFinanceDetailsFacade;
import com.divudi.core.util.CommonFunctions;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 * EJB service that backfills missing BillItemFinanceDetails (BIFD) and
 * BillFinanceDetails (BFD) for historical pharmacy stock transfer bills
 * (issue: PHARMACY_ISSUE / PharmacyTransferIssue, receive: PHARMACY_RECEIVE /
 * PharmacyTransferReceive).
 *
 * Why (issue #24721): transfers saved before finance details existed have no
 * BFD and/or no BIFD on their items, so the Stock Transfer Report, which reads
 * those rows, shows 0. Everything needed is on the PharmaceuticalBillItem
 * (pbi) that was persisted at transfer time.
 *
 * Sign conventions (same as the live writers, TransferIssueNativeSqlService /
 * TransferReceiveNativeSqlService):
 *   - pbi.qty is in units and signed: negative on issue, positive on receive.
 *   - BFD totalPurchaseValue / totalRetailSaleValue / totalCostValue =
 *     sum(rate x signed pbi.qty): NEGATIVE on issue, POSITIVE on receive.
 *   - BIFD valueAtPurchaseRate / valueAtRetailRate / valueAtCostRate are
 *     POSITIVE for both. BIFD quantity / quantityByUnits are signed like
 *     pbi.qty.
 *
 * Rate sources per item: purchase = pbi.purchaseRate if > 0, else the
 * ItemBatch purchase rate, else 0; retail = pbi.retailRate if > 0, else the
 * ItemBatch retail rate, else 0. pbi.costRate is always 0 historically, so
 * cost = ItemBatch cost rate when present and > 0, otherwise the purchase rate
 * (counted in costFallbackItems and written to the audit comment).
 *
 * "Missing" includes rows that exist but are empty: Bill.getBillFinanceDetails()
 * and BillItem.getBillItemFinanceDetails() auto-create an instance when the field
 * is null, so code that calls them on old bills can leave BFD/BIFD rows whose
 * value fields are all null. The service creates rows that are missing and fills
 * only the null fields of rows that exist (UPDATE ... SET x = COALESCE(x, :v)),
 * so a stored value is never overwritten and re-running it changes nothing.
 *
 * Performance: Bill and BillItem carry large eager graphs, and loading them made
 * a two-week run take over 20 minutes. This service therefore never loads those
 * entities. It reads scalar JPQL projections, persists only new, empty BFD/BIFD
 * rows, and links and fills them with JPQL bulk updates (which also invalidate
 * the affected entities in the EclipseLink shared cache).
 *
 * Transaction note: one call is one transaction. Persistence failures are not
 * caught per bill: catching them would mark the transaction rollback-only while
 * the summary still reported success (see PharmacyBfdBackfillService).
 *
 * dryRun = true computes the same summary and writes nothing.
 */
@Stateless
public class PharmacyTransferBifdBackfillService {

    @EJB
    private BillFacade billFacade;

    @EJB
    private BillItemFacade billItemFacade;

    @EJB
    private BillFinanceDetailsFacade billFinanceDetailsFacade;

    @EJB
    private BillItemFinanceDetailsFacade billItemFinanceDetailsFacade;

    /** Bill types this service can process; also the default when none given. */
    public static final List<BillTypeAtomic> SUPPORTED_TYPES = Collections.unmodifiableList(Arrays.asList(
            BillTypeAtomic.PHARMACY_ISSUE,
            BillTypeAtomic.PHARMACY_RECEIVE
    ));

    /** Bills per item-projection query, to keep the IN list bounded. */
    private static final int CHUNK = 500;

    /**
     * Backfills BIFD/BFD for pharmacy transfer issue/receive bills.
     *
     * @param types        bill types to process (null/empty = SUPPORTED_TYPES;
     *                     caller validates)
     * @param departmentId optional department filter (null = all departments)
     * @param fromDate     start date (inclusive)
     * @param toDate       end date (inclusive)
     * @param auditComment audit trail comment appended to bill.comments
     * @param approvedBy   name of the approver
     * @param user         WebUser performing the backfill
     * @param dryRun       true = compute and report only, persist nothing
     * @return summary map (totalBillsFound, processedBills, skippedBills,
     *         createdBifds, filledBifds, createdBfds, filledBfds, skippedItems,
     *         costFallbackItems, totalPurchaseValue, totalRetailValue,
     *         totalCostValue, dryRun, errors)
     */
    public Map<String, Object> backfillTransferBifds(
            List<BillTypeAtomic> types,
            Long departmentId,
            Date fromDate,
            Date toDate,
            String auditComment,
            String approvedBy,
            WebUser user,
            boolean dryRun) {

        List<BillTypeAtomic> useTypes = (types == null || types.isEmpty())
                ? new ArrayList<>(SUPPORTED_TYPES) : types;

        Map<Long, BillRow> bills = findBillsNeedingWork(useTypes, departmentId,
                CommonFunctions.getStartOfDay(fromDate), CommonFunctions.getEndOfDay(toDate));
        loadItems(bills);

        int processedBills = 0;
        int skippedBills = 0;
        int createdBifds = 0;
        int filledBifds = 0;
        int createdBfds = 0;
        int filledBfds = 0;
        int skippedItems = 0;
        int costFallbackItems = 0;
        double totalPurchase = 0.0;
        double totalRetail = 0.0;
        double totalCost = 0.0;

        for (BillRow bill : bills.values()) {
            skippedItems += bill.skippedItems;
            if (!computeBill(bill)) {
                skippedBills++;
                continue;
            }
            processedBills++;
            createdBfds += bill.bfdId == null ? 1 : 0;
            filledBfds += bill.bfdId != null && bill.bfdIncomplete ? 1 : 0;
            for (ItemRow item : bill.items) {
                if (item.needsWrite) {
                    if (item.bifdId == null) {
                        createdBifds++;
                    } else {
                        filledBifds++;
                    }
                    if (item.costFallback) {
                        costFallbackItems++;
                    }
                }
            }
            totalPurchase += bill.purchaseSum;
            totalRetail += bill.retailSum;
            totalCost += bill.costSum;

            if (!dryRun) {
                writeBill(bill, auditComment, approvedBy, user);
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBillsFound", bills.size());
        summary.put("processedBills", processedBills);
        summary.put("skippedBills", skippedBills);
        summary.put("createdBifds", createdBifds);
        summary.put("filledBifds", filledBifds);
        summary.put("createdBfds", createdBfds);
        summary.put("filledBfds", filledBfds);
        summary.put("skippedItems", skippedItems);
        summary.put("costFallbackItems", costFallbackItems);
        summary.put("totalPurchaseValue", totalPurchase);
        summary.put("totalRetailValue", totalRetail);
        summary.put("totalCostValue", totalCost);
        summary.put("dryRun", dryRun);
        summary.put("errors", new ArrayList<String>());
        return summary;
    }

    // -------------------------------------------------------------------------
    // Read (scalar projections only; no Bill/BillItem entities are loaded)
    // -------------------------------------------------------------------------

    /** One bill's projected data and computed totals. */
    private static class BillRow {

        Long id;
        BillTypeAtomic type;
        String comments;
        Long bfdId;
        boolean bfdIncomplete;
        List<ItemRow> items = new ArrayList<>();
        int skippedItems;
        double purchaseSum;
        double retailSum;
        double costSum;
        int costFallbackItems;
    }

    /** One bill item's projected data and computed values. */
    private static class ItemRow {

        Long id;
        Long bifdId;
        boolean needsWrite;
        boolean costFallback;
        double signedQty;
        double unitsPerPack;
        double purchaseRate;
        double retailRate;
        double costRate;
        double purchaseValue;
        double retailValue;
        double costValue;
        double lineRate;
        double lineTotal;
    }

    private Map<Long, BillRow> findBillsNeedingWork(List<BillTypeAtomic> types, Long departmentId,
            Date from, Date to) {
        // BFD missing or with null totals, or a non-retired item whose BIFD is
        // missing or has null values. LEFT JOINs keep the null rows.
        String jpql = "SELECT b.id, b.billTypeAtomic, b.comments, bfd.id,"
                + " bfd.totalPurchaseValue, bfd.totalRetailSaleValue, bfd.totalCostValue"
                + " FROM Bill b LEFT JOIN b.billFinanceDetails bfd"
                + " WHERE b.retired = false"
                + " AND b.billTypeAtomic IN :types"
                + " AND b.billType IN :billTypes"
                + " AND b.createdAt BETWEEN :from AND :to"
                + (departmentId != null ? " AND b.department.id = :deptId" : "")
                + " AND (bfd IS NULL OR bfd.totalPurchaseValue IS NULL"
                + " OR bfd.totalRetailSaleValue IS NULL OR bfd.totalCostValue IS NULL"
                + " OR EXISTS (SELECT bi FROM BillItem bi LEFT JOIN bi.billItemFinanceDetails f"
                + " WHERE bi.bill = b AND bi.retired = false"
                + " AND (f IS NULL OR f.valueAtPurchaseRate IS NULL"
                + " OR f.valueAtRetailRate IS NULL OR f.valueAtCostRate IS NULL)))"
                + " ORDER BY b.id";

        Map<String, Object> params = new HashMap<>();
        params.put("types", types);
        params.put("billTypes", Arrays.asList(BillType.PharmacyTransferIssue, BillType.PharmacyTransferReceive));
        params.put("from", from);
        params.put("to", to);
        if (departmentId != null) {
            params.put("deptId", departmentId);
        }

        Map<Long, BillRow> bills = new LinkedHashMap<>();
        for (Object[] r : billFacade.findAggregates(jpql, params, TemporalType.TIMESTAMP)) {
            BillRow b = new BillRow();
            b.id = (Long) r[0];
            b.type = (BillTypeAtomic) r[1];
            b.comments = (String) r[2];
            b.bfdId = (Long) r[3];
            b.bfdIncomplete = b.bfdId == null || r[4] == null || r[5] == null || r[6] == null;
            bills.put(b.id, b);
        }
        return bills;
    }

    private void loadItems(Map<Long, BillRow> bills) {
        List<Long> ids = new ArrayList<>(bills.keySet());
        for (int start = 0; start < ids.size(); start += CHUNK) {
            List<Long> chunk = ids.subList(start, Math.min(ids.size(), start + CHUNK));
            String jpql = "SELECT bi.id, b.id, pbi.id, pbi.qty, pbi.purchaseRate, pbi.retailRate,"
                    + " ib.purcahseRate, ib.retailsaleRate, ib.costRate,"
                    + " TYPE(i), i.dblValue, bi.Rate, bi.netValue,"
                    + " f.id, f.valueAtPurchaseRate, f.valueAtRetailRate, f.valueAtCostRate"
                    + " FROM BillItem bi JOIN bi.bill b"
                    + " LEFT JOIN bi.pharmaceuticalBillItem pbi"
                    + " LEFT JOIN pbi.itemBatch ib"
                    + " LEFT JOIN bi.item i"
                    + " LEFT JOIN bi.billItemFinanceDetails f"
                    + " WHERE b.id IN :ids AND bi.retired = false";
            Map<String, Object> params = new HashMap<>();
            params.put("ids", chunk);
            for (Object[] r : billItemFacade.findAggregates(jpql, params)) {
                BillRow bill = bills.get((Long) r[1]);
                if (bill == null) {
                    continue;
                }
                if (r[2] == null) {
                    bill.skippedItems++;
                    continue;
                }
                bill.items.add(toItemRow(r));
            }
        }
    }

    private ItemRow toItemRow(Object[] r) {
        ItemRow c = new ItemRow();
        c.id = (Long) r[0];
        double pbiQty = num(r[3]);
        double pbiPurchase = num(r[4]);
        double pbiRetail = num(r[5]);
        boolean hasBatch = r[6] != null;
        Double batchCost = r[8] == null ? null : num(r[8]);

        c.purchaseRate = pbiPurchase > 0 ? pbiPurchase : (hasBatch ? num(r[6]) : 0.0);
        c.retailRate = pbiRetail > 0 ? pbiRetail : (hasBatch ? num(r[7]) : 0.0);
        c.costFallback = batchCost == null || batchCost <= 0;
        c.costRate = c.costFallback ? c.purchaseRate : batchCost;

        c.unitsPerPack = 1.0;
        if (r[9] != null && Ampp.class.isAssignableFrom((Class<?>) r[9]) && num(r[10]) > 0) {
            c.unitsPerPack = num(r[10]);
        }
        c.signedQty = pbiQty;
        double units = Math.abs(pbiQty);
        c.purchaseValue = c.purchaseRate * units;
        c.retailValue = c.retailRate * units;
        c.costValue = c.costRate * units;
        c.lineRate = num(r[11]) != 0 ? num(r[11]) : c.purchaseRate;
        c.lineTotal = num(r[12]) != 0 ? Math.abs(num(r[12])) : c.purchaseValue;

        c.bifdId = (Long) r[13];
        c.needsWrite = c.bifdId == null || r[14] == null || r[15] == null || r[16] == null;
        // Values already stored on the BIFD win over recomputed ones.
        if (r[14] != null) {
            c.purchaseValue = Math.abs(num(r[14]));
        }
        if (r[15] != null) {
            c.retailValue = Math.abs(num(r[15]));
        }
        if (r[16] != null) {
            c.costValue = Math.abs(num(r[16]));
        }
        return c;
    }

    private static double num(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    /** Sums item values; returns true when the bill has anything to write. */
    private boolean computeBill(BillRow bill) {
        if (bill.type == null || bill.items.isEmpty()) {
            return false;
        }
        boolean anyItem = false;
        for (ItemRow item : bill.items) {
            bill.purchaseSum += item.purchaseValue;
            bill.retailSum += item.retailValue;
            bill.costSum += item.costValue;
            if (item.needsWrite) {
                anyItem = true;
                if (item.costFallback) {
                    bill.costFallbackItems++;
                }
            }
        }
        return anyItem || bill.bfdIncomplete;
    }

    // -------------------------------------------------------------------------
    // Write (new rows are persisted empty, then linked and filled by JPQL updates)
    // -------------------------------------------------------------------------

    private void writeBill(BillRow bill, String auditComment, String approvedBy, WebUser user) {
        // Issue = stock out (negative), receive = stock in (positive)
        double sign = (bill.type == BillTypeAtomic.PHARMACY_ISSUE) ? -1.0 : 1.0;
        int created = 0;
        int filled = 0;

        for (ItemRow item : bill.items) {
            if (!item.needsWrite) {
                continue;
            }
            Long bifdId = item.bifdId;
            if (bifdId == null) {
                BillItemFinanceDetails f = new BillItemFinanceDetails();
                f.setCreatedAt(new Date());
                f.setUnitsPerPack(BigDecimal.valueOf(item.unitsPerPack));
                billItemFinanceDetailsFacade.create(f);
                billItemFinanceDetailsFacade.flush();
                Map<String, Object> link = new HashMap<>();
                link.put("f", f);
                link.put("id", item.id);
                billItemFacade.updateByJpql(
                        "UPDATE BillItem bi SET bi.billItemFinanceDetails = :f WHERE bi.id = :id", link);
                bifdId = f.getId();
                created++;
            } else {
                filled++;
            }
            fillBifd(bifdId, item);
        }

        Long bfdId = bill.bfdId;
        boolean newBfd = bfdId == null;
        if (newBfd) {
            BillFinanceDetails bfd = new BillFinanceDetails();
            billFinanceDetailsFacade.create(bfd);
            billFinanceDetailsFacade.flush();
            Map<String, Object> link = new HashMap<>();
            link.put("bfd", bfd);
            link.put("id", bill.id);
            billFacade.updateByJpql("UPDATE Bill b SET b.billFinanceDetails = :bfd WHERE b.id = :id", link);
            bfdId = bfd.getId();
        }
        // Signed totals: issue negative, receive positive; only where null
        Map<String, Object> p = new HashMap<>();
        p.put("id", bfdId);
        p.put("pv", BigDecimal.valueOf(sign * bill.purchaseSum));
        p.put("rv", BigDecimal.valueOf(sign * bill.retailSum));
        p.put("cv", BigDecimal.valueOf(sign * bill.costSum));
        billFinanceDetailsFacade.updateByJpql("UPDATE BillFinanceDetails x SET"
                + " x.totalPurchaseValue = COALESCE(x.totalPurchaseValue, :pv),"
                + " x.totalRetailSaleValue = COALESCE(x.totalRetailSaleValue, :rv),"
                + " x.totalCostValue = COALESCE(x.totalCostValue, :cv)"
                + " WHERE x.id = :id", p);

        Map<String, Object> c = new HashMap<>();
        c.put("id", bill.id);
        c.put("c", auditLog(bill, auditComment, approvedBy, user, newBfd, created, filled, sign));
        billFacade.updateByJpql("UPDATE Bill b SET b.comments = :c WHERE b.id = :id", c);
    }

    /** Sets only the null fields of one BIFD. */
    private void fillBifd(Long bifdId, ItemRow item) {
        BigDecimal qtyPacks = BigDecimal.valueOf(item.signedQty / item.unitsPerPack);
        Map<String, Object> p = new HashMap<>();
        p.put("id", bifdId);
        p.put("qp", qtyPacks);
        p.put("qu", BigDecimal.valueOf(item.signedQty));
        p.put("pr", BigDecimal.valueOf(item.purchaseRate));
        p.put("rr", BigDecimal.valueOf(item.retailRate));
        p.put("cr", BigDecimal.valueOf(item.costRate));
        p.put("pv", BigDecimal.valueOf(item.purchaseValue));
        p.put("rv", BigDecimal.valueOf(item.retailValue));
        p.put("cv", BigDecimal.valueOf(item.costValue));
        p.put("lr", BigDecimal.valueOf(item.lineRate));
        p.put("lt", BigDecimal.valueOf(item.lineTotal));
        billItemFinanceDetailsFacade.updateByJpql("UPDATE BillItemFinanceDetails f SET"
                + " f.quantity = COALESCE(f.quantity, :qp),"
                + " f.totalQuantity = COALESCE(f.totalQuantity, :qp),"
                + " f.quantityByUnits = COALESCE(f.quantityByUnits, :qu),"
                + " f.purchaseRate = COALESCE(f.purchaseRate, :pr),"
                + " f.retailSaleRate = COALESCE(f.retailSaleRate, :rr),"
                + " f.retailSaleRatePerUnit = COALESCE(f.retailSaleRatePerUnit, :rr),"
                + " f.costRate = COALESCE(f.costRate, :cr),"
                + " f.lineCostRate = COALESCE(f.lineCostRate, :cr),"
                + " f.totalCostRate = COALESCE(f.totalCostRate, :cr),"
                + " f.valueAtPurchaseRate = COALESCE(f.valueAtPurchaseRate, :pv),"
                + " f.valueAtRetailRate = COALESCE(f.valueAtRetailRate, :rv),"
                + " f.valueAtCostRate = COALESCE(f.valueAtCostRate, :cv),"
                + " f.lineCost = COALESCE(f.lineCost, :cv),"
                + " f.totalCost = COALESCE(f.totalCost, :cv),"
                + " f.lineGrossRate = COALESCE(f.lineGrossRate, :lr),"
                + " f.lineNetRate = COALESCE(f.lineNetRate, :lr),"
                + " f.grossRate = COALESCE(f.grossRate, :lr),"
                + " f.lineGrossTotal = COALESCE(f.lineGrossTotal, :lt),"
                + " f.lineNetTotal = COALESCE(f.lineNetTotal, :lt),"
                + " f.netTotal = COALESCE(f.netTotal, :lt)"
                + " WHERE f.id = :id", p);
    }

    private String auditLog(BillRow bill, String auditComment, String approvedBy, WebUser user,
            boolean newBfd, int createdBifds, int filledBifds, double sign) {
        String performer = (user != null) ? user.getName() : "API";
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        StringBuilder sb = new StringBuilder();
        if (bill.comments != null && !bill.comments.trim().isEmpty()) {
            sb.append(bill.comments.trim()).append("\n\n");
        }
        sb.append("[Transfer BIFD Backfill]")
                .append("\nTime: ").append(now)
                .append("\nBillType: ").append(bill.type)
                .append("\nAction: ").append(newBfd ? "Created BFD"
                        : bill.bfdIncomplete ? "Filled BFD" : "BFD unchanged")
                .append(", created ").append(createdBifds).append(" BIFD(s)")
                .append(", filled ").append(filledBifds).append(" empty BIFD(s)")
                .append("\nSaleValue: ").append(sign * bill.retailSum)
                .append("\nPurchaseValue: ").append(sign * bill.purchaseSum)
                .append("\nCostValue: ").append(sign * bill.costSum);
        if (bill.costFallbackItems > 0) {
            sb.append("\nNote: Cost fell back to purchase rate for ")
                    .append(bill.costFallbackItems).append(" item(s)");
        }
        sb.append("\nPerformedBy: ").append(performer)
                .append("\nApprovedBy: ").append(approvedBy)
                .append("\nComment: ").append(auditComment);
        return sb.toString();
    }
}
