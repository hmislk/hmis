package com.divudi.service.pharmacy;

import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.pharmacy.GrnLineData;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * Native SQL write path for the GRN (Goods Received Note) "Manage Costing =
 * true" draft bill -- the pre-Approve stage of GrnCostingController.
 * Directly modeled on PurchaseOrderRequestNativeSqlService (issue #22727 /
 * PR #22871); read that class's Javadoc first if extending this one.
 * Approve-time writes (batch/expiry capture, PO remaining-qty decrement,
 * BILLTYPEATOMIC promotion to PHARMACY_GRN) belong to a separate
 * GrnApprovingNativeSqlService, not this class -- this service only takes
 * the bill from creation through the checked/finalized PRE state.
 * Related issue: #22874
 */
@Stateless
public class GrnCostingNativeSqlService {

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
     * native INSERT. A native INSERT here would leave DTYPE null, producing
     * "Missing class indicator field from database row" on the very next JPA
     * read of this bill. Same fix, same precedent as
     * PurchaseOrderRequestNativeSqlService.createDraftBill() -- see that
     * method's Javadoc for the full history.
     */
    public long createDraftBill(long departmentId, long institutionId, Long referenceBillId, long createrId,
                                 String deptId, String insId, Long supplierInstitutionId, Long referenceInstitutionId,
                                 Long toInstitutionId) {
        BilledBill bill = new BilledBill();
        bill.setBillType(BillType.PharmacyGrnBill);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_GRN_PRE);
        bill.setDepartment(em.getReference(Department.class, departmentId));
        bill.setInstitution(em.getReference(Institution.class, institutionId));
        bill.setFromDepartment(em.getReference(Department.class, departmentId));
        // fromInstitution is the SUPPLIER (legacy: approveBill.getToInstitution()),
        // not the session's own institution -- falls back to the session
        // institution only if the caller genuinely has no supplier to hand (should
        // not happen on the real create-from-PO path).
        bill.setFromInstitution(em.getReference(Institution.class,
                supplierInstitutionId != null ? supplierInstitutionId : institutionId));
        if (referenceInstitutionId != null) {
            bill.setReferenceInstitution(em.getReference(Institution.class, referenceInstitutionId));
        }
        if (toInstitutionId != null) {
            bill.setToInstitution(em.getReference(Institution.class, toInstitutionId));
        }
        if (referenceBillId != null) {
            bill.setReferenceBill(em.getReference(Bill.class, referenceBillId));
        }
        bill.setCreater(em.getReference(WebUser.class, createrId));
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
     * Persists the GRN draft bill's header fields.
     * NOTE: Bill has no distinct "bankRefNo" column -- only chequeRefNo
     * exists (confirmed against Bill.java and GrnCostingController's usage,
     * which only ever calls getChequeRefNo()/setChequeRefNo()). bankRefNo is
     * accepted here to satisfy the intended controller call shape but is not
     * written to any column; only chequeRefNo is persisted.
     * <p>
     * invoiceDate added for #22892: this method previously never wrote it at
     * all (Bill.invoiceDate is a real column; it was simply missing from
     * this UPDATE), so it reverted to blank on every reload -- same root
     * cause shape as the Batch No/Date of Expiry fix in saveLine() above.
     * This method has exactly one caller (GrnCostingNativeSqlController),
     * so extending the signature directly is safe.
     */
    public void updateDraftBillHeader(long billId, String invoiceNumber, Date invoiceDate, String paymentMethod, Integer creditDuration,
                                       String bankRefNo, String chequeRefNo, String comments, String departmentType,
                                       double discount, long editorId) {
        em.createNativeQuery(
            "UPDATE " + billTable()
            + " SET invoiceNumber=?, invoiceDate=?, paymentMethod=?, creditDuration=?, chequeRefNo=?,"
            + " comments=?, departmentType=?, discount=?, editor_ID=?, editedAt=NOW() WHERE ID=?")
            .setParameter(1, invoiceNumber)
            .setParameter(2, invoiceDate)
            .setParameter(3, paymentMethod)
            .setParameter(4, creditDuration != null ? creditDuration : 0)
            .setParameter(5, chequeRefNo)
            .setParameter(6, comments)
            .setParameter(7, departmentType)
            .setParameter(8, discount)
            .setParameter(9, editorId)
            .setParameter(10, billId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Persists the bill-level netTotal/total columns. Same rationale as
     * PurchaseOrderRequestNativeSqlService.updateBillTotals(): nothing else
     * in the native save path writes these back to the DB.
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
     * Carries back both generated/resolved primary keys from {@link #saveLine}
     * so the caller can set them on its in-memory BillItem AND
     * PharmaceuticalBillItem entities -- a native INSERT never reports a
     * generated PK back onto the JPA entity object the way em.persist() does,
     * so without this, a caller that reads pharmaceuticalBillItem.getId()
     * later in the same session (e.g. building GrnApproveLineData for
     * Approve, before any DB reload happens) would silently see null.
     */
    public static final class SavedLineIds {
        public final long billItemId;
        public final long pharmaceuticalBillItemId;

        public SavedLineIds(long billItemId, long pharmaceuticalBillItemId) {
            this.billItemId = billItemId;
            this.pharmaceuticalBillItemId = pharmaceuticalBillItemId;
        }
    }

    /**
     * Upserts a single GRN line's billitem + pharmaceuticalbillitem rows,
     * then its BillItemFinanceDetails. Returns both the billItemId and the
     * pharmaceuticalbillitem id so the controller can track them back onto
     * its in-memory model (see {@link SavedLineIds}).
     */
    public SavedLineIds saveLine(long billId, GrnLineData line) {
        long billItemId;
        if (line.getBillItemId() == null) {
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (bill_ID, item_ID, referanceBillItem_ID, qty, netValue, grossValue, Rate, netRate,"
                + " discountRate, createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, inwardChargeType, searialNo)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,0,0,0,1,'Medicine',?)")
                .setParameter(1, billId)
                .setParameter(2, line.getItemId())
                .setParameter(3, line.getReferenceBillItemId())
                .setParameter(4, line.getQuantity())
                // billitem.netValue is negative for purchases by this codebase's
                // sign convention (legacy: bi.setNetValue(0 - biNetValue), confirmed
                // in GrnCostingController's recalculateFinancialsBeforeAddingBillItem*
                // methods) -- GrnLineData.lineNetTotal itself stays positive since
                // BillItemFinanceDetails.lineNetTotal is positive by convention.
                .setParameter(5, -line.getLineNetTotal())
                .setParameter(6, line.getLineGrossTotal())
                .setParameter(7, line.getLineGrossRate())
                .setParameter(8, line.getLineNetRate())
                .setParameter(9, line.getLineDiscountRate())
                .setParameter(10, new Timestamp(new Date().getTime()))
                .setParameter(11, line.getCreaterId())
                .setParameter(12, line.getSerialNo())
                .executeUpdate();
            billItemId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        } else {
            billItemId = line.getBillItemId();
            em.createNativeQuery(
                "UPDATE " + billItemTable()
                + " SET item_ID=?, referanceBillItem_ID=?, qty=?, netValue=?, grossValue=?, Rate=?, netRate=?,"
                + " discountRate=?, searialNo=? WHERE ID=?")
                .setParameter(1, line.getItemId())
                .setParameter(2, line.getReferenceBillItemId())
                .setParameter(3, line.getQuantity())
                // Same negative sign convention as the INSERT branch above.
                .setParameter(4, -line.getLineNetTotal())
                .setParameter(5, line.getLineGrossTotal())
                .setParameter(6, line.getLineGrossRate())
                .setParameter(7, line.getLineNetRate())
                .setParameter(8, line.getLineDiscountRate())
                .setParameter(9, line.getSerialNo())
                .setParameter(10, billItemId)
                .executeUpdate();
        }

        // Branch on whether a PBI row already exists for this billItem_ID rather
        // than on line.getPharmaceuticalBillItemId(): a native INSERT never
        // reports its generated PK back onto the DTO, so that field stays null
        // across every subsequent save of the same line -- a field-based branch
        // would insert a duplicate PHARMACEUTICALBILLITEM row on every save
        // after the first. Same pattern as
        // PurchaseOrderRequestNativeSqlService.saveLine().
        int updated = em.createNativeQuery(
                "UPDATE " + pharmBillItemTable()
                + " SET qty=?, freeQty=?, purchaseRate=?, purchaseRatePack=?, purchaseValue=?,"
                + " retailRate=?, retailRatePack=?, retailValue=?, wholesaleRate=?, wholesaleRatePack=?,"
                // doe/stringValue (Batch No / Date of Expiry) were the missing
                // columns behind #22892 -- entered here at Save time but never
                // written, so they silently reverted to blank on any reload
                // before Finalize/Approve.
                + " doe=?, stringValue=?"
                + " WHERE billItem_ID=?")
                .setParameter(1, line.getQuantityByUnits())
                .setParameter(2, line.getFreeQuantityByUnits())
                .setParameter(3, line.isAmpp() ? divide(line.getGrossRate(), line.getUnitsPerPack()) : line.getGrossRate())
                .setParameter(4, line.getGrossRate())
                .setParameter(5, line.getValueAtPurchaseRate())
                .setParameter(6, line.isAmpp() ? divide(line.getRetailSaleRate(), line.getUnitsPerPack()) : line.getRetailSaleRate())
                .setParameter(7, line.getRetailSaleRate())
                .setParameter(8, line.getValueAtRetailRate())
                .setParameter(9, line.isAmpp() ? divide(line.getWholesaleRate(), line.getUnitsPerPack()) : line.getWholesaleRate())
                .setParameter(10, line.getWholesaleRate())
                .setParameter(11, line.getDoe())
                .setParameter(12, line.getBatchNumber())
                .setParameter(13, billItemId)
                .executeUpdate();

        long pharmaceuticalBillItemId;
        if (updated == 0) {
            em.createNativeQuery(
                "INSERT INTO " + pharmBillItemTable()
                + " (billItem_ID, qty, freeQty, purchaseRate, purchaseRatePack, purchaseValue,"
                + " retailRate, retailRatePack, retailValue, wholesaleRate, wholesaleRatePack,"
                + " doe, stringValue,"
                + " costRate, costRatePack, costValue, createdAt, creater_ID)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
                .setParameter(1, billItemId)
                .setParameter(2, line.getQuantityByUnits())
                .setParameter(3, line.getFreeQuantityByUnits())
                .setParameter(4, line.isAmpp() ? divide(line.getGrossRate(), line.getUnitsPerPack()) : line.getGrossRate())
                .setParameter(5, line.getGrossRate())
                .setParameter(6, line.getValueAtPurchaseRate())
                .setParameter(7, line.isAmpp() ? divide(line.getRetailSaleRate(), line.getUnitsPerPack()) : line.getRetailSaleRate())
                .setParameter(8, line.getRetailSaleRate())
                .setParameter(9, line.getValueAtRetailRate())
                .setParameter(10, line.isAmpp() ? divide(line.getWholesaleRate(), line.getUnitsPerPack()) : line.getWholesaleRate())
                .setParameter(11, line.getWholesaleRate())
                .setParameter(12, line.getDoe())
                .setParameter(13, line.getBatchNumber())
                .setParameter(14, new Timestamp(new Date().getTime()))
                .setParameter(15, line.getCreaterId())
                .executeUpdate();
            pharmaceuticalBillItemId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        } else {
            // The UPDATE above doesn't tell us the row's own PK -- resolve it
            // explicitly so the caller can set it on the in-memory
            // PharmaceuticalBillItem entity (see SavedLineIds).
            pharmaceuticalBillItemId = ((Number) em.createNativeQuery(
                    "SELECT ID FROM " + pharmBillItemTable() + " WHERE billItem_ID=?")
                    .setParameter(1, billItemId)
                    .getSingleResult()).longValue();
        }

        saveBillItemFinanceDetails(billItemId, line);

        evictLineCache();
        return new SavedLineIds(billItemId, pharmaceuticalBillItemId);
    }

    private double divide(double value, double divisor) {
        return divisor > 0 ? value / divisor : value;
    }

    /**
     * JPA persist/merge of BillItemFinanceDetails (IDENTITY PK), followed by
     * a native UPDATE to link it back onto billitem.BILLITEMFINANCEDETAILS_ID.
     * Same two-step pattern as
     * PurchaseOrderRequestNativeSqlService.saveBillItemFinanceDetails().
     */
    private void saveBillItemFinanceDetails(long billItemId, GrnLineData line) {
        // Key the existing-row lookup on the resolved billItemId parameter,
        // not line.getBillItemId() -- the DTO field is unset on a first-ever
        // save (correctly falls through to "new" below either way), but a
        // future caller passing a resolved billItemId with an unset DTO
        // field would otherwise create and re-link a duplicate BIFD row.
        String jpql = "SELECT bi.billItemFinanceDetails FROM BillItem bi WHERE bi.id = :id";
        List<BillItemFinanceDetails> existing = em.createQuery(jpql, BillItemFinanceDetails.class)
                .setParameter("id", billItemId)
                .getResultList();
        BillItemFinanceDetails bifd = (existing != null && !existing.isEmpty() && existing.get(0) != null)
                ? existing.get(0)
                : new BillItemFinanceDetails();

        bifd.setLineGrossRate(BigDecimal.valueOf(line.getLineGrossRate()));
        bifd.setLineDiscountRate(BigDecimal.valueOf(line.getLineDiscountRate()));
        bifd.setLineNetRate(BigDecimal.valueOf(line.getLineNetRate()));
        bifd.setGrossRate(BigDecimal.valueOf(line.getGrossRate()));
        bifd.setLineGrossTotal(BigDecimal.valueOf(line.getLineGrossTotal()));
        bifd.setLineNetTotal(BigDecimal.valueOf(line.getLineNetTotal()));
        bifd.setGrossTotal(BigDecimal.valueOf(line.getGrossTotal()));
        bifd.setRetailSaleRate(BigDecimal.valueOf(line.getRetailSaleRate()));
        bifd.setRetailSaleRatePerUnit(BigDecimal.valueOf(line.getRetailSaleRatePerUnit()));
        bifd.setWholesaleRate(BigDecimal.valueOf(line.getWholesaleRate()));
        bifd.setWholesaleRatePerUnit(BigDecimal.valueOf(line.getWholesaleRatePerUnit()));
        bifd.setUnitsPerPack(BigDecimal.valueOf(line.getUnitsPerPack()));
        bifd.setQuantity(BigDecimal.valueOf(line.getQuantity()));
        bifd.setFreeQuantity(BigDecimal.valueOf(line.getFreeQuantity()));
        bifd.setQuantityByUnits(BigDecimal.valueOf(line.getQuantityByUnits()));
        bifd.setFreeQuantityByUnits(BigDecimal.valueOf(line.getFreeQuantityByUnits()));
        bifd.setValueAtPurchaseRate(BigDecimal.valueOf(line.getValueAtPurchaseRate()));
        bifd.setValueAtRetailRate(BigDecimal.valueOf(line.getValueAtRetailRate()));
        bifd.setLineCost(BigDecimal.valueOf(line.getLineCost()));
        bifd.setLineCostRate(BigDecimal.valueOf(line.getLineCostRate()));
        bifd.setValueAtCostRate(BigDecimal.valueOf(line.getValueAtCostRate()));
        bifd.setTotalCostRate(BigDecimal.valueOf(line.getTotalCostRate()));

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
     * Upserts a GRN expense row -- GRN-only concept with no PO precedent.
     * Expense rows are plain BillItem rows linked via the expenseBill_ID FK
     * (BillItem.expenseBill, confirmed against BillItem.java and
     * GrnCostingController.addExpense()) rather than the normal bill_ID FK;
     * no PharmaceuticalBillItem row is created for them. GrnLineData's
     * lineNetTotal/lineGrossTotal map onto BillItem's actual netValue/
     * grossValue columns -- BillItem has no "netTotal"/"grossTotal" columns
     * of its own (those names live on BillItemFinanceDetails/
     * BillFinanceDetails instead).
     */
    public long saveExpenseLine(long billId, GrnLineData expenseLine) {
        long billItemId;
        if (expenseLine.getBillItemId() == null) {
            em.createNativeQuery(
                "INSERT INTO " + billItemTable()
                + " (expenseBill_ID, item_ID, qty, netValue, grossValue, Rate, netRate,"
                + " createdAt, creater_ID, retired, refunded, billItemRefunded,"
                + " consideredForCosting, searialNo)"
                + " VALUES (?,?,?,?,?,?,?,?,?,0,0,0,?,?)")
                .setParameter(1, billId)
                .setParameter(2, expenseLine.getItemId())
                .setParameter(3, expenseLine.getQuantity())
                .setParameter(4, expenseLine.getLineNetTotal())
                .setParameter(5, expenseLine.getLineGrossTotal())
                .setParameter(6, expenseLine.getLineGrossRate())
                .setParameter(7, expenseLine.getLineNetRate())
                .setParameter(8, new Timestamp(new Date().getTime()))
                .setParameter(9, expenseLine.getCreaterId())
                .setParameter(10, expenseLine.isConsideredForCosting() ? 1 : 0)
                .setParameter(11, expenseLine.getSerialNo())
                .executeUpdate();
            billItemId = ((Number) em.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).longValue();
        } else {
            billItemId = expenseLine.getBillItemId();
            em.createNativeQuery(
                "UPDATE " + billItemTable()
                + " SET item_ID=?, qty=?, netValue=?, grossValue=?, Rate=?, netRate=?, consideredForCosting=?, searialNo=?"
                + " WHERE ID=?")
                .setParameter(1, expenseLine.getItemId())
                .setParameter(2, expenseLine.getQuantity())
                .setParameter(3, expenseLine.getLineNetTotal())
                .setParameter(4, expenseLine.getLineGrossTotal())
                .setParameter(5, expenseLine.getLineGrossRate())
                .setParameter(6, expenseLine.getLineNetRate())
                .setParameter(7, expenseLine.isConsideredForCosting() ? 1 : 0)
                .setParameter(8, expenseLine.getSerialNo())
                .setParameter(9, billItemId)
                .executeUpdate();
        }
        evictLineCache();
        return billItemId;
    }

    /**
     * JPA persist/merge of the bill-level BillFinanceDetails (IDENTITY PK),
     * followed by a native UPDATE to link it back onto
     * bill.BILLFINANCEDETAILS_ID on first creation -- same two-step pattern
     * as saveBillItemFinanceDetails().
     */
    public void saveBillFinanceDetails(long billId, double totalDiscount, double totalExpense, double totalTax) {
        Bill billRef = em.find(Bill.class, billId);
        BillFinanceDetails bfd = billRef != null ? billRef.getBillFinanceDetails() : null;
        boolean isNew = (bfd == null || bfd.getId() == null);
        if (bfd == null) {
            bfd = new BillFinanceDetails();
        }

        bfd.setTotalDiscount(BigDecimal.valueOf(totalDiscount));
        bfd.setTotalExpense(BigDecimal.valueOf(totalExpense));
        bfd.setTotalTaxValue(BigDecimal.valueOf(totalTax));

        if (isNew) {
            bfd.setCreatedAt(new Date());
            em.persist(bfd);
            em.flush();
            em.createNativeQuery("UPDATE " + billTable() + " SET BILLFINANCEDETAILS_ID=? WHERE ID=?")
                    .setParameter(1, bfd.getId())
                    .setParameter(2, billId)
                    .executeUpdate();
        } else {
            em.merge(bfd);
        }
        evictCache();
    }

    /**
     * Marks the GRN draft bill as completed. Does NOT touch billTypeAtomic --
     * unlike PurchaseOrderRequestNativeSqlService.finalizeBill(), which
     * promotes BILLTYPEATOMIC from _PRE to the final atomic type at this
     * step, GRN's billTypeAtomic deliberately stays PHARMACY_GRN_PRE until
     * Approve (GrnApprovingNativeSqlService.finalizeApprove(), promoting to
     * PHARMACY_GRN).
     */
    public void finalizeBill(long billId, long editorId) {
        em.createNativeQuery(
            "UPDATE " + billTable()
            + " SET completed=1, completedBy_ID=?, completedAt=NOW(), editor_ID=?, editedAt=NOW() WHERE ID=?")
            .setParameter(1, editorId)
            .setParameter(2, editorId)
            .setParameter(3, billId)
            .executeUpdate();
        evictCache();
    }

    /**
     * Retires a single billitem line (and its pharmaceuticalbillitem row)
     * when a user removes a line from an in-progress GRN. Same shape as
     * PurchaseOrderRequestNativeSqlService.retireLine().
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

    private void evictCache() {
        javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
        cache.evict(Bill.class);
        cache.evict(BillItem.class);
        cache.evict(PharmaceuticalBillItem.class);
        cache.evict(BillItemFinanceDetails.class);
        cache.evict(BillFinanceDetails.class);
    }

    private void evictLineCache() {
        evictCache();
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

    private String billItemTable() {
        if (tBillItem == null) tBillItem = resolveTable("BILLITEM");
        return tBillItem;
    }

    private String pharmBillItemTable() {
        if (tPharmBillItem == null) tPharmBillItem = resolveTable("PHARMACEUTICALBILLITEM");
        return tPharmBillItem;
    }
}
