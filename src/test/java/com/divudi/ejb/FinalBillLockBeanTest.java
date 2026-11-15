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
                    releaseA.await(5, TimeUnit.SECONDS);
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

        assertTrue(bFinished.await(5, TimeUnit.SECONDS), "Admission B should finish without waiting for A to release");
        assertTrue(bCompletedWhileAHeld.get(), "Admission B must complete while Admission A is still holding its lock");

        releaseA.countDown();
        threadA.join(2000);
        threadB.join(2000);
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

        assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
        // Give thread 2 time to attempt acquiring lock
        Thread.sleep(100);
        assertFalse(secondEnteredWhileFirstHeld.get(), "Second thread must NOT enter while first holds the lock");
        assertFalse(secondFinished.get(), "Second thread must be waiting");

        releaseFirst.countDown();
        thread1.join(2000);
        thread2.join(2000);

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
                    releaseInside.await(5, TimeUnit.SECONDS);
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

        assertTrue(insideEntered.await(5, TimeUnit.SECONDS));
        Thread.sleep(100);
        assertFalse(outsideEnteredPrematurely.get());
        assertEquals(1, counter.get());

        releaseInside.countDown();
        t1.join(2000);
        t2.join(2000);

        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("BillNumberGenerator delegates to FinalBillLockBean")
    public void testBillNumberGeneratorDelegatesToFinalBillLockBean() throws Exception {
        BillNumberGenerator generator = new BillNumberGenerator();
        Field field = BillNumberGenerator.class.getDeclaredField("finalBillLockBean");
        field.setAccessible(true);
        field.set(generator, lockBean);

        PatientEncounter pe = createEncounter(505L);
        String result = generator.withFinalBillVersionLock(pe, () -> "delegated-ok");
        assertEquals("delegated-ok", result);

        String resultOutside = generator.withFinalBillVersionLockOutsideTransaction(pe, () -> "delegated-outside-ok");
        assertEquals("delegated-outside-ok", resultOutside);
    }
}
