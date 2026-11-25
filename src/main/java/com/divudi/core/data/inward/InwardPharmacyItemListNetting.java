package com.divudi.core.data.inward;

import com.divudi.core.entity.BillItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the rows of the inpatient pharmacy item list shown when the option
 * "Use Legacy Calculation Method for Pharmacy Summary in Inward Patient Profile"
 * is off: one row per live issue line, less the returns that refer to it. A return
 * that refers to no listed issue line (an accepted ward return, or a return of a
 * cancelled issue) is kept as its own row, so the list total equals the interim
 * bill's Medicine total (#26105).
 */
public final class InwardPharmacyItemListNetting {

    private InwardPharmacyItemListNetting() {
    }

    /**
     * @param items every line of the types in {@link InwardMedicineBillTypes#ALL_EXCLUDING_ISSUE_CANCELLATIONS},
     * in display order
     * @return the rows; issue lines are changed in place to hold the net quantity and value
     */
    public static List<BillItem> rows(List<BillItem> items) {
        List<BillItem> issues = new ArrayList<>();
        List<BillItem> returns = new ArrayList<>();
        for (BillItem bi : items) {
            if (bi == null || bi.getBill() == null) {
                continue;
            }
            if (!InwardMedicineBillTypes.isCountedInLiveList(bi.getBill().getBillTypeAtomic(), bi.getBill().isCancelled())) {
                continue;
            }
            if (InwardMedicineBillTypes.isItemListReturn(bi.getBill().getBillTypeAtomic())) {
                returns.add(bi);
            } else {
                issues.add(bi);
            }
        }
        List<BillItem> rows = new ArrayList<>(issues);
        for (BillItem ret : returns) {
            BillItem target = null;
            if (ret.getReferanceBillItem() != null) {
                for (BillItem issue : issues) {
                    if (ret.getReferanceBillItem().equals(issue)) {
                        target = issue;
                        break;
                    }
                }
            }
            if (target == null) {
                rows.add(ret);
            } else {
                double qty = target.getQty() != null ? target.getQty() : 0.0;
                double retQty = ret.getQty() != null ? Math.abs(ret.getQty()) : 0.0;
                target.setQty(qty - retQty);
                target.setNetValue(target.getNetValue() - Math.abs(ret.getNetValue()));
            }
        }
        return rows;
    }

    /** Sum of the rows as the list footer shows it: issues add, returns deduct. */
    public static double total(List<BillItem> rows) {
        double total = 0.0;
        for (BillItem bi : rows) {
            total += rowValue(bi);
        }
        return total;
    }

    /**
     * The value a row adds to the list total. An issue row already holds its
     * value less the returns that refer to it.
     */
    public static double rowValue(BillItem bi) {
        if (bi == null || bi.getBill() == null) {
            return 0.0;
        }
        return InwardMedicineBillTypes.signedRowValue(bi.getBill().getBillTypeAtomic(), bi.getNetValue());
    }
}
