package com.divudi.service;

import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.CancelledBill;
import com.divudi.core.entity.Patient;
import com.divudi.core.facade.PatientFacade;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #26260: a single Patient Deposit settle lowers Patient.runningBalance
 * by the bill total. Its cancel gives back the same amount.
 */
class PatientDepositServiceCancelTest {

    private final List<Patient> saved = new ArrayList<>();
    private Patient stored;

    private PatientDepositService service() throws Exception {
        PatientDepositService s = new PatientDepositService();
        Field f = PatientDepositService.class.getDeclaredField("patientFacade");
        f.setAccessible(true);
        f.set(s, new PatientFacade() {
            @Override
            public Patient findWithoutCache(Object id) {
                return stored;
            }

            @Override
            public void edit(Patient p) {
                saved.add(p);
            }
        });
        return s;
    }

    private CancelledBill cancelOf(double total) {
        stored = new Patient();
        stored.setId(9L);
        CancelledBill cb = new CancelledBill();
        cb.setPatient(stored);
        cb.setNetTotal(total);
        return cb;
    }

    @Test
    void cancelGivesBackTheAmountTheSettleTook() throws Exception {
        CancelledBill cb = cancelOf(-300.0);
        stored.setRunningBalance(700.0);
        service().restoreRunningBalanceOnCancel(cb);
        assertEquals(1000.0, stored.getRunningBalance());
        assertEquals(1, saved.size());
    }

    @Test
    void nullBalanceCountsAsZero() throws Exception {
        CancelledBill cb = cancelOf(300.0);
        stored.setRunningBalance(null);
        service().restoreRunningBalanceOnCancel(cb);
        assertEquals(300.0, stored.getRunningBalance());
    }

    @Test
    void billWithoutPatientIsIgnored() throws Exception {
        PatientDepositService s = service();
        BilledBill b = new BilledBill();
        b.setNetTotal(100.0);
        s.restoreRunningBalanceOnCancel(b);
        assertTrue(saved.isEmpty());
    }
}
