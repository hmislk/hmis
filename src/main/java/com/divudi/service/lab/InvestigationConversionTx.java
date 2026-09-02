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

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Converts a single Investigation into a Service, each in its own transaction.
 *
 * <p>
 * Split out from {@link InvestigationConversionService} purely so the
 * {@code REQUIRES_NEW} boundary is real: a self-invocation inside one bean would
 * bypass the container interceptor and silently run in the caller's transaction.
 * </p>
 *
 * <p>
 * One transaction per row is what makes per-row partial success honest. Sharing a
 * single transaction across the batch cannot work: {@link ItemFacade} is itself a
 * {@code @Stateless} EJB, so a {@code PersistenceException} from one row would
 * mark the shared transaction rollback-only, and the rows counted as converted
 * before it would be rolled back anyway while still being reported as successes.
 * </p>
 */
@Stateless
public class InvestigationConversionTx {

    @EJB
    private InvestigationFacade investigationFacade;

    @EJB
    private ItemFacade itemFacade;

    /**
     * Rewrites one Investigation into a Service in a transaction of its own, so
     * that a failure here cannot roll back rows converted by earlier calls.
     *
     * <p>
     * The discriminator has to be changed with native SQL. {@code Item} uses
     * single-table inheritance and {@code DTYPE} is not a mapped attribute, so no
     * JPQL update can express this type transition - this is the documented
     * exception to the repository's "JPQL first, native SQL last" rule, not an
     * oversight.
     * </p>
     *
     * <p>
     * {@code investigationFacade.find(...)} leaves a managed {@code Investigation}
     * in the persistence context that the native update makes stale. That is
     * harmless here: the transaction-scoped persistence context is discarded when
     * this method returns, so the commit of this row is the synchronization
     * boundary. Callers that need the new state read it in a later transaction.
     * </p>
     *
     * @param investigationId id of the investigation to convert
     * @return {@code true} if a row was converted, {@code false} if no such
     *         investigation exists
     * @throws Exception if the update itself fails; this transaction is then
     *                   rolled back on its own
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean convertToService(Long investigationId) throws Exception {
        Investigation ix = investigationFacade.find(investigationId);
        if (ix == null) {
            return false;
        }
        // Table name resolved at runtime rather than hardcoded: EclipseLink's default
        // naming for an @Entity with no @Table annotation is uppercase ("ITEM"), which
        // only matches by luck on a case-insensitive MySQL. A literal "Item" fails with
        // "Table doesn't exist" on a case-sensitive deployment (Linux with the default
        // lower_case_table_names=0). getTableName() reads the name EclipseLink itself
        // resolved, so this works on both. Same fix already established for this exact
        // bug class elsewhere (AbstractFacade.getTableName(), closes #19648). Closes #23406.
        String table = itemFacade.getTableName();
        String sql = "UPDATE " + table + " SET DTYPE = ? WHERE id = ?";
        List<Object> params = Arrays.asList("Service", ix.getId());
        itemFacade.executeNativeSql(sql, params);
        return true;
    }
}
