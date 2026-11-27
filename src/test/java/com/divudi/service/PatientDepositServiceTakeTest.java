package com.divudi.service;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.data.HistoryType;
import com.divudi.core.entity.Bill;
import com.divudi.core.facade.PatientDepositFacade;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hardening, tracked under #26265.
 */
class PatientDepositServiceTakeTest {

    private PatientDeposit locked;
    private final List<PatientDeposit> edited = new ArrayList<>();
    private final List<Double[]> history = new ArrayList<>();
    private int lockCalls;

    private PatientDepositService service() throws Exception {
        PatientDepositService s = new PatientDepositService() {
            @Override
            public void createPatientDepositHitory(HistoryType ht, PatientDeposit pd, Bill b,
                    Double beforeBalance, Double afterBalance, Double transactionValue) {
                history.add(new Double[]{beforeBalance, afterBalance, transactionValue});
            }
        };
        set(s, "patientDepositFacade", new PatientDepositFacade() {
            @Override
            public PatientDeposit findWithLock(Object id) {
                lockCalls++;
                return locked;
            }

            @Override
            public void edit(PatientDeposit pd) {
                edited.add(pd);
            }
        });
        return s;
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field f = PatientDepositService.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private PatientDeposit deposit(double balance) {
        PatientDeposit pd = new PatientDeposit();
        pd.setId(5L);
        pd.setBalance(balance);
        return pd;
    }

    private BilledBill bill(double total) {
        BilledBill b = new BilledBill();
        b.setNetTotal(total);
        return b;
    }

    @Test
    void lockedCopyIsLowered() throws Exception {
        PatientDeposit other = deposit(500.0);
        locked = deposit(100.0);
        PatientDepositTake take = service().takeLocked(other, 80.0, 0.0);
        assertEquals(1, lockCalls);
        assertEquals(20.0, locked.getBalance());
        assertEquals(500.0, other.getBalance());
        assertSame(locked, edited.get(0));
        assertEquals(100.0, take.getBalanceBefore());
        assertEquals(20.0, take.getBalanceAfter());
        assertEquals(80.0, take.getAmount());
        assertTrue(history.isEmpty());
    }

    @Test
    void refusesWhenBalanceTooLowAndSavesNothing() throws Exception {
        PatientDeposit other = deposit(500.0);
        locked = deposit(30.0);
        PatientDepositService s = service();
        PatientDepositException e = assertThrows(PatientDepositException.class,
                () -> s.takeLocked(other, 80.0, 0.0));
        assertEquals("No Sufficient Patient Deposit", e.getMessage());
        assertEquals(30.0, locked.getBalance());
        assertTrue(edited.isEmpty());
        assertTrue(history.isEmpty());
    }

    @Test
    void creditLimitCountsAsCover() throws Exception {
        locked = deposit(30.0);
        service().takeLocked(deposit(30.0), 80.0, 50.0);
        assertEquals(-50.0, locked.getBalance());
    }

    @Test
    void exactBalanceIsAllowed() throws Exception {
        locked = deposit(80.0);
        service().takeLocked(deposit(80.0), 80.0, 0.0);
        assertEquals(0.0, locked.getBalance());
    }

    @Test
    void smallRoundingDifferenceIsAllowed() throws Exception {
        locked = deposit(79.995);
        service().takeLocked(deposit(79.995), 80.0, 0.0);
        assertEquals(79.995 - 80.0, locked.getBalance(), 1e-9);
    }

    @Test
    void differenceBeyondToleranceIsRefused() throws Exception {
        locked = deposit(79.98);
        PatientDepositService s = service();
        assertThrows(PatientDepositException.class, () -> s.takeLocked(deposit(79.98), 80.0, 0.0));
        assertEquals(79.98, locked.getBalance());
    }

    @Test
    void missingLockedCopyFallsBackToTheFoundOne() throws Exception {
        locked = null;
        PatientDeposit found = deposit(100.0);
        service().takeLocked(found, 40.0, 0.0);
        assertEquals(60.0, found.getBalance());
    }

    @Test
    void historyRowIsWrittenWithTheBillAfterTheTake() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        BilledBill b = bill(80.0);
        s.writeTakeHistory(take, b);
        assertEquals(1, history.size());
        assertEquals(100.0, history.get(0)[0]);
        assertEquals(20.0, history.get(0)[1]);
        assertEquals(-80.0, history.get(0)[2]);
        assertSame(b, take.getHistoryBill());
    }

    @Test
    void laterFailureGivesTheDepositBackWithoutHistoryWhenNoRowWasWritten() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        s.giveBackLocked(take);
        assertEquals(100.0, locked.getBalance());
        assertTrue(history.isEmpty());
        assertTrue(take.isGivenBack());
    }

    @Test
    void laterFailureGivesTheDepositBackWithHistoryWhenARowWasWritten() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        s.writeTakeHistory(take, bill(80.0));
        s.giveBackLocked(take);
        assertEquals(100.0, locked.getBalance());
        assertEquals(2, history.size());
        assertEquals(20.0, history.get(1)[0]);
        assertEquals(100.0, history.get(1)[1]);
        assertEquals(80.0, history.get(1)[2]);
    }

    @Test
    void giveBackRunsOnlyOnce() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        s.giveBackLocked(take);
        s.giveBackLocked(take);
        assertEquals(100.0, locked.getBalance());
    }

    @Test
    void billTotalWithinToleranceChangesNothing() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        int editsBefore = edited.size();
        s.reconcileTake(take, 80.005);
        assertEquals(editsBefore, edited.size());
        assertEquals(20.0, locked.getBalance());
    }

    @Test
    void smallerBillTotalGivesTheDifferenceBack() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        s.reconcileTake(take, 50.0);
        assertEquals(50.0, locked.getBalance());
        assertEquals(50.0, take.getAmount());
        assertEquals(50.0, take.getBalanceAfter());
    }

    @Test
    void largerBillTotalIsRefusedWhenNotCovered() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = s.takeLocked(deposit(100.0), 80.0, 0.0);
        assertThrows(PatientDepositException.class, () -> s.reconcileTake(take, 150.0));
        assertEquals(20.0, locked.getBalance());
        assertFalse(take.isGivenBack());
    }
}
