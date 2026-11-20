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
import javax.transaction.TransactionSynchronizationRegistry;

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

    @Resource
    private TransactionSynchronizationRegistry transactionRegistry;

    private final ConcurrentHashMap<String, ReentrantLock> lockMap = new ConcurrentHashMap<>();

    /**
     * Runs {@code action} under a per-encounter lock. Two concurrent actions
     * for the same admission serialise on this lock, while actions for different
     * admissions execute in parallel.
     *
     * <p>A first call goes through {@link #lockAndRunInNewTransaction} ({@code NOT_SUPPORTED}):
     * it acquires the per-admission lock, runs the action within a new transaction through
     * {@link #executeInNewTransaction(Supplier)} on the bean's own EJB proxy, and commits
     * before releasing the lock in {@code finally}. A nested call on the same thread, while
     * the lock is held, runs the action directly in the outer call's transaction.
     * This prevents concurrent operations on the same admission from reading uncommitted
     * state or computing duplicate version serials (#25552).</p>
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public <T> T withFinalBillVersionLock(PatientEncounter pe, Supplier<T> action) {
        // Nested call (e.g. saveBill() inside settle()): this thread already holds the lock.
        // When a transaction is active, SUPPORTS has joined it, so run the action there;
        // another REQUIRES_NEW would commit part of the save on its own. With no
        // transaction, keep the old behaviour: one new transaction for the action.
        ReentrantLock held = lockMap.get("finalBillVersionSerial-" + pe.getId());
        if (held != null && held.isHeldByCurrentThread()) {
            if (isTransactionActive()) {
                return action.get();
            }
            if (sessionContext != null) {
                return sessionContext.getBusinessObject(FinalBillLockBean.class).executeInNewTransaction(action);
            }
            return executeInNewTransaction(action);
        }
        if (sessionContext != null) {
            return sessionContext.getBusinessObject(FinalBillLockBean.class).lockAndRunInNewTransaction(pe, action);
        }
        return lockAndRunInNewTransaction(pe, action);
    }

    /**
     * Takes the per-admission lock with no transaction, runs the action in a new
     * transaction and commits it before the lock is released. Call through
     * {@link #withFinalBillVersionLock}.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public <T> T lockAndRunInNewTransaction(PatientEncounter pe, Supplier<T> action) {
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

    private boolean isTransactionActive() {
        return transactionRegistry != null && transactionRegistry.getTransactionKey() != null;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public <T> T executeInNewTransaction(Supplier<T> action) {
        return action.get();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
