/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.core.data.dto.timeditem;

import java.util.List;

/**
 * The complete slot list for a timed item, replacing whatever is configured now
 * ({@code PUT /timed-items/{id}/fees}).
 *
 * <p>Configuring an N-slot tiered service one POST at a time takes N round trips, each
 * re-saving the parent's total, and lets the slot-order uniqueness rule see only one row
 * at a time. Submitting the whole set makes the configuration atomic and lets the rule be
 * checked across the complete list (issue #23236 §4).
 *
 * @author Buddhika
 */
public class TimedItemFeeBulkRequestDTO {

    private List<TimedItemFeeUpsertDTO> fees;

    public TimedItemFeeBulkRequestDTO() {
    }

    /**
     * An empty list is valid — it retires every slot. A missing list is not: that is
     * far more likely to be a malformed body than a deliberate "remove everything".
     */
    public boolean isValid() {
        return fees != null;
    }

    public List<TimedItemFeeUpsertDTO> getFees() {
        return fees;
    }

    public void setFees(List<TimedItemFeeUpsertDTO> fees) {
        this.fees = fees;
    }
}
