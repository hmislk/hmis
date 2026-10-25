package com.divudi.service.inward;

import com.divudi.core.entity.inward.TimedItem;
import com.divudi.core.entity.inward.TimedItemFee;
import com.divudi.core.facade.TimedItemFeeFacade;
import com.divudi.core.util.CommonFunctions;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Prices timed services and time-based room charges from their TimedItemFee
 * tiers. Moved out of InwardBeanController (which now delegates here) so that
 * stateless report services can price a running timed service the same way the
 * interim bill does (#24548).
 */
@Stateless
public class TimedItemPricingService {

    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;

    public List<TimedItemFee> getAllTimedItemFees(TimedItem ti) {
        if (ti == null || ti.getId() == null) {
            return new ArrayList<>();
        }
        HashMap hm = new HashMap();
        hm.put("id", ti.getId());
        // The id tiebreak matters: getFeeForBlock() indexes into this list positionally,
        // so any two fees sharing a sortOrder would otherwise order arbitrarily and the
        // same stay could be billed at a different tier on different runs. New fees can
        // no longer collide (TimedItemFeeRules), but rows saved before that still can.
        String sql = "SELECT tif FROM TimedItemFee tif WHERE tif.retired=false AND tif.item.id=:id ORDER BY tif.sortOrder ASC, tif.id ASC";
        List<TimedItemFee> fees = timedItemFeeFacade.findByJpql(sql, hm);
        return fees != null ? fees : new ArrayList<>();
    }

    public TimedItemFee getFeeForBlock(List<TimedItemFee> fees, int blockNumber) {
        if (fees == null || fees.isEmpty()) {
            return null;
        }
        if (blockNumber <= fees.size()) {
            return fees.get(blockNumber - 1);
        }
        TimedItemFee lastFee = fees.get(fees.size() - 1);
        if (lastFee.isRepeating() || fees.size() == 1) {
            return lastFee;
        }
        return null;
    }

    public double calTotalTimedChargeForItem(TimedItem ti, Date fromTime, Date toTime, boolean foreigner) {
        List<TimedItemFee> fees = getAllTimedItemFees(ti);
        if (fees.isEmpty()) {
            return 0.0;
        }
        TimedItemFee firstFee = fees.get(0);
        double count = calCount(firstFee, fromTime, toTime);
        int wholeBlocks = (int) count;
        double total = 0.0;
        for (int b = 1; b <= wholeBlocks; b++) {
            TimedItemFee fee = getFeeForBlock(fees, b);
            if (fee != null) {
                total += foreigner ? fee.getFfee() : fee.getFee();
            }
        }
        double remainder = count - wholeBlocks;
        if (remainder > 0) {
            TimedItemFee fee = getFeeForBlock(fees, wholeBlocks + 1);
            if (fee != null) {
                total += (foreigner ? fee.getFfee() : fee.getFee()) * remainder;
            }
        }
        return total;
    }

    public double calCount(TimedItemFee tif, Date admittedDate, Date dischargedDate) {

        // No fee configured at all counts the same as a fee with no duration set:
        // nothing to bill. RoomFacilityCharge.timedItemFee is a nullable mapping,
        // so this is reachable from the room paths, and a missing configuration
        // should not break the page that is rendering the bill.
        if (tif == null) {
            return 0;
        }

        // A one-time fee is charged once for the whole service, however long it
        // ran — no block counting, and no dependency on elapsed time at all.
        if (tif.isOneTime()) {
            return 1;
        }

        double duration = tif.getDurationMinutes();
        double overShoot = tif.getOverShootMinutes();
        double consumeTime = 0;

        if (dischargedDate == null) {
            dischargedDate = new Date();
        }

        consumeTime = CommonFunctions.calculateDurationMin(admittedDate, dischargedDate);
        if (consumeTime == 0) {
            return 0;
        }
        double count = 0;
        double calculation = 0;

        if (consumeTime != 0 && duration != 0) {
            if (tif.isBooleanValue()) {
                //For Minut Calculation (Theatre Charges)
                count = (consumeTime / duration);
            } else {
                //For Room Calculation Hour(For Room Charges)
                count = (long) (consumeTime / duration);
            }

            calculation = (consumeTime - (count * duration));
            if ((overShoot != 0 && overShoot <= calculation) || count == 0) {
                count++;
            }
        }

        return count;
    }
}
