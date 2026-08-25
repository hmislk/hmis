package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.BillItemFinanceDetails;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Native SQL write path for the Purchase Order Approving bill.
 * Only the APPROVED bill's own bill / billitem / pharmaceuticalbillitem rows
 * are touched here -- the requested bill stays JPA (see
 * PurchaseOrderApprovingNativeSqlController). BillItemFinanceDetails stays
 * JPA (IDENTITY PK, calculation-heavy), matching
 * PurchaseOrderRequestNativeSqlService's split.
 * Related issue: #22738
 */
@Stateless
public class PurchaseOrderApprovingNativeSqlService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @EJB
    private PurchaseOrderRequestNativeSqlService purchaseOrderRequestNativeSqlService;

    private String tBill;
    private String tBillItem;
    private String tPharmBillItem;

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

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
    }

    private void evictLineCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
        cache.evict(com.divudi.core.entity.BillItem.class);
        cache.evict(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem.class);
        cache.evict(BillItemFinanceDetails.class);
    }

    /**
     * JPA persist for the approved bill row -- NOT native SQL. BilledBill is
     * a single-table-inheritance subclass of Bill (discriminated by the
     * DTYPE column), and DTYPE has no explicit @DiscriminatorColumn/
     * @DiscriminatorValue mapping in this codebase -- EclipseLink derives and
     * writes it automatically only through em.persist(), never through a raw
     * native INSERT. A native INSERT here previously left DTYPE null,
     * producing "Missing class indicator field from database row" on the
     * very next JPA read of this bill. This is the same reason
     * TransferIssueNativeSqlService.settle() persists its BilledBill header
     * via JPA rather than native SQL (see its class-level Javadoc, step 1:
     * "Persist bill header via JPA (correct DTYPE + IDENTITY PK)") --
     * follow that established precedent here instead of reinventing it.
     * This row's own backwardReferenceBill points at the requested bill,
     * matching legacy's setBackwardReferenceBill(getRequestedBill()); the
     * requested bill's own referenceBill (pointing here) is written later in
     * JPA by the controller.
     */
    public long createApprovedBill(long requestedBillId, long departmentId, long institutionId,
            long fromDepartmentId, long fromInstitutionId, long createrId, String deptId, String insId) {
        com.divudi.core.entity.BilledBill bill = new com.divudi.core.entity.BilledBill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_APPROVAL);
        bill.setBillType(BillType.PharmacyOrderApprove);
        bill.setDepartment(em.getReference(com.divudi.core.entity.Department.class, departmentId));
        bill.setInstitution(em.getReference(com.divudi.core.entity.Institution.class, institutionId));
        bill.setFromDepartment(em.getReference(com.divudi.core.entity.Department.class, fromDepartmentId));
        bill.setFromInstitution(em.getReference(com.divudi.core.entity.Institution.class, fromInstitutionId));
        bill.setBackwardReferenceBill(em.getReference(com.divudi.core.entity.Bill.class, requestedBillId));
        bill.setCreater(em.getReference(com.divudi.core.entity.WebUser.class, createrId));
        bill.setCreatedAt(new Date());
        bill.setDeptId(deptId);
        bill.setInsId(insId);
        bill.setNetTotal(0.0);
        bill.setTotal(0.0);
        em.persist(bill);
        em.flush();
        evictCache();
        return bill.getId();
    }

    /**
     * Native UPDATE for payment method / supplier / etc. Mirrors
     * PurchaseOrderRequestNativeSqlService.updateDraftBillHeader exactly,
     * including the comments column.
     */
    public void updateApprovedBillHeader(long approvedBillId, Long toInstitutionId, PaymentMethod paymentMethod,
            int creditDuration, boolean consignment, DepartmentType departmentType, String comments, Long editorId) {
        em.createNativeQuery(
            "UPDATE " + billTable()
            + " SET toInstitution_ID=?, paymentMethod=?, creditDuration=?, consignment=?,"
            + " departmentType=?, comments=?, editor_ID=?, editedAt=?"
            + " WHERE ID=?")
            .setParameter(1, toInstitutionId)
            .setParameter(2, paymentMethod != null ? paymentMethod.toString() : null)
            .setParameter(3, creditDuration)
            .setParameter(4, consignment ? 1 : 0)
            .setParameter(5, departmentType != null ? departmentType.toString() : null)
            .setParameter(6, comments)
            .setParameter(7, editorId)
            .setParameter(8, new Timestamp(new Date().getTime()))
            .setParameter(9, approvedBillId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Persists the approved bill's netTotal/total columns. Delegates to
     * PurchaseOrderRequestNativeSqlService.updateBillTotals() (already public,
     * same UPDATE shape) rather than duplicating it -- this service's own
     * createApprovedBill()/updateApprovedBillHeader() never write these two
     * columns, the same gap Phase 1 had before its own updateBillTotals() fix.
     */
    public void updateBillTotals(long billId, double netTotal, double total) {
        purchaseOrderRequestNativeSqlService.updateBillTotals(billId, netTotal, total);
    }

    /**
     * Native UPDATE for approveAt/approveUser_ID on the approved bill.
     * Deliberately NOT a JPA billFacade.edit() merge: approvedBill is
     * detached (BillFacade is @Stateless) and Bill.billItems carries
     * cascade=ALL, orphanRemoval=true, but this bill's lines were written by
     * saveApprovedLine() through native SQL, not through this entity's
     * managed collection -- merging the detached bill would risk EclipseLink
     * treating the native-written lines as absent from the in-memory
     * collection and deleting them via orphan removal. Same reasoning as
     * updateApprovedBillHeader() and updateBillTotals() above; see also
     * PurchaseOrderRequestNativeSqlService's own Javadoc for the same trap.
     */
    public void approveBill(long approvedBillId, long approveUserId) {
        em.createNativeQuery(
            "UPDATE " + billTable() + " SET approveAt=?, approveUser_ID=? WHERE ID=?")
            .setParameter(1, new Timestamp(new Date().getTime()))
            .setParameter(2, approveUserId)
            .setParameter(3, approvedBillId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Native UPDATE for the approved bill's own referenceBill_ID, pointing
     * back at the requested bill. Mirrors legacy PurchaseOrderController
     * .saveBill(): getAprovedBill().setReferenceBill(getRequestedBill()).
     * The po.xhtml/po_custom_*.xhtml print composites read
     * cc.attrs.bill.referenceBill.* (institution letterhead, "Prepared By",
     * "Authorized By") off this same approved bill, so without this write
     * those fields render blank (EL null-safe navigation swallows the NPE).
     */
    public void linkApprovedBillToRequest(long approvedBillId, long requestedBillId) {
        em.createNativeQuery(
            "UPDATE " + billTable() + " SET referenceBill_ID=? WHERE ID=?")
            .setParameter(1, requestedBillId)
            .setParameter(2, approvedBillId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Native UPDATE for the requested bill's forward-pointing referenceBill_ID
     * cross-link (the approved bill's own backwardReferenceBill_ID is already
     * set at creation in createApprovedBill()). Matches the referenceBill_ID
     * write pattern in TransferReceiveNativeSqlService.settle().
     */
    public void linkRequestedBillToApproval(long requestedBillId, long approvedBillId) {
        em.createNativeQuery(
            "UPDATE " + billTable() + " SET referenceBill_ID=? WHERE ID=?")
            .setParameter(1, approvedBillId)
            .setParameter(2, requestedBillId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Native INSERT/UPDATE for billitem + pharmaceuticalbillitem on the
     * approved bill. Delegates the line math to
     * PurchaseOrderRequestNativeSqlService.computeLineValues() (injected
     * @EJB) rather than duplicating it. Uses the same upsert-by-existence
     * fix for the PBI row as Phase 1's saveLine (UPDATE first, INSERT only
     * if 0 rows affected -- never branch on line.getPharmaceuticalBillItemId(),
     * since the controller never learns the generated PBI id back from an
     * insert).
     *
     * Unlike Phase 1's Request-side saveLine, the PBI UPDATE/INSERT here
     * also sets remainingQty=pbiQty, remainingFreeQty=pbiFreeQty inline --
     * legacy's saveBillComponent() sets these explicitly on approve, whereas
     * a request line has no "remaining" concept until it is approved.
     */
    public long saveApprovedLine(long approvedBillId, PurchaseOrderRequestLineData line) {
        PurchaseOrderRequestNativeSqlService.LineValues v = purchaseOrderRequestNativeSqlService.computeLineValues(line);
        BigDecimal qty = v.qty, freeQty = v.freeQty, purchaseRate = v.purchaseRate, retailRate = v.retailRate;
        BigDecimal grossValue = v.grossValue, netValue = v.netValue, netRate = v.netRate;
        BigDecimal purchaseValue = v.purchaseValue, retailValue = v.retailValue, unitsPerPack = v.unitsPerPack;

        long billItemId;
        if (line.getBillItemId() == null) {
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, qty, netValue, grossValue, Rate, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, searialNo)"
                + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, approvedBillId)
                .setParameter(2, line.getItemId())
                .setParameter(3, qty.doubleValue())
                .setParameter(4, netValue.doubleValue())
                .setParameter(5, grossValue.doubleValue())
                .setParameter(6, purchaseRate.doubleValue())
                .setParameter(7, netRate.doubleValue())
                .setParameter(8, new Timestamp(new Date().getTime()))
                .setParameter(9, line.getCreaterId())
                .setParameter(10, line.getSerialNo())
                .executeUpdate();
            billItemId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        } else {
            billItemId = line.getBillItemId();
            em.createNativeQuery(
                "UPDATE " + billItemTable()
                + " SET qty=?, netValue=?, grossValue=?, Rate=?, netRate=? WHERE ID=?")
                .setParameter(1, qty.doubleValue())
                .setParameter(2, netValue.doubleValue())
                .setParameter(3, grossValue.doubleValue())
                .setParameter(4, purchaseRate.doubleValue())
                .setParameter(5, netRate.doubleValue())
                .setParameter(6, billItemId)
                .executeUpdate();
        }

        double pbiQty = v.pbiQty, pbiFreeQty = v.pbiFreeQty, pbiPurchaseRate = v.pbiPurchaseRate, pbiRetailRate = v.pbiRetailRate;

        int updated = em.createNativeQuery(
                "UPDATE " + pharmBillItemTable()
                + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
                + " retailRate=?, retailRatePack=?, retailValue=?,"
                + " remainingQty=?, remainingFreeQty=? WHERE billItem_ID=?")
                .setParameter(1, pbiQty)
                .setParameter(2, pbiFreeQty)
                .setParameter(3, pbiPurchaseRate)
                .setParameter(4, purchaseRate.doubleValue())
                .setParameter(5, purchaseValue.doubleValue())
                .setParameter(6, pbiRetailRate)
                .setParameter(7, retailRate.doubleValue())
                .setParameter(8, retailValue.doubleValue())
                .setParameter(9, pbiQty)
                .setParameter(10, pbiFreeQty)
                .setParameter(11, billItemId)
                .executeUpdate();

        if (updated == 0) {
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, qty, freeQty, purchaseRate, purchaseRatePack, purchaseValue,"
                + " retailRate, retailRatePack, retailValue, remainingQty, remainingFreeQty,"
                + " costRate, costRatePack, costValue, createdAt, creater_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
                .setParameter(1, billItemId)
                .setParameter(2, pbiQty)
                .setParameter(3, pbiFreeQty)
                .setParameter(4, pbiPurchaseRate)
                .setParameter(5, purchaseRate.doubleValue())
                .setParameter(6, purchaseValue.doubleValue())
                .setParameter(7, pbiRetailRate)
                .setParameter(8, retailRate.doubleValue())
                .setParameter(9, retailValue.doubleValue())
                .setParameter(10, pbiQty)
                .setParameter(11, pbiFreeQty)
                .setParameter(12, new Timestamp(new Date().getTime()))
                .setParameter(13, line.getCreaterId())
                .executeUpdate();
        }

        saveBillItemFinanceDetails(billItemId, line, qty, freeQty, purchaseRate, retailRate, netValue, grossValue,
                netRate, unitsPerPack, pbiQty, pbiFreeQty);

        evictLineCache();
        return billItemId;
    }

    private void saveBillItemFinanceDetails(long billItemId, PurchaseOrderRequestLineData line,
            BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
            BigDecimal netValue, BigDecimal grossValue, BigDecimal netRate, BigDecimal unitsPerPack,
            double quantityByUnits, double freeQuantityByUnits) {
        BillItemFinanceDetails bifd;
        if (line.getBillItemId() != null) {
            String jpql = "SELECT bi.billItemFinanceDetails FROM BillItem bi WHERE bi.id = :id";
            List<BillItemFinanceDetails> existing = em.createQuery(jpql, BillItemFinanceDetails.class)
                    .setParameter("id", billItemId)
                    .getResultList();
            bifd = (existing != null && !existing.isEmpty() && existing.get(0) != null) ? existing.get(0) : new BillItemFinanceDetails();
        } else {
            bifd = new BillItemFinanceDetails();
        }

        bifd.setLineNetRate(purchaseRate);
        bifd.setLineGrossRate(purchaseRate);
        bifd.setGrossRate(purchaseRate);
        bifd.setLineNetTotal(netValue);
        bifd.setLineGrossTotal(grossValue);
        bifd.setGrossTotal(grossValue);
        bifd.setQuantity(qty);
        bifd.setFreeQuantity(freeQty);
        bifd.setQuantityByUnits(BigDecimal.valueOf(quantityByUnits));
        bifd.setFreeQuantityByUnits(BigDecimal.valueOf(freeQuantityByUnits));
        bifd.setRetailSaleRate(retailRate);
        bifd.setUnitsPerPack(unitsPerPack);
        bifd.setValueAtPurchaseRate(purchaseRate.multiply(qty.add(freeQty)));
        bifd.setValueAtRetailRate(retailRate.multiply(qty.add(freeQty)));
        bifd.setLineCost(BigDecimal.ZERO);
        bifd.setLineCostRate(BigDecimal.ZERO);
        bifd.setValueAtCostRate(BigDecimal.ZERO);

        if (bifd.getId() == null) {
            bifd.setCreatedAt(new Date());
            em.persist(bifd);
            em.flush();
            em.createNativeQuery("UPDATE " + billItemTable() + " SET BILLITEMFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bifd.getId())
                    .setParameter(2, billItemId)
                    .executeUpdate();
        } else {
            em.merge(bifd);
        }
    }

    /**
     * Native zero-qty retirement sweep for the approved bill's lines.
     * Delegates the zero-qty predicate to
     * PurchaseOrderRequestNativeSqlService.isZeroQtyLine() rather than
     * duplicating it. Retire comment is "Retired at Approving PO" (matching
     * legacy's saveBillComponent()), distinct from the Request page's
     * "Retired at Finalising PO".
     *
     * Unlike Phase 1's retireZeroQtyLines, the surviving branch here does
     * NOT re-set remainingQty/remainingFreeQty -- saveApprovedLine already
     * writes those at save time, so there is nothing left to do for
     * survivors here.
     */
    @SuppressWarnings("unchecked")
    public int retireZeroQtyApprovedLines(long approvedBillId, long retirerId) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT bi.ID, pbi.qty, pbi.freeQty FROM " + billItemTable() + " bi "
            + "JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID "
            + "WHERE bi.bill_ID = ? AND bi.retired = 0")
            .setParameter(1, approvedBillId)
            .getResultList();

        int survivingCount = 0;
        Timestamp now = new Timestamp(new Date().getTime());
        for (Object[] row : rows) {
            long billItemId = ((Number) row[0]).longValue();
            double qty = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double freeQty = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

            if (purchaseOrderRequestNativeSqlService.isZeroQtyLine(qty, freeQty)) {
                em.createNativeQuery(
                    "UPDATE " + billItemTable() + " SET retired=1, retirer_ID=?, retiredAt=?, retireComments=? WHERE ID=?")
                    .setParameter(1, retirerId)
                    .setParameter(2, now)
                    .setParameter(3, "Retired at Approving PO")
                    .setParameter(4, billItemId)
                    .executeUpdate();
                em.createNativeQuery(
                    "UPDATE " + pharmBillItemTable() + " SET retired=1, retirer_ID=?, retiredAt=? WHERE billItem_ID=?")
                    .setParameter(1, retirerId)
                    .setParameter(2, now)
                    .setParameter(3, billItemId)
                    .executeUpdate();
            } else {
                survivingCount++;
            }
        }
        evictLineCache();
        return survivingCount;
    }

    /**
     * JPQL projection reading the requested bill's non-retired
     * PharmaceuticalBillItem lines. The ampp flag uses TYPE(bi.item) inside
     * a CASE WHEN expression within the SELECT NEW constructor argument list.
     * Precedent for TYPE(x) = EntityName inside CASE WHEN (in a plain SELECT,
     * not SELECT NEW) exists elsewhere in this codebase --
     * MdInwardReportController (SUM(CASE WHEN TYPE(bi.bill) = BilledBill ...))
     * and ChannelService (SUM(CASE WHEN TYPE(b) = CancelledBill ...)) -- and
     * this project pins EclipseLink 2.7.12, which per its release notes
     * supports TYPE() in CASE expressions. Using it as a SELECT NEW
     * constructor argument specifically (rather than as a SUM operand) is
     * NOT directly precedented in this codebase and has not been run against
     * a live database -- flagged for confirmation during Task 6's Playwright
     * pass, per the task brief.
     */
    /**
     * ampp is deliberately not projected here: EclipseLink cannot resolve
     * {@code CASE WHEN TYPE(bi.item) = <EntityClass> THEN ... END} as a
     * SELECT NEW constructor argument (confirmed at runtime -- "state field
     * path ... cannot be resolved to a valid type"), even though the same
     * TYPE()-in-CASE pattern works fine as a plain SELECT/SUM operand
     * elsewhere in this codebase. The caller (PurchaseOrderApprovingNativeSqlController
     * .generateBillComponent()) already resolves the real Item entity via
     * itemFacade.find(itemId) and derives ampp from that entity's own type
     * where it's actually needed (approve()), so the projection doesn't need
     * to carry it.
     */
    public List<PurchaseOrderRequestLineData> loadRequestedLines(long requestedBillId) {
        String jpql = "SELECT NEW com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData("
                + "bi.item.id, "
                + "bi.billItemFinanceDetails.quantity, "
                + "bi.billItemFinanceDetails.freeQuantity, "
                + "bi.billItemFinanceDetails.lineGrossRate, "
                + "bi.billItemFinanceDetails.retailSaleRate, "
                + "bi.billItemFinanceDetails.unitsPerPack, "
                + "bi.searialNo) "
                + "FROM PharmaceuticalBillItem p JOIN p.billItem bi "
                + "WHERE bi.bill.id = :billId AND bi.retired = false "
                + "ORDER BY bi.searialNo";
        return em.createQuery(jpql, PurchaseOrderRequestLineData.class)
                .setParameter("billId", requestedBillId)
                .getResultList();
    }
}
