package com.divudi.service.pharmacy;

import com.divudi.core.data.BillTypeAtomic;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Atomic compare-and-set guards for the Direct Purchase Finalize/Approve
 * transitions, closing the race where two sessions (two users, or one user
 * with two tabs/a double-click) both act on the same bill concurrently.
 *
 * Modeled on the atomic-claim technique in
 * {@link GrnApprovingNativeSqlService#approveGrn} -- a single native UPDATE
 * with the current state in the WHERE clause, checked via the affected-row
 * count -- rather than the read-then-write pattern
 * (reload bill, check a boolean in Java, then facade.edit()) previously used
 * in {@code PharmacyDirectPurchaseController}. That pattern has an
 * unavoidable window between the check and the write where a second session
 * can pass the same check before either write lands; a single atomic UPDATE
 * has no such window since the database itself resolves the race.
 *
 * Deliberately narrower in scope than {@code GrnApprovingNativeSqlService}:
 * only the state-flip itself is native/atomic here. The stock/ItemBatch/
 * StockHistory writes and the discount/tax/expense distribution math stay in
 * {@code PharmacyDirectPurchaseController} exactly as before (already JPA,
 * already correct) -- the claim below is called immediately before that
 * existing logic runs, so only the session that wins the claim ever reaches
 * it, which is what actually prevents double stock mutation. Related issue:
 * #23005 (comment thread on PR #23009).
 */
@Stateless
public class DirectPurchaseApprovingNativeSqlService {

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    private String tBill;

    private String resolveTable(String upperName) {
        Object name = em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = ? LIMIT 1")
                .setParameter(1, upperName)
                .getSingleResult();
        return name.toString();
    }

    private String billTable() {
        if (tBill == null) {
            tBill = resolveTable("BILL");
        }
        return tBill;
    }

    private void evictCache() {
        em.getEntityManagerFactory().getCache().evict(com.divudi.core.entity.Bill.class);
    }

    /**
     * Atomically transitions a Direct Purchase draft from "finalized,
     * awaiting approval" to "approved" -- flips BILLTYPEATOMIC from
     * PHARMACY_DIRECT_PURCHASE_PRE to PHARMACY_DIRECT_PURCHASE and sets
     * CHECKED=1, in one UPDATE gated on the bill still being in that exact
     * pre-approval state. If another session already approved (or is
     * concurrently approving) this same bill, this call affects 0 rows and
     * returns false -- the caller must not proceed to add stock.
     *
     * @return true if this call won the claim and may proceed; false if the
     * bill was already approved (by this or another session) or is not in a
     * claimable state.
     */
    public boolean claimForApproval(long billId, long approveUserId) {
        int claimed = em.createNativeQuery(
                "UPDATE " + billTable()
                + " SET BILLTYPEATOMIC=?, CHECKED=1, CHECKEDBY_ID=?, CHECKEAT=?"
                + " WHERE ID=? AND BILLTYPEATOMIC=? AND COMPLETED=1 AND CHECKED=0")
                .setParameter(1, BillTypeAtomic.PHARMACY_DIRECT_PURCHASE.toString())
                .setParameter(2, approveUserId)
                .setParameter(3, new Timestamp(new Date().getTime()))
                .setParameter(4, billId)
                .setParameter(5, BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_PRE.toString())
                .executeUpdate();
        evictCache();
        return claimed == 1;
    }

    /**
     * Atomically transitions a Direct Purchase draft from "in progress" to
     * "finalized, awaiting approval" -- sets COMPLETED=1 (plus the
     * completedBy/completedAt/editor/editedAt audit columns) gated on the
     * bill not already being completed. Returns false if another session
     * already finalized (or approved -- approval implies completed=1 too)
     * this same bill.
     *
     * @return true if this call won the claim and may proceed; false
     * otherwise.
     */
    public boolean claimForFinalize(long billId, long editorId) {
        Timestamp now = new Timestamp(new Date().getTime());
        int claimed = em.createNativeQuery(
                "UPDATE " + billTable()
                + " SET COMPLETED=1, COMPLETEDBY_ID=?, COMPLETEDAT=?, EDITOR_ID=?, EDITEDAT=?"
                + " WHERE ID=? AND COMPLETED=0")
                .setParameter(1, editorId)
                .setParameter(2, now)
                .setParameter(3, editorId)
                .setParameter(4, now)
                .setParameter(5, billId)
                .executeUpdate();
        evictCache();
        return claimed == 1;
    }
}
