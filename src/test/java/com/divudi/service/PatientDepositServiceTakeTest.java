package com.divudi.service;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.PatientDeposit;
import com.divudi.core.data.HistoryType;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientDepositHistory;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.PatientDepositHistoryFacade;
import com.divudi.core.facade.PatientDepositFacade;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    private final List<PatientDepositHistory> rows = new ArrayList<>();
    private final List<PatientDepositHistory> editedRows = new ArrayList<>();
    private int lockCalls;

    private PatientDepositService service() throws Exception {
        PatientDepositService s = new PatientDepositService() {
            @Override
            PatientDepositHistory writeHistoryRow(HistoryType ht, PatientDeposit pd, Bill b, WebUser creater,
                    Department department, Institution institution,
                    Double beforeBalance, Double afterBalance, Double transactionValue) {
                history.add(new Double[]{beforeBalance, afterBalance, transactionValue});
                PatientDepositHistory row = new PatientDepositHistory();
                row.setBill(b);
                row.setTransactionValue(transactionValue);
                row.setBalanceAfterTransaction(afterBalance);
                rows.add(row);
                return row;
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
        set(s, "patientDepositHistoryFacade", new PatientDepositHistoryFacade() {
            @Override
            public void edit(PatientDepositHistory h) {
                editedRows.add(h);
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

    private PatientDepositTake take(PatientDepositService s, PatientDeposit found, double amount, double credit) {
        return s.takeLocked(found, amount, credit, null, null, null);
    }

    @Test
    void takeWritesOneRowWithoutBill() throws Exception {
        PatientDeposit other = deposit(300.0);
        locked = deposit(100.0);
        PatientDepositTake take = take(service(), other, 80.0, 0.0);
        assertEquals(1, lockCalls);
        assertEquals(20.0, locked.getBalance());
        assertEquals(300.0, other.getBalance());
        assertSame(locked, edited.get(0));
        assertEquals(100.0, take.getBalanceBefore());
        assertEquals(20.0, take.getBalanceAfter());
        assertEquals(80.0, take.getAmount());
        assertEquals(1, history.size());
        assertEquals(-80.0, history.get(0)[2]);
        assertNull(rows.get(0).getBill());
        assertSame(rows.get(0), take.getHistoryRow());
    }

    @Test
    void refusesWhenBalanceTooLowAndSavesNothing() throws Exception {
        PatientDeposit other = deposit(300.0);
        locked = deposit(30.0);
        PatientDepositService s = service();
        PatientDepositException e = assertThrows(PatientDepositException.class,
                () -> take(s, other, 80.0, 0.0));
        assertEquals("No Sufficient Patient Deposit", e.getMessage());
        assertEquals(30.0, locked.getBalance());
        assertTrue(edited.isEmpty());
        assertTrue(history.isEmpty());
    }

    @Test
    void creditLimitCountsAsCover() throws Exception {
        locked = deposit(30.0);
        take(service(), deposit(30.0), 80.0, 50.0);
        assertEquals(-50.0, locked.getBalance());
    }

    @Test
    void exactBalanceIsAllowed() throws Exception {
        locked = deposit(80.0);
        take(service(), deposit(80.0), 80.0, 0.0);
        assertEquals(0.0, locked.getBalance());
    }

    @Test
    void smallRoundingDifferenceIsAllowed() throws Exception {
        locked = deposit(79.995);
        take(service(), deposit(79.995), 80.0, 0.0);
        assertEquals(79.995 - 80.0, locked.getBalance(), 1e-9);
    }

    @Test
    void differenceBeyondToleranceIsRefused() throws Exception {
        locked = deposit(79.98);
        PatientDepositService s = service();
        assertThrows(PatientDepositException.class, () -> take(s, deposit(79.98), 80.0, 0.0));
        assertEquals(79.98, locked.getBalance());
    }

    @Test
    void fallbackWhenRecordMissing() throws Exception {
        locked = null;
        PatientDeposit found = deposit(100.0);
        take(service(), found, 40.0, 0.0);
        assertEquals(60.0, found.getBalance());
    }

    @Test
    void linkSetsTheBillOnTheExistingRow() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        BilledBill b = bill(80.0);
        s.linkTakeHistory(take, b);
        assertEquals(1, history.size());
        assertSame(b, rows.get(0).getBill());
        assertSame(rows.get(0), editedRows.get(0));
        assertSame(b, take.getHistoryBill());
    }

    @Test
    void giveBackRestoresTheBalanceAndWritesARow() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        s.giveBackLocked(take);
        assertEquals(100.0, locked.getBalance());
        assertEquals(2, history.size());
        assertEquals(20.0, history.get(1)[0]);
        assertEquals(100.0, history.get(1)[1]);
        assertEquals(80.0, history.get(1)[2]);
        assertTrue(take.isGivenBack());
    }

    @Test
    void giveBackIsSkippedWhenMarked() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        assertTrue(take.canGiveBack());
        take.markBillSaved();
        assertFalse(take.canGiveBack());
        int editsBefore = edited.size();
        s.giveBackLocked(take);
        assertEquals(editsBefore, edited.size());
        assertEquals(20.0, locked.getBalance());
        assertEquals(1, history.size());
        assertFalse(take.isGivenBack());
    }

    @Test
    void giveBackRunsOnlyOnce() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        s.giveBackLocked(take);
        s.giveBackLocked(take);
        assertEquals(100.0, locked.getBalance());
        assertEquals(2, history.size());
    }

    @Test
    void billTotalWithinToleranceChangesNothing() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        int editsBefore = edited.size();
        assertEquals(0.0, s.reconcileTake(take, 80.005));
        assertEquals(editsBefore, edited.size());
        assertEquals(20.0, locked.getBalance());
    }

    @Test
    void smallerBillTotalGivesTheDifferenceBackAndUpdatesTheRow() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        assertEquals(0.0, s.reconcileTake(take, 50.0));
        assertEquals(50.0, locked.getBalance());
        assertEquals(50.0, take.getAmount());
        assertEquals(50.0, take.getBalanceAfter());
        assertEquals(-50.0, rows.get(0).getTransactionValue());
        assertEquals(50.0, rows.get(0).getBalanceAfterTransaction());
        assertSame(rows.get(0), editedRows.get(0));
    }

    @Test
    void largerBillTotalIsTakenInFullWhenCovered() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        assertEquals(0.0, s.reconcileTake(take, 95.0));
        assertEquals(5.0, locked.getBalance());
        assertEquals(95.0, take.getAmount());
    }

    @Test
    void largerBillTotalReportsTheShortfall() throws Exception {
        locked = deposit(100.0);
        PatientDepositService s = service();
        PatientDepositTake take = take(s, deposit(100.0), 80.0, 0.0);
        double shortfall = s.reconcileTake(take, 150.0);
        assertEquals(50.0, shortfall, 1e-9);
        assertEquals(0.0, locked.getBalance(), 1e-9);
        assertFalse(take.isGivenBack());
    }
}
