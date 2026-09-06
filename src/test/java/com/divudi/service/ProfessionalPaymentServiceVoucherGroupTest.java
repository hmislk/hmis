package com.divudi.service;

import com.divudi.core.data.ProfessionalPaymentVoucherGroup;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.BillItemFacade;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProfessionalPaymentServiceVoucherGroupTest {

    private static class DummyBillItemFacade extends BillItemFacade {
        String jpql;
        Map<String, Object> params;
        List<BillItem> result = new ArrayList<>();

        @Override
        public List<BillItem> findByJpql(String jpql, Map<String, Object> parameters) {
            this.jpql = jpql;
            this.params = parameters;
            return result;
        }
    }

    private ProfessionalPaymentService serviceWith(DummyBillItemFacade facade) throws Exception {
        ProfessionalPaymentService service = new ProfessionalPaymentService();
        Field f = ProfessionalPaymentService.class.getDeclaredField("billItemFacade");
        f.setAccessible(true);
        f.set(service, facade);
        return service;
    }

    private Patient patient(long id, String phn, String name) {
        Patient p = new Patient();
        p.setId(id);
        p.setPhn(phn);
        Person person = new Person();
        person.setName(name);
        p.setPerson(person);
        return p;
    }

    private BillItem paymentItem(Bill sourceBill, double netValue) {
        BillFee sourceFee = new BillFee();
        sourceFee.setBill(sourceBill);
        BillItem paymentBillItem = new BillItem();
        paymentBillItem.setPaidForBillFee(sourceFee);
        paymentBillItem.setNetValue(netValue);
        return paymentBillItem;
    }

    @Test
    public void nullBillReturnsEmptyList() throws Exception {
        ProfessionalPaymentService service = serviceWith(new DummyBillItemFacade());
        List<ProfessionalPaymentVoucherGroup> groups
                = service.groupPaymentBillItemsByPatientOrBht(null);
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    public void groupsOpdItemsByPatientAndSumsSubtotals() throws Exception {
        DummyBillItemFacade facade = new DummyBillItemFacade();

        Patient patientA = patient(1L, "MRN1", "Patient A");
        Patient patientB = patient(2L, "MRN2", "Patient B");

        Bill sourceBillA1 = new Bill();
        sourceBillA1.setPatient(patientA);
        Bill sourceBillA2 = new Bill();
        sourceBillA2.setPatient(patientA);
        Bill sourceBillB = new Bill();
        sourceBillB.setPatient(patientB);

        facade.result.add(paymentItem(sourceBillA1, 100.0));
        facade.result.add(paymentItem(sourceBillB, 250.0));
        facade.result.add(paymentItem(sourceBillA2, 50.0));

        Bill paymentBill = new Bill();
        paymentBill.setId(99L);

        ProfessionalPaymentService service = serviceWith(facade);
        List<ProfessionalPaymentVoucherGroup> groups
                = service.groupPaymentBillItemsByPatientOrBht(paymentBill);

        assertTrue(facade.jpql.contains("bi.bill=:b"));
        assertTrue(facade.jpql.contains("bi.retired"));
        assertEquals(paymentBill, facade.params.get("b"));

        assertEquals(2, groups.size());
        // insertion order preserved: patient A first
        assertEquals(patientA, groups.get(0).getPatient());
        assertEquals(2, groups.get(0).getBillItems().size());
        assertEquals(150.0, groups.get(0).getSubtotal(), 0.001);
        assertEquals(patientB, groups.get(1).getPatient());
        assertEquals(250.0, groups.get(1).getSubtotal(), 0.001);
    }

    @Test
    public void groupsInwardItemsByBhtAndShowsBhtIdentifier() throws Exception {
        DummyBillItemFacade facade = new DummyBillItemFacade();

        Patient patientA = patient(1L, "MRN1", "Patient A");
        PatientEncounter encounter = new PatientEncounter();
        encounter.setId(7L);
        encounter.setBhtNo("BHT/123");
        encounter.setPatient(patientA);

        Bill sourceBill1 = new Bill();
        sourceBill1.setPatientEncounter(encounter);
        Bill sourceBill2 = new Bill();
        sourceBill2.setPatientEncounter(encounter);

        facade.result.add(paymentItem(sourceBill1, 300.0));
        facade.result.add(paymentItem(sourceBill2, 200.0));

        Bill paymentBill = new Bill();
        paymentBill.setId(99L);

        ProfessionalPaymentService service = serviceWith(facade);
        List<ProfessionalPaymentVoucherGroup> groups
                = service.groupPaymentBillItemsByPatientOrBht(paymentBill);

        assertEquals(1, groups.size());
        assertEquals(encounter, groups.get(0).getPatientEncounter());
        assertEquals(500.0, groups.get(0).getSubtotal(), 0.001);
        assertTrue(groups.get(0).getDisplayIdentifier().contains("BHT/123"));
    }

    @Test
    public void itemsWithoutPatientGoToMiscellaneousGroup() throws Exception {
        DummyBillItemFacade facade = new DummyBillItemFacade();

        Bill sourceBillNoPatient = new Bill(); // miscellaneous staff fee
        facade.result.add(paymentItem(sourceBillNoPatient, 75.0));

        Bill paymentBill = new Bill();
        paymentBill.setId(99L);

        ProfessionalPaymentService service = serviceWith(facade);
        List<ProfessionalPaymentVoucherGroup> groups
                = service.groupPaymentBillItemsByPatientOrBht(paymentBill);

        assertEquals(1, groups.size());
        assertNull(groups.get(0).getPatient());
        assertNull(groups.get(0).getPatientEncounter());
        assertEquals("Miscellaneous", groups.get(0).getDisplayName());
        assertEquals(75.0, groups.get(0).getSubtotal(), 0.001);
    }
}
