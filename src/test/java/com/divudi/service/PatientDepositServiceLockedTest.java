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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26265: the deposit record is checked and lowered on a copy loaded
 * under a lock.
 */
class PatientDepositServiceLockedTest {

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
    void staleCopyIsNotUsedTheLockedCopyIsLowered() throws Exception {
        PatientDeposit stale = deposit(500.0);
        locked = deposit(100.0);
        service().lowerLocked(bill(80.0), stale, 0.0);
        assertEquals(1, lockCalls);
        assertEquals(20.0, locked.getBalance());
        assertEquals(500.0, stale.getBalance());
        assertSame(locked, edited.get(0));
        assertEquals(1, history.size());
        assertEquals(100.0, history.get(0)[0]);
        assertEquals(20.0, history.get(0)[1]);
    }

    @Test
    void staleCopyThatLooksRichDoesNotLetAnOverdrawThrough() throws Exception {
        PatientDeposit stale = deposit(500.0);
        locked = deposit(30.0);
        PatientDepositService s = service();
        PatientDepositException e = assertThrows(PatientDepositException.class,
                () -> s.lowerLocked(bill(80.0), stale, 0.0));
        assertEquals("No Sufficient Patient Deposit", e.getMessage());
        assertEquals(30.0, locked.getBalance());
        assertTrue(edited.isEmpty());
        assertTrue(history.isEmpty());
    }

    @Test
    void creditLimitCountsAsCover() throws Exception {
        locked = deposit(30.0);
        service().lowerLocked(bill(80.0), deposit(30.0), 50.0);
        assertEquals(-50.0, locked.getBalance());
    }

    @Test
    void exactBalanceIsAllowed() throws Exception {
        locked = deposit(80.0);
        service().lowerLocked(bill(80.0), deposit(80.0), 0.0);
        assertEquals(0.0, locked.getBalance());
    }

    @Test
    void missingLockedCopyFallsBackToTheFoundOne() throws Exception {
        locked = null;
        PatientDeposit found = deposit(100.0);
        service().lowerLocked(bill(40.0), found, 0.0);
        assertEquals(60.0, found.getBalance());
    }
}
