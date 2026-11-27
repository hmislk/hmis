package com.divudi.bean.inward;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.PatientEncounter;
import java.util.List;

/**
 * Page-copy state of a Save Final Bill, captured before the single locked
 * transaction runs and put back if that transaction rolls back (#17562).
 * <p>
 * The database undoes every row of a failed save. The session's own objects do
 * not undo themselves: the new bill keeps the id the insert gave it, the bill
 * items stay in its list, and the admission copy still shows the payment as
 * finalized. Left like that, the next Save Final Bill would answer "already
 * saved". Package-private and free of CDI so it can be unit tested.
 */
final class FinalBillSaveRollbackState {

    private final Bill current;
    private final Long currentId;
    private final int billItemCount;
    private final PatientEncounter encounter;
    private final Bill encounterFinalBill;
    private final double encounterGrantTotal;
    private final double encounterDiscount;
    private final double encounterNetTotal;
    private final boolean encounterPaymentFinalized;
    private final double encounterCreditUsedAmount;

    FinalBillSaveRollbackState(Bill current, PatientEncounter encounter) {
        this.current = current;
        this.currentId = current.getId();
        this.billItemCount = current.getBillItems() == null ? 0 : current.getBillItems().size();
        this.encounter = encounter;
        this.encounterFinalBill = encounter.getFinalBill();
        this.encounterGrantTotal = encounter.getGrantTotal();
        this.encounterDiscount = encounter.getDiscount();
        this.encounterNetTotal = encounter.getNetTotal();
        this.encounterPaymentFinalized = encounter.isPaymentFinalized();
        this.encounterCreditUsedAmount = encounter.getCreditUsedAmount();
    }

    /** Puts the page copies back as they were before the save started. */
    void restore() {
        current.setId(currentId);
        List<BillItem> items = current.getBillItems();
        if (items != null && items.size() > billItemCount) {
            items.subList(billItemCount, items.size()).clear();
        }
        encounter.setFinalBill(encounterFinalBill);
        encounter.setGrantTotal(encounterGrantTotal);
        encounter.setDiscount(encounterDiscount);
        encounter.setNetTotal(encounterNetTotal);
        encounter.setPaymentFinalized(encounterPaymentFinalized);
        encounter.setCreditUsedAmount(encounterCreditUsedAmount);
    }
}
