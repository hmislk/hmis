/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.service.lab;

import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.ItemFacade;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;

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
    private InvestigationFacade investigationFacade;

    @EJB
    private ItemFacade itemFacade;

    /**
     * Outcome of a conversion run, so the caller can build its own user messages
     * without needing the transaction to still be open.
     */
    public static class ConversionResult {

        private final int successCount;
        private final int failureCount;

        public ConversionResult(int successCount, int failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public boolean isCompletelySuccessful() {
            return failureCount == 0;
        }
    }

    /**
     * Converts each identified Investigation into a Service by rewriting its
     * discriminator column.
     *
     * <p>
     * Individual failures are counted rather than aborting the run, matching the
     * behaviour this had while it lived in the controller.
     * </p>
     *
     * @param investigationIds ids of the investigations to convert; may be null or empty
     * @return counts of converted and failed rows, never null
     */
    public ConversionResult convertInvestigationsToServices(List<Long> investigationIds) {
        if (investigationIds == null || investigationIds.isEmpty()) {
            return new ConversionResult(0, 0);
        }

        int successCount = 0;
        int failureCount = 0;

        for (Long investigationId : investigationIds) {
            try {
                Investigation ix = investigationFacade.find(investigationId);
                if (ix == null) {
                    continue;
                }
                String sql = "UPDATE Item SET DTYPE = ? WHERE id = ?";
                List<Object> params = Arrays.asList("Service", ix.getId());
                itemFacade.executeNativeSql(sql, params);
                successCount++;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to convert investigation " + investigationId + " to a service", e);
                failureCount++;
            }
        }

        if (failureCount == 0) {
            itemFacade.flush();
        }

        return new ConversionResult(successCount, failureCount);
    }
}
