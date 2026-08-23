/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import com.divudi.core.data.inward.TimedItemDurationUnit;
import com.divudi.core.entity.inward.TimedItemFee;

import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.List;

/**
 * The single definition of the rules a {@link TimedItemFee} must satisfy, shared by
 * the fee page ({@code TimedItemFeeController}) and the REST API
 * ({@code TimedItemApiService}) so an identical payload is accepted or rejected
 * identically whichever surface it arrives on (issue #23236 §3).
 *
 * <p>These are not cosmetic checks. {@code InwardBeanController.getAllTimedItemFees}
 * orders fees by {@code sortOrder} and {@code getFeeForBlock} then picks
 * {@code fees.get(blockNumber - 1)}, so a duplicate or zero slot order makes the
 * ordering non-deterministic and a tiered service can silently charge the wrong tier.
 * A zero-length block is just as bad in the other direction: it bills nothing for
 * every stay.
 *
 * @author Buddhika
 */
@Stateless
public class TimedItemFeeRules implements Serializable {

    /**
     * The slot order to give a fee that was saved without one.
     *
     * <p>Deliberately {@code max + 1} rather than {@code size + 1}: with slots 1 and 3
     * already taken, {@code size + 1} yields 3 and collides with an existing row, so
     * the auto-assignment itself would be rejected by
     * {@link #validateSlotOrder(int, List, Long)}. The next free slot above the
     * highest one in use always fits.
     */
    public int nextSlotOrder(List<TimedItemFee> existingFees) {
        int max = 0;
        if (existingFees != null) {
            for (TimedItemFee f : existingFees) {
                if (f.getSortOrder() > max) {
                    max = f.getSortOrder();
                }
            }
        }
        return max + 1;
    }

    /**
     * Slot order must be a real position (1 or greater) and must not collide with
     * another fee on the same service.
     *
     * @param editingFeeId id of the fee being edited, so a row does not clash with
     *                     itself; null when the fee is new
     */
    public void validateSlotOrder(int sortOrder, List<TimedItemFee> existingFees, Long editingFeeId)
            throws TimedItemFeeRuleException {
        if (sortOrder < 1) {
            throw new TimedItemFeeRuleException("Slot Order must be 1 or greater.");
        }
        if (existingFees == null) {
            return;
        }
        for (TimedItemFee f : existingFees) {
            if (f.isRetired()) {
                continue;
            }
            if (editingFeeId != null && f.getId() != null && editingFeeId.equals(f.getId())) {
                continue;
            }
            if (f.getSortOrder() == sortOrder) {
                throw new TimedItemFeeRuleException("Slot Order must be unique per service.");
            }
        }
    }

    /**
     * A time-based fee needs a block longer than zero — a zero-length block bills
     * nothing for every stay. A ONE_TIME fee is not time-based, so it is exempt.
     */
    public void validateDuration(TimedItemDurationUnit unit, double durationHours)
            throws TimedItemFeeRuleException {
        TimedItemDurationUnit effective = unit == null ? TimedItemDurationUnit.HOUR : unit;
        if (effective == TimedItemDurationUnit.ONE_TIME) {
            return;
        }
        if (durationHours <= 0) {
            throw new TimedItemFeeRuleException(
                    "Duration must be greater than 0 for a " + effective.getLabel() + " fee.");
        }
    }

    /**
     * Both rules for one fee, resolving an unset slot order first. Returns the slot
     * order that should be stored.
     */
    public int applyToFee(TimedItemFee fee, List<TimedItemFee> existingFees)
            throws TimedItemFeeRuleException {
        validateDuration(fee.getDurationUnit(), fee.getDurationHours());
        int sortOrder = fee.getSortOrder() == 0 ? nextSlotOrder(existingFees) : fee.getSortOrder();
        validateSlotOrder(sortOrder, existingFees, fee.getId());
        return sortOrder;
    }
}
