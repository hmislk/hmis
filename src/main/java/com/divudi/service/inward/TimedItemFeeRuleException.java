/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.inward;

import javax.ejb.ApplicationException;

/**
 * Raised when a timed item fee breaks one of the rules in {@link TimedItemFeeRules}.
 *
 * <p>The message is written for the person configuring the fee, not for a log: the
 * fee page shows it verbatim through {@code JsfUtil.addErrorMessage}, and the REST
 * API returns it as the body of a 400. That is the point — one rule, one wording,
 * whichever surface the payload arrived on.
 *
 * <p>Rolls back the transaction: the REST services set fields before a rule can
 * fail, so without the rollback a refused request would still commit those
 * changes (issue #25992).
 *
 * @author Buddhika
 */
@ApplicationException(rollback = true)
public class TimedItemFeeRuleException extends Exception {

    public TimedItemFeeRuleException(String message) {
        super(message);
    }
}
