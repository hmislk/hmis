package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.BillType;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.pharmacy.PurchaseOrderRequestLineData;
import com.divudi.core.entity.BillItemFinanceDetails;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Native SQL write path for the Purchase Order Request draft bill.
 * Only bill / billitem / pharmaceuticalbillitem are touched here —
 * BillItemFinanceDetails stays JPA (IDENTITY PK, calculation-heavy),
 * matching RetailSaleNativeSqlService's split.
 * Related issue: #22727
 */
@Stateless
public class PurchaseOrderRequestNativeSqlService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    private String tBill;
    private String tBillItem;
    private String tPharmBillItem;

    /**
     * JPA persist for the draft bill row -- NOT native SQL. BilledBill is a
     * single-table-inheritance subclass of Bill (discriminated by the DTYPE
     * column), and DTYPE has no explicit @DiscriminatorColumn/
     * @DiscriminatorValue mapping in this codebase -- EclipseLink derives and
     * writes it automatically only through em.persist(), never through a raw
     * native INSERT. A native INSERT here previously left DTYPE null,
     * producing "Missing class indicator field from database row" on the
     * very next JPA read of this bill (billFacade.find() in the controller).
     * Same fix as PurchaseOrderApprovingNativeSqlService.createApprovedBill()
     * -- see that method's Javadoc for the full precedent.
     */
    public long createDraftBill(long departmentId, long institutionId, long createrId, String deptId, String insId) {
        com.divudi.core.entity.BilledBill bill = new com.divudi.core.entity.BilledBill();
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_ORDER_PRE);
        bill.setBillType(BillType.PharmacyOrder);
        bill.setDepartment(em.getReference(com.divudi.core.entity.Department.class, departmentId));
        bill.setInstitution(em.getReference(com.divudi.core.entity.Institution.class, institutionId));
        bill.setFromDepartment(em.getReference(com.divudi.core.entity.Department.class, departmentId));
        bill.setFromInstitution(em.getReference(com.divudi.core.entity.Institution.class, institutionId));
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

    public void updateDraftBillHeader(long billId, Long toInstitutionId, PaymentMethod paymentMethod,
                                       int creditDuration, boolean consignment, DepartmentType departmentType,
                                       String comments, Long editorId) {
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
            .setParameter(9, billId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Persists the bill-level netTotal/total columns. calculateBillTotals()
     * in the controller only mutates the in-memory Bill entity — nothing else
     * in the native save path wrote these columns back to the DB, so totals
     * went stale in the database after the first edit-then-save cycle.
     * Legacy relied on billFacade.edit(currentBill) (a full JPA merge) to
     * persist whatever calculateBillTotals() computed; this is the native
     * equivalent for just these two columns.
     */
    public void updateBillTotals(long billId, double netTotal, double total) {
        em.createNativeQuery(
            "UPDATE " + billTable() + " SET netTotal=?, total=? WHERE ID=?")
            .setParameter(1, netTotal)
            .setParameter(2, total)
            .setParameter(3, billId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Persists a single BillItem's renumbered searialNo. calculateBillTotals()
     * in the controller renumbers surviving lines' serials in memory after a
     * removal, but nothing else in the native save path wrote that column back
     * to the DB -- a reload (loadBillItems, ordered by searialNo) could then
     * see gaps left by the retired line, and a later-added line reusing that
     * gap's serial would collide with a persisted survivor.
     */
    public void updateBillItemSerialNo(long billItemId, int serialNo) {
        em.createNativeQuery(
            "UPDATE " + billItemTable() + " SET searialNo=? WHERE ID=?")
            .setParameter(1, serialNo)
            .setParameter(2, billItemId)
            .executeUpdate();
        evictLineCache();
    }

    public boolean isBillChecked(long billId) {
        Object result = em.createNativeQuery(
            "SELECT checked FROM " + billTable() + " WHERE ID=?")
            .setParameter(1, billId)
            .getSingleResult();
        if (result instanceof Boolean) return (Boolean) result;
        if (result instanceof Number) return ((Number) result).intValue() != 0;
        return false;
    }

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
    }

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

    // Forward-declared table helpers; used by line-item and finalization methods added in Task 3/4
    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    // Forward-declared table helpers; used by line-item and finalization methods added in Task 3/4
    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }

    /**
     * Pure calculation extracted from legacy calculateLineValues() — no em access,
     * unit tested directly. AMPP items store rates per-pack on BillItemFinanceDetails
     * but per-unit on PharmaceuticalBillItem; non-AMPP items use the same rate for both.
     */
    public static final class LineValues {
        final BigDecimal qty, freeQty, purchaseRate, retailRate, unitsPerPack;
        final BigDecimal grossValue, netValue, purchaseValue, retailValue, netRate;
        final double pbiQty, pbiFreeQty, pbiPurchaseRate, pbiRetailRate;

        LineValues(BigDecimal qty, BigDecimal freeQty, BigDecimal purchaseRate, BigDecimal retailRate,
                   BigDecimal unitsPerPack, BigDecimal grossValue, BigDecimal netValue,
                   BigDecimal purchaseValue, BigDecimal retailValue, BigDecimal netRate,
                   double pbiQty, double pbiFreeQty, double pbiPurchaseRate, double pbiRetailRate) {
            this.qty = qty; this.freeQty = freeQty; this.purchaseRate = purchaseRate; this.retailRate = retailRate;
            this.unitsPerPack = unitsPerPack; this.grossValue = grossValue; this.netValue = netValue;
            this.purchaseValue = purchaseValue; this.retailValue = retailValue; this.netRate = netRate;
            this.pbiQty = pbiQty; this.pbiFreeQty = pbiFreeQty;
            this.pbiPurchaseRate = pbiPurchaseRate; this.pbiRetailRate = pbiRetailRate;
        }
    }

    /**
     * Public (not package-private): called cross-EJB from
     * PurchaseOrderApprovingNativeSqlService via the injected @EJB reference.
     * A stateless EJB's no-interface local view only exposes public methods
     * through the container-generated proxy -- package-private access works
     * for plain same-package Java calls but throws "Illegal non-business
     * method access on no-interface view" when invoked through the proxy.
     * Confirmed by a live redeploy failure; do not revert to package-private.
     */
    public LineValues computeLineValues(PurchaseOrderRequestLineData line) {
        BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ZERO;
        BigDecimal freeQty = line.getFreeQuantity() != null ? line.getFreeQuantity() : BigDecimal.ZERO;
        BigDecimal purchaseRate = line.getPurchaseRate() != null ? line.getPurchaseRate() : BigDecimal.ZERO;
        BigDecimal retailRate = line.getRetailRate() != null ? line.getRetailRate() : BigDecimal.ZERO;
        BigDecimal unitsPerPack = (line.getUnitsPerPack() != null && line.getUnitsPerPack().doubleValue() > 0)
                ? line.getUnitsPerPack() : BigDecimal.ONE;

        BigDecimal grossValue = purchaseRate.multiply(qty);
        BigDecimal netValue = grossValue;
        BigDecimal purchaseValue = purchaseRate.multiply(qty.add(freeQty));
        BigDecimal retailValue = retailRate.multiply(qty.add(freeQty));
        BigDecimal netRate = qty.doubleValue() > 0
                ? netValue.divide(qty, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        double pbiQty = line.isAmpp() ? qty.doubleValue() * unitsPerPack.doubleValue() : qty.doubleValue();
        double pbiFreeQty = line.isAmpp() ? freeQty.doubleValue() * unitsPerPack.doubleValue() : freeQty.doubleValue();
        double pbiPurchaseRate = line.isAmpp() ? purchaseRate.doubleValue() / unitsPerPack.doubleValue() : purchaseRate.doubleValue();
        double pbiRetailRate = line.isAmpp() ? retailRate.doubleValue() / unitsPerPack.doubleValue() : retailRate.doubleValue();

        return new LineValues(qty, freeQty, purchaseRate, retailRate, unitsPerPack, grossValue, netValue,
                purchaseValue, retailValue, netRate, pbiQty, pbiFreeQty, pbiPurchaseRate, pbiRetailRate);
    }

    public long saveLine(long billId, PurchaseOrderRequestLineData line) {
        LineValues v = computeLineValues(line);
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
                .setParameter(1, billId)
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

        // Branch on whether a PBI row already exists for this billItem_ID rather than
        // on line.getPharmaceuticalBillItemId(): the controller never learns the
        // generated PBI id back from an insert, so that DTO field stays null across
        // every subsequent save of the same line and a field-based branch would insert
        // a duplicate PHARMACEUTICALBILLITEM row on every save after the first.
        int updated = em.createNativeQuery(
                "UPDATE " + pharmBillItemTable()
                + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
                + " retailRate=?, retailRatePack=?, retailValue=? WHERE billItem_ID=?")
                .setParameter(1, pbiQty)
                .setParameter(2, pbiFreeQty)
                .setParameter(3, pbiPurchaseRate)
                .setParameter(4, purchaseRate.doubleValue())
                .setParameter(5, purchaseValue.doubleValue())
                .setParameter(6, pbiRetailRate)
                .setParameter(7, retailRate.doubleValue())
                .setParameter(8, retailValue.doubleValue())
                .setParameter(9, billItemId)
                .executeUpdate();

        if (updated == 0) {
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, qty, freeQty, purchaseRate, purchaseRatePack, purchaseValue,"
                + " retailRate, retailRatePack, retailValue, costRate, costRatePack, costValue,"
                + " createdAt, creater_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
                .setParameter(1, billItemId)
                .setParameter(2, pbiQty)
                .setParameter(3, pbiFreeQty)
                .setParameter(4, pbiPurchaseRate)
                .setParameter(5, purchaseRate.doubleValue())
                .setParameter(6, purchaseValue.doubleValue())
                .setParameter(7, pbiRetailRate)
                .setParameter(8, retailRate.doubleValue())
                .setParameter(9, retailValue.doubleValue())
                .setParameter(10, new Timestamp(new Date().getTime()))
                .setParameter(11, line.getCreaterId())
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
            java.util.List<BillItemFinanceDetails> existing = em.createQuery(jpql, BillItemFinanceDetails.class)
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

    private void evictLineCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(com.divudi.core.entity.Bill.class);
        cache.evict(com.divudi.core.entity.BillItem.class);
        cache.evict(com.divudi.core.entity.pharmacy.PharmaceuticalBillItem.class);
        cache.evict(BillItemFinanceDetails.class);
    }

    /**
     * Pure predicate extracted from legacy finalizeBillComponent()'s
     * totalUnits.compareTo(BigDecimal.ZERO) <= 0 check.
     * No em access, unit tested.
     */
    /**
     * Public (not package-private): called cross-EJB from
     * PurchaseOrderApprovingNativeSqlService.retireZeroQtyApprovedLines() via
     * the injected @EJB reference -- see computeLineValues()'s Javadoc above
     * for why package-private fails through the EJB proxy.
     */
    public boolean isZeroQtyLine(double qty, double freeQty) {
        return (qty + freeQty) <= 0;
    }

    /**
     * Promotes BILLTYPEATOMIC to PHARMACY_ORDER and marks bill as checked.
     * Sets checked=1, checkeAt=now, checkedBy_ID=editorId, editor_ID=editorId, editedAt=now.
     */
    public void finalizeBill(long billId, long editorId) {
        em.createNativeQuery(
            "UPDATE " + billTable()
            + " SET BILLTYPEATOMIC=?, checked=1, checkeAt=?, checkedBy_ID=?, editor_ID=?, editedAt=? WHERE ID=?")
            .setParameter(1, BillTypeAtomic.PHARMACY_ORDER.toString())
            .setParameter(2, new Timestamp(new Date().getTime()))
            .setParameter(3, editorId)
            .setParameter(4, editorId)
            .setParameter(5, new Timestamp(new Date().getTime()))
            .setParameter(6, billId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Finalizes the bill and retires zero-qty lines as one EJB call, so both
     * native writes share the same container-managed transaction. Calling
     * finalizeBill() and retireZeroQtyLines() as two separate EJB invocations
     * from the controller let the first commit even if the second later
     * failed, leaving the bill checked=1 with stale/unretired zero-qty lines.
     */
    public int finalizeAndRetireZeroQtyLines(long billId, long editorId) {
        finalizeBill(billId, editorId);
        return retireZeroQtyLines(billId, editorId);
    }

    /**
     * Retires any line where qty + freeQty <= 0 (zero-qty lines).
     * For surviving (non-retired) lines, sets remainingQty=qty and remainingFreeQty=freeQty.
     * Returns count of surviving lines with qty > 0.
     * Mirrors legacy finalizeBillComponent()'s totalBillItemsCount accumulation.
     */
    @SuppressWarnings("unchecked")
    public int retireZeroQtyLines(long billId, long retirerId) {
        java.util.List<Object[]> rows = em.createNativeQuery(
            "SELECT bi.ID, pbi.qty, pbi.freeQty FROM " + billItemTable() + " bi "
            + "JOIN " + pharmBillItemTable() + " pbi ON pbi.billItem_ID = bi.ID "
            + "WHERE bi.bill_ID = ? AND bi.retired = 0")
            .setParameter(1, billId)
            .getResultList();

        int survivingCount = 0;
        Timestamp now = new Timestamp(new Date().getTime());
        for (Object[] row : rows) {
            long billItemId = ((Number) row[0]).longValue();
            double qty = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            double freeQty = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

            if (isZeroQtyLine(qty, freeQty)) {
                em.createNativeQuery(
                    "UPDATE " + billItemTable() + " SET retired=1, retirer_ID=?, retiredAt=?, retireComments=? WHERE ID=?")
                    .setParameter(1, retirerId)
                    .setParameter(2, now)
                    .setParameter(3, "Retired at Finalising PO")
                    .setParameter(4, billItemId)
                    .executeUpdate();
                em.createNativeQuery(
                    "UPDATE " + pharmBillItemTable() + " SET retired=1, retirer_ID=?, retiredAt=? WHERE billItem_ID=?")
                    .setParameter(1, retirerId)
                    .setParameter(2, now)
                    .setParameter(3, billItemId)
                    .executeUpdate();
            } else {
                em.createNativeQuery(
                    "UPDATE " + pharmBillItemTable() + " SET remainingQty=qty, remainingFreeQty=freeQty WHERE billItem_ID=?")
                    .setParameter(1, billItemId)
                    .executeUpdate();
                survivingCount++;
            }
        }
        evictLineCache();
        return survivingCount;
    }

    /**
     * Retires a single billitem line (used when a user removes a line from
     * an in-progress purchase order request) and its associated
     * pharmacybillitem row. Mirrors legacy PurchaseOrderRequestController's
     * removeItem() persisting the retirement via billItemFacade.edit(bi).
     */
    public void retireLine(long billItemId, long retirerId) {
        Timestamp now = new Timestamp(new Date().getTime());
        em.createNativeQuery(
            "UPDATE " + billItemTable() + " SET retired=1, retirer_ID=?, retiredAt=? WHERE ID=?")
            .setParameter(1, retirerId)
            .setParameter(2, now)
            .setParameter(3, billItemId)
            .executeUpdate();
        em.createNativeQuery(
            "UPDATE " + pharmBillItemTable() + " SET retired=1, retirer_ID=?, retiredAt=? WHERE billItem_ID=?")
            .setParameter(1, retirerId)
            .setParameter(2, now)
            .setParameter(3, billItemId)
            .executeUpdate();
        evictLineCache();
    }
}
