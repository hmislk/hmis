package com.divudi.ejb;

import com.divudi.core.entity.PatientEncounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FinalBillLockBean} per-admission locking (#25552).
 */
public class FinalBillLockBeanTest {

    private FinalBillLockBean lockBean;

    @BeforeEach
    public void setUp() {
        lockBean = new FinalBillLockBean();
    }

    private PatientEncounter createEncounter(Long id) {
        PatientEncounter pe = new PatientEncounter();
        pe.setId(id);
        return pe;
    }

    @Test
    @DisplayName("Two different admissions execute under lock in parallel without blocking each other")
    public void testDifferentAdmissionsRunInParallel() throws Exception {
        PatientEncounter admissionA = createEncounter(101L);
        PatientEncounter admissionB = createEncounter(202L);

        CountDownLatch aStarted = new CountDownLatch(1);
        CountDownLatch bFinished = new CountDownLatch(1);
        CountDownLatch releaseA = new CountDownLatch(1);

        AtomicBoolean bCompletedWhileAHeld = new AtomicBoolean(false);

        Thread threadA = new Thread(() -> {
            lockBean.withFinalBillVersionLock(admissionA, () -> {
                aStarted.countDown();
                try {
                    releaseA.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                return null;
            });
        });

        Thread threadB = new Thread(() -> {
            try {
                assertTrue(aStarted.await(5, TimeUnit.SECONDS), "Thread A should have acquired lock");
            } catch (InterruptedException ignored) {
            }
            lockBean.withFinalBillVersionLock(admissionB, () -> {
                // If admission B is not blocked by admission A, it can enter and finish while A is still paused
                bCompletedWhileAHeld.set(true);
                bFinished.countDown();
                return null;
            });
        });

        threadA.start();
        threadB.start();

        try {
            assertTrue(bFinished.await(5, TimeUnit.SECONDS), "Admission B should finish without waiting for A to release");
            assertTrue(bCompletedWhileAHeld.get(), "Admission B must complete while Admission A is still holding its lock");
        } finally {
            releaseA.countDown();
            threadA.join(3000);
            threadB.join(3000);
        }
    }

    @Test
    @DisplayName("Two concurrent operations on the same admission mutually exclude each other")
    public void testSameAdmissionSerialises() throws Exception {
        PatientEncounter admission = createEncounter(303L);

        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        AtomicBoolean secondEnteredWhileFirstHeld = new AtomicBoolean(false);
        AtomicBoolean secondFinished = new AtomicBoolean(false);

        Thread thread1 = new Thread(() -> {
            lockBean.withFinalBillVersionLock(admission, () -> {
                firstEntered.countDown();
                try {
                    releaseFirst.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                return null;
            });
        });

        Thread thread2 = new Thread(() -> {
            try {
                firstEntered.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            lockBean.withFinalBillVersionLock(admission, () -> {
                if (releaseFirst.getCount() > 0) {
                    secondEnteredWhileFirstHeld.set(true);
                }
                secondFinished.set(true);
                return null;
            });
        });

        thread1.start();
        thread2.start();

        try {
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            // Give thread 2 time to attempt acquiring lock
            Thread.sleep(100);
            assertFalse(secondEnteredWhileFirstHeld.get(), "Second thread must NOT enter while first holds the lock");
            assertFalse(secondFinished.get(), "Second thread must be waiting");
        } finally {
            releaseFirst.countDown();
            thread1.join(3000);
            thread2.join(3000);
        }

        assertTrue(secondFinished.get(), "Second thread should finish after first releases");
    }

    @Test
    @DisplayName("withFinalBillVersionLockOutsideTransaction uses the same admission lock")
    public void testOutsideTransactionLockExcludes() throws Exception {
        PatientEncounter admission = createEncounter(404L);

        CountDownLatch insideEntered = new CountDownLatch(1);
        CountDownLatch releaseInside = new CountDownLatch(1);
        AtomicBoolean outsideEnteredPrematurely = new AtomicBoolean(false);
        AtomicInteger counter = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            lockBean.withFinalBillVersionLock(admission, () -> {
                counter.incrementAndGet();
                insideEntered.countDown();
                try {
                    releaseInside.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                return null;
            });
        });

        Thread t2 = new Thread(() -> {
            try {
                insideEntered.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            lockBean.withFinalBillVersionLockOutsideTransaction(admission, () -> {
                if (releaseInside.getCount() > 0) {
                    outsideEnteredPrematurely.set(true);
                }
                counter.incrementAndGet();
                return null;
            });
        });

        t1.start();
        t2.start();

        try {
            assertTrue(insideEntered.await(5, TimeUnit.SECONDS));
            Thread.sleep(100);
            assertFalse(outsideEnteredPrematurely.get());
            assertEquals(1, counter.get());
        } finally {
            releaseInside.countDown();
            t1.join(3000);
            t2.join(3000);
        }

        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("BillNumberGenerator delegates to FinalBillLockBean")
    public void testBillNumberGeneratorDelegatesToFinalBillLockBean() throws Exception {
        AtomicBoolean standardDelegated = new AtomicBoolean(false);
        AtomicBoolean outsideDelegated = new AtomicBoolean(false);
        PatientEncounter pe = createEncounter(505L);

        FinalBillLockBean recordingBean = new FinalBillLockBean() {
            @Override
            public <T> T withFinalBillVersionLock(PatientEncounter encounter, Supplier<T> action) {
                if (encounter == pe) {
                    standardDelegated.set(true);
                }
                return action.get();
            }

            @Override
            public <T> T withFinalBillVersionLockOutsideTransaction(PatientEncounter encounter, Supplier<T> action) {
                if (encounter == pe) {
                    outsideDelegated.set(true);
                }
                return action.get();
            }
        };

        BillNumberGenerator generator = new BillNumberGenerator();
        Field field = BillNumberGenerator.class.getDeclaredField("finalBillLockBean");
        field.setAccessible(true);
        field.set(generator, recordingBean);

        String result = generator.withFinalBillVersionLock(pe, () -> "delegated-ok");
        assertEquals("delegated-ok", result);
        assertTrue(standardDelegated.get(), "BillNumberGenerator must invoke FinalBillLockBean.withFinalBillVersionLock");

        String resultOutside = generator.withFinalBillVersionLockOutsideTransaction(pe, () -> "delegated-outside-ok");
        assertEquals("delegated-outside-ok", resultOutside);
        assertTrue(outsideDelegated.get(), "BillNumberGenerator must invoke FinalBillLockBean.withFinalBillVersionLockOutsideTransaction");
    }
    @Test
    @DisplayName("A nested call for the same admission on the same thread runs inline, inside the outer call")
    public void testNestedCallRunsInline() {
        PatientEncounter pe = createEncounter(303L);
        AtomicInteger order = new AtomicInteger();
        Integer result = lockBean.withFinalBillVersionLock(pe, () -> {
            order.compareAndSet(0, 1);
            Integer inner = lockBean.withFinalBillVersionLock(pe, () -> order.compareAndSet(1, 2) ? 7 : -1);
            order.compareAndSet(2, 3);
            return inner;
        });
        assertEquals(7, result);
        assertEquals(3, order.get());
    }
}
