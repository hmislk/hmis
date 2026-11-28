/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.bean.common.BillController;
import com.divudi.core.entity.BillEntry;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;


/**
 * Turns a list of {@link BillEntry} into inward service bills, bundled by
 * department and linked under one {@code INWARD_SERVICE_BATCH_BILL}.
 *
 * <p>This is the <i>Add Services / Investigations to BHT</i> settle pipeline,
 * lifted out of {@code BillBhtController} so the automatic admission charges
 * (issue #23594) run the very same process rather than a lookalike beside it.
 * Anything that produces bills a different way - one BillItem with no BillFee,
 * or a bill with no batch bill - cancels and refunds as zero, because both of
 * those paths read BillFees rather than BillItems.</p>
 *
 * <p>Two behaviours of the original are preserved deliberately, because
 * {@code BillBhtController.settleBillSurgery()} depends on them:</p>
 * <ul>
 * <li>the batch bill is linked over the caller's whole
 * {@link InwardServiceBillRequest#getBillCollector() collector} list, not just
 * the bills this call created, because {@code settleBillSurgery()} never resets
 * that list before settling;</li>
 * <li>the batch bill is returned but must not become the caller's
 * {@code batchBill} field. The original {@code saveBatchBill()} kept its bill
 * local, so the surgery path's later {@code saveEncounterComponents(...)} and
 * {@code updateBatchBill(...)} calls still operate on the <i>surgery</i> bill.</li>
 * </ul>
 *
 * <p>Known wrinkle preserved as-is: {@code calculateNumberOfBillsPerOrder()}
 * counts distinct {@code item.getTransDepartment()} while {@code putToBills}
 * groups by {@code item.getDepartment()}. Where those differ, the count and the
 * grouping disagree. Changing it would silently alter how existing hand-added
 * service bills are split.</p>
 *
 * @author Buddhika
 */
@Named
@ApplicationScoped
public class InwardServiceBillService implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private InwardServiceBillWriter writer;
    @Inject
    private BillController billController;

    /**
     * Creates the inward service bills for the given entries and links them
     * under a new batch bill, all in ONE transaction (#17562): the bills, bill
     * items, bill fees, components and the batch bill are saved together or not
     * at all. If the settle fails, the database has nothing of it and the
     * caller's objects are put back as they were, so the user can settle again.
     *
     * @return the bills created by this call and the batch bill they sit under
     * @throws InwardServiceBillException for a business error; any other failure
     * reaches the caller as it always did
     */
    public InwardServiceBillResult createServiceBills(InwardServiceBillRequest request) {
        InwardServiceBillRollbackState rollbackState = new InwardServiceBillRollbackState(request);
        // The settle uses up the session's part-payment balance as it totals each bill;
        // if the transaction rolls back, that balance goes back too.
        final double cashRemain = billController.getCashRemain();
        rollbackState.addUndo(() -> billController.setCashRemain(cashRemain));
        try {
            return writer.createServiceBills(request);
        } catch (RuntimeException e) {
            rollbackState.restore();
            throw e;
        }
    }
}
