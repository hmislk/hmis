package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.HistoryType;
import com.divudi.core.data.dto.StockAggregateResult;
import com.divudi.core.data.dto.pharmacy.GrnApproveLineData;
import com.divudi.core.data.dto.pharmacy.GrnApproveRequest;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.ItemBatch;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.StockHistory;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Native SQL write path for the GRN (Goods Received Note) "Manage Costing =
 * true" Approve stage -- the highest-risk part of the #22874 conversion,
 * since this is where stock and ItemBatch rows actually get mutated.
 * Directly modeled on PurchaseOrderApprovingNativeSqlService (issue #22738)
 * for the bill-header-mutation rules, and on
 * TransferReceiveNativeSqlService (stock addition path) for the
 * findOrCreateDeptStock / computeAggregates / insertStockHistory pattern --
 * each duplicated here as its own private copy, per this codebase's
 * established convention that no shared helper class exists for these.
 *
 * Single public entry point, {@link #approveGrn(GrnApproveRequest)}, is one
 * {@code @Stateless} EJB method with the default REQUIRED transaction
 * attribute, so every write below -- including the {@code @EJB}-delegated
 * {@code updateBillTotals} call on {@link GrnCostingNativeSqlService}, since
 * EJB calls within the same call graph join the caller's transaction by
 * default -- commits atomically as one unit. That is the whole reason this
 * class exists as a separate {@code @Stateless} service rather than living on
 * the calling {@code @SessionScoped} GrnCostingController: the legacy
 * per-item loop in {@code approveGrnWithSaveApprove()} has no surrounding
 * transaction (each Facade edit/create call commits independently), so a
 * failure partway through the loop can leave stock added without the bill
 * ever reaching PHARMACY_GRN. This service closes that gap.
 *
 * Bill-header mutations (approveBillHeader, updateBalance,
 * markPurchaseOrderFullyIssued, retireOrphanPreBills, updateBillNumbers) are
 * deliberately native UPDATEs, never {@code billFacade.edit()}/
 * {@code em.merge()} of a detached Bill: {@code Bill.billItems} is
 * {@code cascade=ALL, orphanRemoval=true}, so merging a detached Bill entity
 * whose in-memory {@code billItems} collection doesn't include lines written
 * by native SQL elsewhere in this same approve flow would delete those lines
 * via orphan removal. Same rule, same reasoning as
 * {@code PurchaseOrderApprovingNativeSqlService.approveBill()}'s Javadoc.
 *
 * Related issue: #22874
 */
@Stateless
public class GrnApprovingNativeSqlService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private GrnCostingNativeSqlService grnCostingNativeSqlService;

    private String tBill;
    private String tBillItem;
    private String tPharmBillItem;
    private String tStock;
    private String tStockHistory;
    private String tDepartment;
    private String tItemBatch;

    // -----------------------------------------------------------------------
    // Public entry point
    // -----------------------------------------------------------------------

    public void approveGrn(GrnApproveRequest request) {
        // Claim the bill by flipping it out of PHARMACY_GRN_PRE atomically,
        // before any stock write. If no row changes, another approve call
        // already ran (double-click, or two sessions on the same saved GRN)
        // -- without this, the stock-mutating loop below would run twice,
        // doubling Stock/StockHistory rows and the PO remaining-qty
        // decrement, since approveBillHeader() unconditionally overwrites
        // BILLTYPEATOMIC rather than gating on it. Same technique already
        // used by markPurchaseOrderFullyIssued() below.
        int claimed = em.createNativeQuery(
                "UPDATE " + billTable() + " SET BILLTYPEATOMIC=? WHERE ID=? AND BILLTYPEATOMIC=?")
                .setParameter(1, BillTypeAtomic.PHARMACY_GRN.toString())
                .setParameter(2, request.getBillId())
                .setParameter(3, BillTypeAtomic.PHARMACY_GRN_PRE.toString())
                .executeUpdate();
        if (claimed == 0) {
            throw new IllegalStateException(
                    "GRN approve: bill " + request.getBillId() + " is not in PHARMACY_GRN_PRE state (already approved or invalid).");
        }

        updateBillNumbers(request.getBillId(), request.getDeptId(), request.getInsId());

        List<GrnApproveLineData> lines = request.getLines();
        if (lines != null) {
            for (GrnApproveLineData line : lines) {
                approveLine(request, line);
            }
        }

        approveBillHeader(request.getBillId(), request.getEditorId(), request.getApproveUserId());

        // Delegated @EJB call -- joins this same transaction (default
        // TransactionAttributeType.REQUIRED on both sides), so it commits or
        // rolls back atomically with everything else in this method.
        grnCostingNativeSqlService.updateBillTotals(
                request.getBillId(), request.getPostNegationNetTotal(), request.getPostNegationTotal());

        updateBalance(request.getBillId(), request.getBalanceValue());

        if (request.isGeneratePayment()) {
            insertPayment(request);
        }

        if (request.getPoBillId() != null) {
            if (request.isPoFullyReceived()) {
                markPurchaseOrderFullyIssued(request.getPoBillId(), request.getApproveUserId());
            }
            retireOrphanPreBills(request.getPoBillId(), request.getBillId(), request.getApproveUserId());
        }

        evictCache();
    }

    // -----------------------------------------------------------------------
    // Per-line processing
    // -----------------------------------------------------------------------

    private void approveLine(GrnApproveRequest request, GrnApproveLineData line) {
        updateApprovedLine(line);
        mergeBillItemFinanceDetails(line);

        // Resolve the AMP item for ItemBatch/Stock/StockHistory keying --
        // mirrors legacy saveItemBatchWithCosting()'s
        // "Item amp = originalItem instanceof Ampp ? ((Ampp) originalItem).getAmp() : originalItem".
        // GrnApproveLineData carries only the picked item's id (which may be
        // an Ampp), not a separate resolved-AMP id, so this is resolved here
        // via a small JPA read rather than adding a new DTO field.
        Item originalItem = em.find(Item.class, line.getItemId());
        if (originalItem == null) {
            throw new RuntimeException("GRN approve: item " + line.getItemId() + " not found for billItem "
                    + line.getBillItemId());
        }
        Item amp = (originalItem instanceof Ampp) ? originalItem.getAmp() : originalItem;
        if (amp == null || line.getExpiryDate() == null) {
            // Legacy's saveItemBatchWithCosting() silently returns null here and
            // lets the bill item end up with no ItemBatch/Stock link -- a latent
            // gap, not a deliberate rule. Failing loudly here instead of
            // corrupting the stock chain; controller-side validation
            // (errorCheck()) is expected to have already guaranteed a non-null
            // expiry before this native service is ever called. Flagged for
            // scrutiny during Task 8's Playwright/DB verification pass.
            throw new RuntimeException("GRN approve: missing AMP item or expiry date for billItem "
                    + line.getBillItemId() + " (itemId=" + line.getItemId() + ")");
        }

        long itemBatchId = findOrCreateItemBatch(line, amp);

        double addingQty = Math.abs(line.getQuantityByUnits()) + Math.abs(line.getFreeQuantityByUnits());
        long stockId = findOrCreateDeptStock(request.getDepartmentId(), itemBatchId, addingQty);

        updatePharmaceuticalBillItemStockLink(line.getPharmaceuticalBillItemId(), stockId, itemBatchId);

        double postAddQty = fetchStockQty(stockId);
        StockAggregateResult agg = computeAggregates(
                amp.getId(), itemBatchId, request.getDepartmentId(), request.getInstitutionId(),
                postAddQty, line.getRetailRatePerUnit(), line.getPurchaseRatePerUnit(), line.getCostRatePerUnit());

        insertStockHistory(line.getPharmaceuticalBillItemId(), line, agg, amp.getId(), itemBatchId,
                request.getDepartmentId(), request.getInstitutionId());

        if (line.getPoBillItemId() > 0) {
            decrementPoRemainingQty(line.getPoBillItemId(), line.getPoReceivedQty(), line.getPoReceivedFreeQty());
        }
    }

    /**
     * Native UPDATE writing the final approved qty/freeQty/rates/costRate
     * onto this GRN line's own billitem + pharmaceuticalbillitem rows --
     * the post-{@code applyFinanceDetailsToPharmaceutical(bi, true)} values,
     * already computed by the controller (which runs the equivalent
     * distribution/recalculation math) and carried on the DTO.
     *
     * The billitem qty/rate columns are unchanged from what
     * {@code GrnCostingNativeSqlService.saveLine()} already wrote at Save
     * time (verified against legacy: for both Ampp and non-Ampp items,
     * {@code applyFinanceDetailsToPharmaceutical}'s bi.setQty()/bi.setRate()
     * resolve to the same quantity/lineNetRate values saveLine() already
     * used), so this UPDATE only needs to re-assert them defensively.
     *
     * pharmaceuticalbillitem.costRate/costValue are the one genuinely new
     * write here: Save-stage's INSERT always hardcodes these to 0 (costing
     * requires the bill-level discount/expense distribution that only
     * happens once, right before Approve) -- see
     * {@code GrnCostingNativeSqlService.saveLine()}'s Javadoc-free INSERT.
     * purchaseRate/purchaseRatePack are also refreshed here to the
     * net-of-line-discount value ({@code GrnApproveLineData.purchaseRatePerUnit}
     * / {@code getLineNetRate()}), superseding Save-stage's gross-rate-based
     * figure -- matching
     * {@code recalculateFinancialsBeforeAddingBillItemPreservingDistributedCosts()}.
     */
    private void updateApprovedLine(GrnApproveLineData line) {
        em.createNativeQuery(
                "UPDATE " + billItemTable()
                + " SET qty=?, netValue=?, grossValue=?, Rate=?, netRate=?, discountRate=? WHERE ID=?")
                .setParameter(1, line.getQuantity())
                // billitem.netValue is negative for purchases -- same sign
                // convention/rationale as GrnCostingNativeSqlService.saveLine().
                .setParameter(2, -line.getLineNetTotal())
                .setParameter(3, line.getLineGrossTotal())
                .setParameter(4, line.getLineGrossRate())
                .setParameter(5, line.getLineNetRate())
                .setParameter(6, line.getLineDiscountRate())
                .setParameter(7, line.getBillItemId())
                .executeUpdate();

        double costValue = Math.abs(line.getQuantityByUnits()) * line.getCostRatePerUnit();

        em.createNativeQuery(
                "UPDATE " + pharmBillItemTable()
                + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
                + " retailRate=?, retailRatePack=?, retailValue=?, wholesaleRate=?, wholesaleRatePack=?,"
                + " costRate=?, costValue=? WHERE billItem_ID=?")
                .setParameter(1, line.getQuantityByUnits())
                .setParameter(2, line.getFreeQuantityByUnits())
                .setParameter(3, line.getPurchaseRatePerUnit())
                .setParameter(4, line.getLineNetRate())
                .setParameter(5, line.getValueAtPurchaseRate())
                .setParameter(6, line.getRetailSaleRatePerUnit())
                .setParameter(7, line.getRetailSaleRate())
                .setParameter(8, line.getValueAtRetailRate())
                .setParameter(9, line.getWholesaleRatePerUnit())
                .setParameter(10, line.getWholesaleRate())
                .setParameter(11, line.getCostRatePerUnit())
                .setParameter(12, costValue)
                .setParameter(13, line.getBillItemId())
                .executeUpdate();

        evictLineCache();
    }

    /**
     * JPA merge of BillItemFinanceDetails' Approve-time fields. Mirrors what
     * {@code recalculateFinancialsBeforeAddingBillItemPreservingDistributedCosts()}
     * and the bill-level {@code distributeProportionalBillValuesToItems()}
     * actually change on {@code f} between Save and Approve -- the controller
     * is expected to have already run the equivalent math and carried the
     * results on the DTO; this just persists them. BIFD is guaranteed to
     * already exist at this point (created by
     * {@code GrnCostingNativeSqlService.saveBillItemFinanceDetails()} at Save
     * time), so this is always an update of a pre-existing row, never a
     * create.
     */
    private void mergeBillItemFinanceDetails(GrnApproveLineData line) {
        if (line.getBillItemFinanceDetailsId() == null) {
            return;
        }
        BillItemFinanceDetails bifd = em.find(BillItemFinanceDetails.class, line.getBillItemFinanceDetailsId());
        if (bifd == null) {
            return;
        }
        bifd.setLineGrossRate(BigDecimal.valueOf(line.getLineGrossRate()));
        bifd.setLineDiscountRate(BigDecimal.valueOf(line.getLineDiscountRate()));
        bifd.setLineNetRate(BigDecimal.valueOf(line.getLineNetRate()));
        bifd.setGrossRate(BigDecimal.valueOf(line.getGrossRate()));
        bifd.setLineGrossTotal(BigDecimal.valueOf(line.getLineGrossTotal()));
        bifd.setLineNetTotal(BigDecimal.valueOf(line.getLineNetTotal()));
        bifd.setGrossTotal(BigDecimal.valueOf(line.getGrossTotal()));
        bifd.setPurchaseRate(BigDecimal.valueOf(line.getLineNetRate()));
        bifd.setPurchaseRatePerUnit(BigDecimal.valueOf(line.getPurchaseRatePerUnit()));
        bifd.setValueAtPurchaseRate(BigDecimal.valueOf(line.getValueAtPurchaseRate()));
        bifd.setValueAtRetailRate(BigDecimal.valueOf(line.getValueAtRetailRate()));
        bifd.setLineCost(BigDecimal.valueOf(line.getLineCost()));
        bifd.setLineCostRate(BigDecimal.valueOf(line.getLineCostRate()));
        bifd.setValueAtCostRate(BigDecimal.valueOf(line.getValueAtCostRate()));
        bifd.setTotalCostRate(BigDecimal.valueOf(line.getTotalCostRate()));
        em.merge(bifd);
    }

    // -----------------------------------------------------------------------
    // ItemBatch find-or-create (JPA -- IDENTITY PK, no DTYPE concern)
    // -----------------------------------------------------------------------

    /**
     * Replicates {@code GrnCostingController.saveItemBatchWithCosting()}'s
     * find-or-create uniqueness key: an existing non-retired ItemBatch row
     * matching (item, purcahseRate [sic], retailsaleRate, costRate,
     * dateOfExpire). If found, only wholesaleRate is updated on it (matching
     * legacy exactly); otherwise a new row is persisted.
     *
     * GrnApproveLineData's own purchaseRatePerUnit/retailRatePerUnit/
     * costRatePerUnit fields (distinct from GrnLineData's inherited
     * retailSaleRatePerUnit/wholesaleRatePerUnit, which feed the pbi/BIFD
     * writes above) are used here specifically because they mirror
     * saveItemBatchWithCosting()'s own local variables of the same name --
     * i.e. the exact values legacy uses for this uniqueness key.
     */
    private long findOrCreateItemBatch(GrnApproveLineData line, Item amp) {
        String jpql = "SELECT p FROM ItemBatch p "
                + "WHERE p.retired = false "
                + "AND p.item = :itm "
                + "AND p.dateOfExpire = :doe "
                + "AND p.retailsaleRate = :ret "
                + "AND p.costRate = :cr "
                + "AND p.purcahseRate = :pur";

        List<ItemBatch> found = em.createQuery(jpql, ItemBatch.class)
                .setParameter("itm", amp)
                .setParameter("doe", line.getExpiryDate())
                .setParameter("ret", line.getRetailRatePerUnit())
                .setParameter("cr", line.getCostRatePerUnit())
                .setParameter("pur", line.getPurchaseRatePerUnit())
                .setMaxResults(1)
                .getResultList();

        ItemBatch itemBatch = (found != null && !found.isEmpty()) ? found.get(0) : null;

        if (itemBatch == null) {
            itemBatch = new ItemBatch();
            itemBatch.setItem(amp);
            itemBatch.setDateOfExpire(line.getExpiryDate());
            itemBatch.setBatchNo(line.getBatchNo());
            itemBatch.setPurcahseRate(line.getPurchaseRatePerUnit());
            itemBatch.setRetailsaleRate(line.getRetailRatePerUnit());
            itemBatch.setWholesaleRate(line.getWholesaleRatePerUnit());
            itemBatch.setCostRate(line.getCostRatePerUnit());
            if (line.getBillItemId() != null) {
                itemBatch.setLastPurchaseBillItem(em.getReference(BillItem.class, line.getBillItemId()));
            }
            // make/modal (Category/model) intentionally not set -- GrnApproveLineData
            // carries no make/model fields and these are cosmetic tracking-only
            // columns with no effect on stock/costing correctness. Flagged in the
            // task report as a minor parity gap versus legacy.
            em.persist(itemBatch);
            em.flush();
        } else {
            itemBatch.setWholesaleRate(line.getWholesaleRatePerUnit());
            em.merge(itemBatch);
        }
        return itemBatch.getId();
    }

    // -----------------------------------------------------------------------
    // Stock addition (own private copy of TransferReceiveNativeSqlService's
    // pattern, simplified for GRN's single-department-receiving case -- no
    // source/staff stock deduction, only dept stock addition)
    // -----------------------------------------------------------------------

    private long findOrCreateDeptStock(long deptId, long itemBatchId, double addQty) {
        @SuppressWarnings("unchecked")
        List<Object> found = em.createNativeQuery(
                "SELECT ID FROM " + stockTable()
                + " WHERE department_ID=? AND itemBatch_ID=?"
                + " AND (retired IS NULL OR retired=0) LIMIT 1")
                .setParameter(1, deptId)
                .setParameter(2, itemBatchId)
                .getResultList();

        if (!found.isEmpty()) {
            long stockId = ((Number) found.get(0)).longValue();
            em.createNativeQuery("UPDATE " + stockTable() + " SET stock=stock+? WHERE ID=?")
                    .setParameter(1, addQty)
                    .setParameter(2, stockId)
                    .executeUpdate();
            return stockId;
        } else {
            em.createNativeQuery(
                "INSERT INTO " + stockTable()
                + " (department_ID, itemBatch_ID, stock, retired)"
                + " VALUES (?,?,?,0)")
                .setParameter(1, deptId)
                .setParameter(2, itemBatchId)
                .setParameter(3, addQty)
                .executeUpdate();
            return ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        }
    }

    private double fetchStockQty(long stockId) {
        Object result = em.createNativeQuery(
                "SELECT stock FROM " + stockTable() + " WHERE ID=?")
                .setParameter(1, stockId)
                .getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    private void updatePharmaceuticalBillItemStockLink(Long pbiId, long stockId, long itemBatchId) {
        em.createNativeQuery(
                "UPDATE " + pharmBillItemTable() + " SET stock_ID=?, itemBatch_ID=? WHERE ID=?")
                .setParameter(1, stockId)
                .setParameter(2, itemBatchId)
                .setParameter(3, pbiId)
                .executeUpdate();
    }

    /**
     * Own private copy of TransferReceiveNativeSqlService.computeAggregates(),
     * simplified to a single receiving department/institution (GRN never
     * deducts from a second department the way Transfer Receive does).
     */
    private StockAggregateResult computeAggregates(
            long ampItemId, long itemBatchId, long departmentId, long institutionId,
            double postAddStockQty, double retailRate, double purchaseRate, double costRate) {

        StockAggregateResult r = new StockAggregateResult();
        r.setStockQty(postAddStockQty);

        String batchSql =
                "SELECT"
                + "  SUM(CASE WHEN d.institution_ID = ? THEN s.stock ELSE 0 END) AS instBatchQty,"
                + "  SUM(s.stock) AS totalBatchQty"
                + " FROM " + stockTable() + " s"
                + " JOIN " + departmentTable() + " d ON s.department_ID = d.ID"
                + " WHERE s.itemBatch_ID = ?";

        Object[] batchRow = (Object[]) em.createNativeQuery(batchSql)
                .setParameter(1, institutionId)
                .setParameter(2, itemBatchId)
                .getSingleResult();

        double instBatchQty = batchRow[0] == null ? 0.0 : ((Number) batchRow[0]).doubleValue();
        double totalBatchQty = batchRow[1] == null ? 0.0 : ((Number) batchRow[1]).doubleValue();

        r.setInstitutionBatchQty(instBatchQty);
        r.setTotalBatchQty(totalBatchQty);
        r.setInstitutionBatchStockValueAtPurchaseRate(instBatchQty * purchaseRate);
        r.setTotalBatchStockValueAtPurchaseRate(totalBatchQty * purchaseRate);
        r.setInstitutionBatchStockValueAtSaleRate(instBatchQty * retailRate);
        r.setTotalBatchStockValueAtSaleRate(totalBatchQty * retailRate);
        r.setInstitutionBatchStockValueAtCostRate(instBatchQty * costRate);
        r.setTotalBatchStockValueAtCostRate(totalBatchQty * costRate);

        String itemSql =
                "SELECT"
                + "  SUM(CASE WHEN s.department_ID = ? THEN s.stock ELSE 0 END) AS deptItemQty,"
                + "  SUM(CASE WHEN d.institution_ID = ? THEN s.stock ELSE 0 END) AS instItemQty,"
                + "  SUM(s.stock) AS totalItemQty,"
                + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.purcahseRate,0)*s.stock ELSE 0 END) AS deptItemPurchVal,"
                + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.purcahseRate,0)*s.stock ELSE 0 END) AS instItemPurchVal,"
                + "  SUM(COALESCE(ib.purcahseRate,0)*s.stock) AS totalItemPurchVal,"
                + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.costRate,0)*s.stock ELSE 0 END) AS deptItemCostVal,"
                + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.costRate,0)*s.stock ELSE 0 END) AS instItemCostVal,"
                + "  SUM(COALESCE(ib.costRate,0)*s.stock) AS totalItemCostVal,"
                + "  SUM(CASE WHEN s.department_ID = ? THEN COALESCE(ib.retailsaleRate,0)*s.stock ELSE 0 END) AS deptItemRetailVal,"
                + "  SUM(CASE WHEN d.institution_ID = ? THEN COALESCE(ib.retailsaleRate,0)*s.stock ELSE 0 END) AS instItemRetailVal,"
                + "  SUM(COALESCE(ib.retailsaleRate,0)*s.stock) AS totalItemRetailVal"
                + " FROM " + stockTable() + " s"
                + " JOIN " + itemBatchTable() + " ib ON s.itemBatch_ID = ib.ID"
                + " JOIN " + departmentTable() + " d ON s.department_ID = d.ID"
                + " WHERE ib.item_ID = ?";

        Object[] itemRow = (Object[]) em.createNativeQuery(itemSql)
                .setParameter(1, departmentId)
                .setParameter(2, institutionId)
                .setParameter(3, departmentId)
                .setParameter(4, institutionId)
                .setParameter(5, departmentId)
                .setParameter(6, institutionId)
                .setParameter(7, departmentId)
                .setParameter(8, institutionId)
                .setParameter(9, ampItemId)
                .getSingleResult();

        r.setDepartmentItemStock(toDouble(itemRow[0]));
        r.setInstitutionItemStock(toDouble(itemRow[1]));
        r.setTotalItemStock(toDouble(itemRow[2]));
        r.setItemStockValueAtPurchaseRate(toDouble(itemRow[3]));
        r.setInstitutionItemStockValueAtPurchaseRate(toDouble(itemRow[4]));
        r.setTotalItemStockValueAtPurchaseRate(toDouble(itemRow[5]));
        r.setItemStockValueAtCostRate(toDouble(itemRow[6]));
        r.setInstitutionItemStockValueAtCostRate(toDouble(itemRow[7]));
        r.setTotalItemStockValueAtCostRate(toDouble(itemRow[8]));
        r.setItemStockValueAtSaleRate(toDouble(itemRow[9]));
        r.setInstitutionItemStockValueAtSaleRate(toDouble(itemRow[10]));
        r.setTotalItemStockValueAtSaleRate(toDouble(itemRow[11]));

        return r;
    }

    /**
     * Own private copy of TransferReceiveNativeSqlService.insertStockHistory().
     * Unlike that copy, historyType IS set here -- to HistoryType.GoodReceive,
     * matching PharmacyBean.addToStockHistory()'s resolveStockHistoryType()
     * mapping for BillType.PharmacyGrnBill (Create/Save/Approve all share the
     * one GRN bill, so its BillType is always PharmacyGrnBill by the time
     * Approve runs).
     */
    private void insertStockHistory(Long pbId, GrnApproveLineData line, StockAggregateResult agg,
                                     long ampItemId, long itemBatchId, long departmentId, long institutionId) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();

        double retailRate = line.getRetailRatePerUnit();
        double purchaseRate = line.getPurchaseRatePerUnit();
        double costRate = line.getCostRatePerUnit();
        double wholesaleRate = line.getWholesaleRatePerUnit();

        em.createNativeQuery(
                "INSERT INTO " + stockHistoryTable()
                + " (pbItem_ID, itemBatch_ID, institution_ID, department_ID, item_ID,"
                + " retailRate, wholesaleRate, purchaseRate, costRate,"
                + " stockAt, fromDate, createdAt,"
                + " stockQty, instituionBatchQty, totalBatchQty,"
                + " itemStock, institutionItemStock, totalItemStock,"
                + " stockSaleValue, stockPurchaseValue, stockCostValue,"
                + " institutionBatchStockValueAtSaleRate, totalBatchStockValueAtSaleRate,"
                + " institutionBatchStockValueAtPurchaseRate, totalBatchStockValueAtPurchaseRate,"
                + " institutionBatchStockValueAtCostRate, totalBatchStockValueAtCostRate,"
                + " itemStockValueAtSaleRate, institutionItemStockValueAtSaleRate, totalItemStockValueAtSaleRate,"
                + " itemStockValueAtPurchaseRate, institutionItemStockValueAtPurchaseRate, totalItemStockValueAtPurchaseRate,"
                + " itemStockValueAtCostRate, institutionItemStockValueAtCostRate, totalItemStockValueAtCostRate,"
                + " hxYear, hxMonth, hxDate, hxWeek,"
                + " historyType, retired)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)")
                .setParameter(1, pbId)
                .setParameter(2, itemBatchId)
                .setParameter(3, institutionId)
                .setParameter(4, departmentId)
                .setParameter(5, ampItemId)
                .setParameter(6, retailRate)
                .setParameter(7, wholesaleRate)
                .setParameter(8, purchaseRate)
                .setParameter(9, costRate)
                .setParameter(10, new java.sql.Date(now.getTime()))
                .setParameter(11, new Timestamp(now.getTime()))
                .setParameter(12, new Timestamp(now.getTime()))
                .setParameter(13, agg.getStockQty())
                .setParameter(14, agg.getInstitutionBatchQty())
                .setParameter(15, agg.getTotalBatchQty())
                .setParameter(16, agg.getDepartmentItemStock())
                .setParameter(17, agg.getInstitutionItemStock())
                .setParameter(18, agg.getTotalItemStock())
                .setParameter(19, agg.getStockQty() * retailRate)
                .setParameter(20, agg.getStockQty() * purchaseRate)
                .setParameter(21, agg.getStockQty() * costRate)
                .setParameter(22, agg.getInstitutionBatchStockValueAtSaleRate())
                .setParameter(23, agg.getTotalBatchStockValueAtSaleRate())
                .setParameter(24, agg.getInstitutionBatchStockValueAtPurchaseRate())
                .setParameter(25, agg.getTotalBatchStockValueAtPurchaseRate())
                .setParameter(26, agg.getInstitutionBatchStockValueAtCostRate())
                .setParameter(27, agg.getTotalBatchStockValueAtCostRate())
                .setParameter(28, agg.getItemStockValueAtSaleRate())
                .setParameter(29, agg.getInstitutionItemStockValueAtSaleRate())
                .setParameter(30, agg.getTotalItemStockValueAtSaleRate())
                .setParameter(31, agg.getItemStockValueAtPurchaseRate())
                .setParameter(32, agg.getInstitutionItemStockValueAtPurchaseRate())
                .setParameter(33, agg.getTotalItemStockValueAtPurchaseRate())
                .setParameter(34, agg.getItemStockValueAtCostRate())
                .setParameter(35, agg.getInstitutionItemStockValueAtCostRate())
                .setParameter(36, agg.getTotalItemStockValueAtCostRate())
                .setParameter(37, cal.get(Calendar.YEAR))
                .setParameter(38, cal.get(Calendar.MONTH))
                .setParameter(39, cal.get(Calendar.DATE))
                .setParameter(40, cal.get(Calendar.WEEK_OF_YEAR))
                .setParameter(41, HistoryType.GoodReceive.toString())
                .executeUpdate();
    }

    private static double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }

    // -----------------------------------------------------------------------
    // PO remaining/completed qty bookkeeping (a DIFFERENT bill's
    // pharmaceuticalbillitem row -- the PO's own line, not this GRN's)
    // -----------------------------------------------------------------------

    /**
     * Two bugs fixed here (#22893):
     * - {@code ABS(completedQty) + ABS(?)}: MySQL {@code ABS(NULL)} is
     *   {@code NULL} and {@code NULL + x} is {@code NULL}, so with the column
     *   starting {@code NULL} it never became non-null, on any GRN.
     *   {@code COALESCE(...,0)} fixes that.
     * - No floor at 0 on {@code remainingQty}/{@code remainingFreeQty} --
     *   {@code GREATEST(...,0)} is a defense-in-depth floor; the primary
     *   guard against over-receipt driving this negative is now the
     *   pre-approve validation in
     *   {@code GrnCostingNativeSqlController.validateGrnQuantities()}, which
     *   blocks Finalize/Approve with an error before this UPDATE ever runs
     *   (product decision: block, not silently clamp -- see #22893).
     */
    private void decrementPoRemainingQty(long poBillItemId, double qty, double freeQty) {
        em.createNativeQuery(
                "UPDATE " + pharmBillItemTable()
                + " SET remainingQty = GREATEST(ABS(COALESCE(remainingQty,0)) - ABS(?), 0),"
                + " remainingFreeQty = GREATEST(ABS(COALESCE(remainingFreeQty,0)) - ABS(?), 0),"
                + " completedQty = ABS(COALESCE(completedQty,0)) + ABS(?),"
                + " completedFreeQty = ABS(COALESCE(completedFreeQty,0)) + ABS(?)"
                + " WHERE billItem_ID=?")
                .setParameter(1, qty)
                .setParameter(2, freeQty)
                .setParameter(3, qty)
                .setParameter(4, freeQty)
                .setParameter(5, poBillItemId)
                .executeUpdate();
        evictLineCache();
    }

    // -----------------------------------------------------------------------
    // Bill-header mutations (native UPDATE only -- see class Javadoc)
    // -----------------------------------------------------------------------

    /**
     * Bill number strings are config-driven and generated by the controller;
     * this just writes them. Bill has no single "billNumber" column -- the
     * two actual columns are deptId/insId (confirmed against Bill.java and
     * GrnCostingNativeSqlService.createDraftBill()'s own deptId/insId params).
     */
    private void updateBillNumbers(long billId, String deptId, String insId) {
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET deptId=?, insId=? WHERE ID=?")
                .setParameter(1, deptId)
                .setParameter(2, insId)
                .setParameter(3, billId)
                .executeUpdate();
        evictCache();
    }

    private void approveBillHeader(long billId, long editorId, long approveUserId) {
        Timestamp now = new Timestamp(new Date().getTime());
        em.createNativeQuery(
                "UPDATE " + billTable()
                + " SET checked=1, checkeAt=?, checkedBy_ID=?, approveUser_ID=?, approveAt=?,"
                + " editedAt=?, editor_ID=?, BILLTYPEATOMIC=? WHERE ID=?")
                .setParameter(1, now)
                .setParameter(2, editorId)
                .setParameter(3, approveUserId)
                .setParameter(4, now)
                .setParameter(5, now)
                .setParameter(6, editorId)
                .setParameter(7, BillTypeAtomic.PHARMACY_GRN.toString())
                .setParameter(8, billId)
                .executeUpdate();
        evictCache();
    }

    private void updateBalance(long billId, double balanceValue) {
        em.createNativeQuery(
                "UPDATE " + billTable() + " SET balance=? WHERE ID=?")
                .setParameter(1, balanceValue)
                .setParameter(2, billId)
                .executeUpdate();
        evictCache();
    }

    private void markPurchaseOrderFullyIssued(long poBillId, long userId) {
        em.createNativeQuery(
                "UPDATE " + billTable()
                + " SET fullyIssued=1, fullyIssuedAt=?, fullyIssuedBy_ID=?"
                + " WHERE ID=? AND (fullyIssued IS NULL OR fullyIssued=0)")
                .setParameter(1, new Timestamp(new Date().getTime()))
                .setParameter(2, userId)
                .setParameter(3, poBillId)
                .executeUpdate();
        evictCache();
    }

    /**
     * Retires any surviving orphan PHARMACY_GRN_PRE bills for the same PO
     * left behind by previous interrupted sessions (#21579). Matches legacy's
     * JPQL read in approveGrnWithSaveApprove() exactly on the filter
     * predicate; unlike legacy (which leaves retirer null on these rows),
     * this write also sets retirer_ID -- a deliberate small improvement, not
     * a functional requirement, flagged in the task report.
     */
    private void retireOrphanPreBills(long poBillId, long currentGrnBillId, long retirerId) {
        em.createNativeQuery(
                "UPDATE " + billTable()
                + " SET retired=1, retiredAt=?, retirer_ID=?"
                + " WHERE referenceBill_ID=? AND BILLTYPEATOMIC=? AND retired=0 AND ID != ?")
                .setParameter(1, new Timestamp(new Date().getTime()))
                .setParameter(2, retirerId)
                .setParameter(3, poBillId)
                .setParameter(4, BillTypeAtomic.PHARMACY_GRN_PRE.toString())
                .setParameter(5, currentGrnBillId)
                .executeUpdate();
        evictCache();
    }

    // -----------------------------------------------------------------------
    // Payment (JPA persist -- IDENTITY PK)
    // -----------------------------------------------------------------------

    /**
     * Mirrors GrnCostingController.createPayment()/setPaymentMethodData().
     * paidValue is the pre-negation positive net total passed explicitly on
     * the request -- NOT derived from the bill row, which by this point has
     * already been sign-flipped by the updateBillTotals() call above.
     * creater is approveUserId (the acting user) -- in legacy this and
     * editorId are always the same session user; the request separates them
     * for flexibility, and Payment.creater is deliberately tied to the
     * approving user specifically.
     */
    private void insertPayment(GrnApproveRequest request) {
        Payment p = new Payment();
        p.setBill(em.getReference(Bill.class, request.getBillId()));
        p.setInstitution(em.getReference(Institution.class, request.getInstitutionId()));
        p.setDepartment(em.getReference(Department.class, request.getDepartmentId()));
        p.setCreatedAt(new Date());
        p.setCreater(em.getReference(WebUser.class, request.getApproveUserId()));
        p.setPaymentMethod(request.getPaymentMethod());
        p.setPaidValue(request.getPrePositiveNetTotalForPayment());
        em.persist(p);
        em.flush();
        evictCache();
    }

    // -----------------------------------------------------------------------
    // Cache eviction
    // -----------------------------------------------------------------------

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(Bill.class);
        cache.evict(BillItem.class);
        cache.evict(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem.class);
        cache.evict(BillItemFinanceDetails.class);
        cache.evict(ItemBatch.class);
        cache.evict(Stock.class);
        cache.evict(StockHistory.class);
        cache.evict(Payment.class);
    }

    private void evictLineCache() {
        evictCache();
    }

    // -----------------------------------------------------------------------
    // Table name resolution (own private fields, not shared with
    // GrnCostingNativeSqlService or TransferReceiveNativeSqlService, per
    // established convention)
    // -----------------------------------------------------------------------

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        return name.toString();
    }

    private String billTable() {
        if (tBill == null) tBill = resolveTable("BILL");
        return tBill;
    }

    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }

    private String stockTable() {
        if (tStock == null) tStock = resolveTable("STOCK");
        return tStock;
    }

    private String stockHistoryTable() {
        if (tStockHistory == null) tStockHistory = resolveTable("STOCKHISTORY");
        return tStockHistory;
    }

    private String itemBatchTable() {
        if (tItemBatch == null) tItemBatch = resolveTable("ITEMBATCH");
        return tItemBatch;
    }

    private String departmentTable() {
        if (tDepartment == null) tDepartment = resolveTable("DEPARTMENT");
        return tDepartment;
    }
}
