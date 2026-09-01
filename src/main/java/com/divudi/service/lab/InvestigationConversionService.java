/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.lab;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Converts Investigations into Services.
 *
 * <p>
 * The work lives here, in a {@code @Stateless} EJB, rather than in the JSF
 * controller. Holding a JTA transaction across a session-scoped controller action
 * is what let a rolled-back transaction stay bound to an HTTP worker thread during
 * the 2026-09-01 Ruhunu outage; container-managed transactions on a stateless bean
 * begin and end inside a single call, so nothing can outlive the invocation.
 * </p>
 */
@Stateless
public class InvestigationConversionService {

    private static final Logger LOGGER = Logger.getLogger(InvestigationConversionService.class.getName());

    @EJB
    private InvestigationConversionTx investigationConversionTx;

    /**
     * Outcome of a conversion run, so the caller can build its own user messages
     * without needing a transaction to still be open.
     */
    public static class ConversionResult {

        private final int successCount;
        private final int skippedCount;
        private final int failureCount;

        public ConversionResult(int successCount, int skippedCount, int failureCount) {
            this.successCount = successCount;
            this.skippedCount = skippedCount;
            this.failureCount = failureCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        /** Ids that no longer resolve to an investigation, so nothing was converted. */
        public int getSkippedCount() {
            return skippedCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        /** True when nothing failed outright; skipped ids do not count as failures. */
        public boolean isCompletelySuccessful() {
            return failureCount == 0;
        }

        public boolean hasSkipped() {
            return skippedCount > 0;
        }
    }

    /**
     * Converts each identified Investigation into a Service.
     *
     * <p>
     * Deliberately {@code NOT_SUPPORTED}: each row is converted by
     * {@link InvestigationConversionTx} in a transaction of its own, and this
     * method must have no transaction of its own for that to mean anything. If it
     * ran transactionally, a failure propagating out of the per-row EJB call would
     * be a system exception and would mark <em>this</em> transaction rollback-only
     * too, so rows already reported as converted would be rolled back at the end
     * of the batch while still being counted as successes.
     * </p>
     *
     * <p>
     * Individual failures are counted rather than aborting the run, matching the
     * behaviour this had while it lived in the controller. Ids that no longer
     * resolve are reported separately as skipped, so a batch of entirely stale ids
     * is not silently reported as a clean success.
     * </p>
     *
     * @param investigationIds ids of the investigations to convert; may be null or empty
     * @return counts of converted, skipped and failed rows, never null
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public ConversionResult convertInvestigationsToServices(List<Long> investigationIds) {
        if (investigationIds == null || investigationIds.isEmpty()) {
            return new ConversionResult(0, 0, 0);
        }

        int successCount = 0;
        int skippedCount = 0;
        int failureCount = 0;

        for (Long investigationId : investigationIds) {
            try {
                if (investigationConversionTx.convertToService(investigationId)) {
                    successCount++;
                } else {
                    skippedCount++;
                    LOGGER.log(Level.WARNING, "Skipped conversion: no investigation with id {0}", investigationId);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to convert investigation " + investigationId + " to a service", e);
                failureCount++;
            }
        }

        return new ConversionResult(successCount, skippedCount, failureCount);
    }
}
