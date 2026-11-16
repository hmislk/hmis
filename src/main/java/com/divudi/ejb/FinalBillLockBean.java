package com.divudi.ejb;

import com.divudi.core.entity.PatientEncounter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

/**
 * Dedicated lock manager for per-admission final bill operations.
 *
 * Runs under ConcurrencyManagementType.BEAN so the container does not acquire
 * a singleton write lock during method execution. This isolates the per-admission
 * ReentrantLock map and lock helpers from the bill-number generators in
 * {@link BillNumberGenerator}, ensuring that holding a lock on one admission
 * does not block unrelated bill-number generation or concurrent payments on
 * other admissions (#25552).
 *
 * @author Dr. M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class FinalBillLockBean {

    @Resource
    private SessionContext sessionContext;

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * Runs {@code action} under a per-encounter lock. Two concurrent actions
     * for the same admission serialise on this lock, while actions for different
     * admissions execute in parallel.
     *
     * <p>The public method runs with {@code NOT_SUPPORTED} so it does not join an
     * existing container transaction. It acquires the per-admission lock, runs the
     * action within a new transaction through {@link #executeInNewTransaction(Supplier)}
     * on the bean's own EJB proxy, and commits before releasing the lock in {@code finally}.
     * This prevents concurrent operations on the same admission from reading uncommitted
     * state or computing duplicate version serials (#25552).</p>
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <T> T withFinalBillVersionLock(PatientEncounter pe, Supplier<T> action) {
        String lockKey = "finalBillVersionSerial-" + pe.getId();
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (sessionContext != null) {
                return sessionContext.getBusinessObject(FinalBillLockBean.class).executeInNewTransaction(action);
            } else {
                return executeInNewTransaction(action);
            }
        } finally {
            lock.unlock();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <T> T executeInNewTransaction(Supplier<T> action) {
        return action.get();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void withFinalBillVersionLock(PatientEncounter pe, Runnable action) {
        withFinalBillVersionLock(pe, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Same lock as {@link #withFinalBillVersionLock}, but runs the action outside
     * a container transaction, so each facade call inside it commits on its own.
     * Use it from non-transactional callers (e.g. REST resources) where identity
     * generation requires independent transactions.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <T> T withFinalBillVersionLockOutsideTransaction(PatientEncounter pe, Supplier<T> action) {
        String lockKey = "finalBillVersionSerial-" + pe.getId();
        ReentrantLock lock = lockMap.computeIfAbsent(lockKey, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void withFinalBillVersionLockOutsideTransaction(PatientEncounter pe, Runnable action) {
        withFinalBillVersionLockOutsideTransaction(pe, () -> {
            action.run();
            return null;
        });
    }
}
