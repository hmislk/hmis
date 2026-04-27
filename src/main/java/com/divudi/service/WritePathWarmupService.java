package com.divudi.service;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.entity.pharmacy.StockHistory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

/**
 * Warms the EclipseLink write path (InsertObjectQuery / UpdateObjectQuery
 * descriptors, sequence allocators, bi-directional cascade compilation) for
 * the entities written during BHT Settle.
 *
 * Read-path warmup ({@link EntityWarmupService}) only exercises ReadObjectQuery
 * — it doesn't compile the write descriptors. The first user-driven Settle
 * therefore still pays a 3–4 second tax to compile InsertObjectQuery for
 * Bill, BillItem, PharmaceuticalBillItem, StockHistory, BillFinanceDetails,
 * and BillItemFinanceDetails. This bean does that compilation at deploy time.
 *
 * Uses bean-managed transactions so we can guarantee rollback even if the
 * entity manager surfaces an exception during persist/flush. No dummy rows
 * ever reach the database.
 *
 * Issue #20138.
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class WritePathWarmupService {

    private static final Logger LOGGER = Logger.getLogger(WritePathWarmupService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    @Resource
    private UserTransaction utx;

    public void warmWritePath() {
        long total = System.currentTimeMillis();

        warmEntity("Bill", Bill::new);
        warmEntity("BillItem", BillItem::new);
        warmEntity("PharmaceuticalBillItem", PharmaceuticalBillItem::new);
        warmEntity("StockHistory", StockHistory::new);
        warmEntity("BillFinanceDetails", BillFinanceDetails::new);
        warmEntity("BillItemFinanceDetails", BillItemFinanceDetails::new);

        LOGGER.log(Level.INFO, "[WritePathWarmup] TOTAL: {0}ms",
                System.currentTimeMillis() - total);
    }

    private interface Factory {
        Object create();
    }

    private void warmEntity(String label, Factory factory) {
        boolean txStarted = false;
        try {
            long t = System.currentTimeMillis();
            utx.begin();
            txStarted = true;

            Object e = factory.create();
            em.persist(e);
            em.flush();   // forces InsertObjectQuery descriptor compile + IDENTITY round-trip

            utx.rollback(); // discard the dummy row
            txStarted = false;

            LOGGER.log(Level.INFO, "[WritePathWarmup] {0}: {1}ms",
                    new Object[]{label, System.currentTimeMillis() - t});
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "[WritePathWarmup] " + label + " skipped: " + ex.getMessage());
            if (txStarted) {
                try {
                    utx.rollback();
                } catch (Exception rbEx) {
                    LOGGER.log(Level.WARNING, "[WritePathWarmup] rollback failed for " + label
                            + ": " + rbEx.getMessage());
                }
            }
        }
    }
}
