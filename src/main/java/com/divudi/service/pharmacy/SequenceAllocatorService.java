package com.divudi.service.pharmacy;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.eclipse.persistence.jpa.JpaEntityManager;

/**
 * Allocates blocks of IDs using EclipseLink's internal sequence mechanism.
 *
 * EclipseLink's getNextSequenceNumberValue() uses a dedicated non-JTA connection
 * for sequence allocation and keeps its internal ID cache in sync. This avoids both:
 *  - Lock-wait conflicts (no JTA UPDATE on the sequence row)
 *  - Duplicate key violations (EclipseLink's cache always reflects what we used)
 */
@Stateless
public class SequenceAllocatorService {

    private static final Logger LOGGER = Logger.getLogger(SequenceAllocatorService.class.getName());

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Allocate {@code count} unique IDs for the given entity class using
     * EclipseLink's built-in sequencing (non-JTA, cache-aware).
     *
     * @param count       number of IDs required
     * @param entityClass the JPA entity class whose sequence to use
     * @return array of {@code count} unique IDs safe for use in native SQL INSERTs
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long[] allocate(int count, Class<?> entityClass) {
        JpaEntityManager jpaEM = em.unwrap(JpaEntityManager.class);
        long[] ids = new long[count];
        for (int i = 0; i < count; i++) {
            Object val = jpaEM.getActiveSession().getNextSequenceNumberValue(entityClass);
            ids[i] = val instanceof Number ? ((Number) val).longValue()
                    : Long.parseLong(val.toString());
        }
        return ids;
    }
}
