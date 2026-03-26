package com.divudi.service.pharmacy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Allocates blocks of IDs from the EclipseLink sequence table in an independent
 * transaction (REQUIRES_NEW) so the row lock is acquired and released immediately,
 * avoiding lock-wait conflicts with the caller's own JPA sequence allocation.
 */
@Stateless
public class SequenceAllocatorService {

    private static final Logger LOGGER = Logger.getLogger(SequenceAllocatorService.class.getName());

    private static final int SEQ_ALLOC_SIZE = 50;
    private static final String SEQ_NAME = "SEQ_GEN";

    /** Cached actual sequence table name resolved from INFORMATION_SCHEMA. */
    private volatile String resolvedSeqTable = null;

    @PersistenceContext(unitName = "hmisPU")
    private EntityManager em;

    /**
     * Allocate {@code count} unique IDs from the EclipseLink SEQ_GEN sequence.
     * Runs in its own committed transaction so the sequence row lock is released
     * before the caller's bulk INSERT statements execute.
     *
     * @param count number of IDs required
     * @return array of {@code count} unique IDs safe to use in native SQL INSERTs
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public long[] allocate(int count) {
        String tbl = seqTable();
        List<Long> ids = new ArrayList<>(count);

        while (ids.size() < count) {
            em.createNativeQuery(
                    "UPDATE " + tbl + " SET SEQ_COUNT = SEQ_COUNT + ? WHERE SEQ_NAME = ?")
                    .setParameter(1, SEQ_ALLOC_SIZE)
                    .setParameter(2, SEQ_NAME)
                    .executeUpdate();

            Object result = em.createNativeQuery(
                    "SELECT SEQ_COUNT FROM " + tbl + " WHERE SEQ_NAME = ?")
                    .setParameter(1, SEQ_NAME)
                    .getSingleResult();

            long newCount = result instanceof Number ? ((Number) result).longValue()
                    : Long.parseLong(result.toString());

            long blockStart = newCount - SEQ_ALLOC_SIZE;
            for (int i = 0; i < SEQ_ALLOC_SIZE && ids.size() < count; i++) {
                ids.add(blockStart + i);
            }
        }

        long[] out = new long[count];
        for (int i = 0; i < count; i++) {
            out[i] = ids.get(i);
        }
        return out;
    }

    private String seqTable() {
        if (resolvedSeqTable == null) {
            Object name = em.createNativeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                    + "WHERE TABLE_SCHEMA = DATABASE() AND UPPER(TABLE_NAME) = 'SEQUENCE' LIMIT 1")
                    .getSingleResult();
            resolvedSeqTable = name.toString();
            System.out.println("[SequenceAllocator] Resolved sequence table: " + resolvedSeqTable);
        }
        return resolvedSeqTable;
    }
}
