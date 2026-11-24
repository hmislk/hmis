package com.divudi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.InstitutionBillEncounter;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.InstitutionFacade;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Figures behind the inpatient debtor balance and opd-and-inward due reports (#25601). */
class InwardCreditFiguresTest {

    private static final double D = 0.0001;

    private final Map<Long, Institution> companies = new HashMap<>();
    /** final bill id -> company id -> {committed, paid} */
    private final Map<Long, Map<Long, double[]>> commitments = new HashMap<>();
    private boolean failQueries;
    private final Map<Long, Double> patientPaid = new HashMap<>();
    /** "finalBillId:companyId" of commitments whose receipt used PaymentMethod.Card */
    private final List<Object[]> cardReceipts = new ArrayList<>();

    private final BillFacade billFacade = new BillFacade() {
        @Override
        public List<Object[]> findAggregates(String jpql, Map<String, Object> parameters) {
            List<Object[]> out = new ArrayList<>();
            if (failQueries) {
                return null;
            }
            if (jpql.contains("paidBill.paymentMethod")) {
                return cardReceipts;
            }
            if (jpql.contains("InwardPaymentBill") || parameters.containsKey("btp")) {
                for (Object id : (List<?>) parameters.get("ids")) {
                    if (patientPaid.containsKey((Long) id)) {
                        out.add(new Object[]{id, patientPaid.get((Long) id)});
                    }
                }
                return out;
            }
            boolean perCompany = jpql.contains("cc.id");
            for (Object id : (List<?>) parameters.get("ids")) {
                Map<Long, double[]> byCompany = commitments.get((Long) id);
                if (byCompany == null || byCompany.isEmpty()) { // grouped SQL returns no row for a bill with no commitments
                    continue;
                }
                double c = 0;
                double p = 0;
                for (Map.Entry<Long, double[]> e : byCompany.entrySet()) {
                    if (perCompany) {
                        out.add(new Object[]{id, e.getKey(), e.getValue()[0], e.getValue()[1]});
                    }
                    c += e.getValue()[0];
                    p += e.getValue()[1];
                }
                if (!perCompany) {
                    out.add(new Object[]{id, c, p});
                }
            }
            return out;
        }
    };

    private final InstitutionFacade institutionFacade = new InstitutionFacade() {
        @Override
        public Institution find(Object id) {
            return companies.get((Long) id);
        }
    };

    private final InwardCreditFigures figures = new InwardCreditFigures(billFacade, institutionFacade);

    private Institution company(long id, String name) {
        Institution i = new Institution();
        i.setId(id);
        i.setName(name);
        companies.put(id, i);
        return i;
    }

    private PatientEncounter admission(long id, double net, double paidByPatient, double[]... companyFigures) {
        PatientEncounter pe = new PatientEncounter();
        pe.setId(id);
        pe.setBhtNo("BHT" + id);
        Bill fb = new Bill();
        fb.setId(id + 1000);
        fb.setNetTotal(net);
        pe.setFinalBill(fb);
        patientPaid.put(id, paidByPatient);
        Map<Long, double[]> byCompany = new HashMap<>();
        for (double[] f : companyFigures) {
            byCompany.put((long) f[0], new double[]{f[1], f[2]});
        }
        commitments.put(fb.getId(), byCompany);
        return pe;
    }

    private List<InstitutionBillEncounter> rows(List<PatientEncounter> pes, String type, String by, Institution filter) {
        return figures.buildReportRows(pes, type, by, filter, null);
    }

    @Test
    void twoCompaniesGetOneRowEachAndPatientFiguresAreCountedOnce() {
        company(1, "A");
        company(2, "B");
        PatientEncounter pe = admission(10, 1000, 200, new double[]{1, 400, 100}, new double[]{2, 300, 300});
        List<InstitutionBillEncounter> r = rows(Arrays.asList(pe), "any", "any", null);
        assertEquals(2, r.size());
        double patientDue = 0;
        double companyDue = 0;
        double paidByPatient = 0;
        for (InstitutionBillEncounter x : r) {
            patientDue += x.getPatientDue();
            companyDue += x.getCompanyDue();
            assertEquals(1000, x.getNetTotal(), D);
            paidByPatient += x.getPaidByPatient();
            assertEquals(400, x.getTotalPaidByCompanies(), D);
            assertEquals(400, x.getTotalDue(), D);
            assertEquals(300, x.getPatientGopAmount(), D);
        }
        assertEquals(100, patientDue, D);
        assertEquals(200, paidByPatient, D);
        assertEquals(300, companyDue, D);
    }

    @Test
    void dueFilterKeepsOnlyRowsWithSomethingDue() {
        company(1, "A");
        company(2, "B");
        PatientEncounter pe = admission(10, 1000, 300, new double[]{1, 400, 100}, new double[]{2, 300, 300});
        List<InstitutionBillEncounter> r = rows(Arrays.asList(pe), "due", "any", null);
        assertEquals(1, r.size());
        assertEquals("A", r.get(0).getInstitution().getName());
        assertEquals(300, r.get(0).getCompanyDue(), D);
        assertEquals(0, r.get(0).getPatientDue(), D);
        assertEquals(100, r.get(0).getPaidByCompany(), D);
        assertEquals(400, r.get(0).getGopAmount(), D);
    }

    @Test
    void overpaidCompanyShowsNegativeExcessAndNoDue() {
        company(1, "A");
        PatientEncounter pe = admission(11, 1000, 600, new double[]{1, 400, 500});
        List<InstitutionBillEncounter> r = rows(Arrays.asList(pe), "excess", "any", null);
        assertEquals(1, r.size());
        assertEquals(0, r.get(0).getCompanyDue(), D);
        assertEquals(-100, r.get(0).getCompanyExcess(), D);
        assertEquals(0, r.get(0).getPatientDue(), D);
        assertEquals(0, r.get(0).getPatientExcess(), D);
    }

    @Test
    void patientOverpaymentIsNegativePatientExcessOnFirstRow() {
        company(1, "A");
        PatientEncounter pe = admission(12, 1000, 700, new double[]{1, 400, 400});
        InstitutionBillEncounter row = rows(Arrays.asList(pe), "any", "any", null).get(0);
        assertEquals(-100, row.getPatientExcess(), D);
        assertEquals(0, row.getPatientDue(), D);
    }

    @Test
    void settledAllNeedsPatientAndCompanyToBeSquare() {
        company(1, "A");
        PatientEncounter square = admission(13, 1000, 600, new double[]{1, 400, 400});
        PatientEncounter patientOwes = admission(14, 1000, 500, new double[]{1, 400, 400});
        List<InstitutionBillEncounter> r = rows(Arrays.asList(square, patientOwes), "settled", "all", null);
        assertEquals(1, r.size());
        assertEquals("BHT13", r.get(0).getBhtNo());
    }

    @Test
    void companyFilterKeepsOnlyThatCompanyAndPatientStaysOnItsRow() {
        company(1, "A");
        Institution b = company(2, "B");
        PatientEncounter pe = admission(15, 1000, 200, new double[]{1, 400, 100}, new double[]{2, 300, 0});
        List<InstitutionBillEncounter> r = rows(Arrays.asList(pe), "any", "any", b);
        assertEquals(1, r.size());
        assertEquals(300, r.get(0).getCompanyDue(), D);
        assertEquals(100, r.get(0).getPatientDue(), D);
    }

    @Test
    void receiptMethodFilterKeepsCompaniesWithAReceiptByThatMethod() {
        company(1, "A");
        company(2, "B");
        PatientEncounter pe = admission(16, 1000, 200, new double[]{1, 400, 100}, new double[]{2, 300, 300});
        cardReceipts.add(new Object[]{pe.getFinalBill().getId(), 2L});
        List<InstitutionBillEncounter> r = figures.buildReportRows(Arrays.asList(pe), "any", "any", null, PaymentMethod.Card);
        assertEquals(1, r.size());
        assertEquals("B", r.get(0).getInstitution().getName());
    }

    @Test
    void admissionWithNoCommitmentBillsAndNoCreditCompanyHasNoRows() {
        PatientEncounter pe = admission(17, 500, 500);
        assertTrue(rows(Arrays.asList(pe), "any", "any", null).isEmpty());
    }

    @Test
    void creditAdmissionWithoutCommitmentBillsUsesLegacyFigures() {
        Institution a = company(1, "A");
        PatientEncounter pe = admission(18, 1000, 100);
        pe.setPaymentMethod(PaymentMethod.Credit);
        pe.setCreditCompany(a);
        pe.setCreditUsedAmount(900);
        pe.getFinalBill().setSettledAmountBySponsor(300);
        InstitutionBillEncounter row = rows(Arrays.asList(pe), "any", "any", null).get(0);
        assertEquals(900, row.getGopAmount(), D);
        assertEquals(300, row.getPaidByCompany(), D);
        assertEquals(600, row.getCompanyDue(), D);
        assertEquals(0, row.getPatientDue(), D);
    }

    @Test
    void balancesAreRoundedToCents() {
        company(1, "A");
        PatientEncounter pe = admission(19, 1000.004, 600, new double[]{1, 400, 400});
        InstitutionBillEncounter row = rows(Arrays.asList(pe), "any", "any", null).get(0);
        assertEquals(0, row.getPatientDue(), 0.0);
        assertEquals(0, row.getPatientExcess(), 0.0);
    }

    @Test
    void patientPaidIsShownOnTheFirstListedRowOnly() {
        company(1, "A");
        company(2, "B");
        PatientEncounter pe = admission(20, 1000, 200, new double[]{1, 400, 100}, new double[]{2, 300, 300});
        List<InstitutionBillEncounter> r = rows(Arrays.asList(pe), "any", "any", null);
        assertEquals(2, r.size());
        assertEquals(200, r.get(0).getPaidByPatient() + r.get(1).getPaidByPatient(), D);
        assertEquals(200, r.get(0).getPaidByPatient(), D);
        assertEquals(0, r.get(1).getPaidByPatient(), D);
    }

    private List<String> dueListing(long idOfDueCompany, long idOfSettledCompany) {
        company(idOfDueCompany, "Due");
        company(idOfSettledCompany, "Settled");
        PatientEncounter pe = admission(30 + idOfDueCompany, 1000, 200,
                new double[]{idOfDueCompany, 400, 100}, new double[]{idOfSettledCompany, 300, 300});
        List<String> names = new ArrayList<>();
        for (InstitutionBillEncounter x : rows(Arrays.asList(pe), "due", "any", null)) {
            names.add(x.getInstitution().getName());
        }
        java.util.Collections.sort(names);
        return names;
    }

    @Test
    void listedRowsDoNotDependOnCompanyOrder() {
        assertEquals(dueListing(1, 2), dueListing(2, 1));
    }

    @Test
    void settledCompanyIsNotListedWhenNothingIsDueToThePatient() {
        company(1, "A");
        company(2, "B");
        PatientEncounter first = admission(40, 1000, 300, new double[]{1, 400, 100}, new double[]{2, 300, 300});
        PatientEncounter reversed = admission(41, 1000, 300, new double[]{2, 400, 100}, new double[]{1, 300, 300});
        assertEquals(1, rows(Arrays.asList(first), "due", "any", null).size());
        assertEquals(1, rows(Arrays.asList(reversed), "due", "any", null).size());
    }

    @Test
    void failedQueryGivesNoRowsInsteadOfLegacyFigures() {
        Institution a = company(1, "A");
        company(2, "B");
        PatientEncounter pe = admission(50, 1000, 0, new double[]{1, 400, 0}, new double[]{2, 600, 0});
        pe.setPaymentMethod(PaymentMethod.Credit);
        pe.setCreditCompany(a);
        pe.setCreditUsedAmount(400);
        failQueries = true;
        assertTrue(rows(Arrays.asList(pe), "any", "any", null).isEmpty());
        failQueries = false;
        assertEquals(2, rows(Arrays.asList(pe), "any", "any", null).size());
    }
}
