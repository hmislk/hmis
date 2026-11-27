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
    private final Bill currentReferenceBill;
    private final boolean currentConfirmedFinalBill;
    private final Bill originalBill;
    private final Long originalBillId;
    private final int originalBillItemCount;
    private final double originalBillDiscount;
    private final double originalBillNetTotal;

    FinalBillSaveRollbackState(Bill current, PatientEncounter encounter) {
        this(current, encounter, null);
    }

    /**
     * @param originalBill the page's original bill as it is before the save, or
     * null when the page has none yet; the save may create or change it.
     */
    FinalBillSaveRollbackState(Bill current, PatientEncounter encounter, Bill originalBill) {
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
        this.currentReferenceBill = current.getReferenceBill();
        this.currentConfirmedFinalBill = current.isConfirmedFinalBill();
        this.originalBill = originalBill;
        if (originalBill != null) {
            this.originalBillId = originalBill.getId();
            this.originalBillItemCount = originalBill.getBillItems() == null ? 0 : originalBill.getBillItems().size();
            this.originalBillDiscount = originalBill.getDiscount();
            this.originalBillNetTotal = originalBill.getNetTotal();
        } else {
            this.originalBillId = null;
            this.originalBillItemCount = 0;
            this.originalBillDiscount = 0;
            this.originalBillNetTotal = 0;
        }
    }

    /**
     * The original bill the page held before the save, or null if it held none.
     * The caller puts this back as the page's original bill after a rollback.
     */
    Bill getOriginalBillBeforeSave() {
        return originalBill;
    }

    /** Puts the page copies back as they were before the save started. */
    void restore() {
        current.setId(currentId);
        List<BillItem> items = current.getBillItems();
        if (items != null && items.size() > billItemCount) {
            items.subList(billItemCount, items.size()).clear();
        }
        current.setReferenceBill(currentReferenceBill);
        current.setConfirmedFinalBill(currentConfirmedFinalBill);
        if (originalBill != null) {
            originalBill.setId(originalBillId);
            List<BillItem> originalItems = originalBill.getBillItems();
            if (originalItems != null && originalItems.size() > originalBillItemCount) {
                originalItems.subList(originalBillItemCount, originalItems.size()).clear();
            }
            originalBill.setDiscount(originalBillDiscount);
            originalBill.setNetTotal(originalBillNetTotal);
        }
        encounter.setFinalBill(encounterFinalBill);
        encounter.setGrantTotal(encounterGrantTotal);
        encounter.setDiscount(encounterDiscount);
        encounter.setNetTotal(encounterNetTotal);
        encounter.setPaymentFinalized(encounterPaymentFinalized);
        encounter.setCreditUsedAmount(encounterCreditUsedAmount);
    }
}
