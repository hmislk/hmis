# Individual Per-Patient/BHT Professional Payment Print — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Print Individual" button to all 4 professional-payment settle pages that prints one voucher per OPD patient / BHT, page-break separated, plus a final summary page (issue hmislk/hmis#17489).

**Architecture:** A shared grouping method in `ProfessionalPaymentService` turns a settled payment `Bill`'s `BillItem`s into per-patient/BHT groups (via `paidForBillFee.bill.patientEncounter` / `.patient`). Three new reusable JSF composites (A4 / 5x5 / POS) loop over the groups and render full mini-vouchers with `page-break-after: always`, ending with a grand-total summary page. Each settle page gets a lazy controller getter, a new button with `p:printer`, and a preview panel gated by that page's existing paper-format config keys.

**Tech Stack:** Java EE (EJB `@Stateless` service, JSF/PrimeFaces composites), JPQL via `AbstractFacade.findByJpql(String, Map)`, JUnit 5 with the dummy-facade-via-reflection test pattern (see `src/test/java/com/divudi/service/BillServiceLabSummaryTest.java`).

**Spec:** `developer_docs/specs/2026-07-11-individual-professional-payment-print-design.md`

## Global Constraints

- Branch: `17489-batch-professional-fee-vouchers` (already created from `origin/development`).
- **NEVER stage `src/main/resources/META-INF/persistence.xml`** — it holds local JNDI (`jdbc/coop` / `jdbc/ruhunuAudit`) and must stay unstaged. HEAD already has the CI placeholders.
- JPQL only, no native SQL (project rule #9).
- Additions only: do NOT modify any existing constructor, method signature, button, or composite (project rule #8).
- No `ui:fragment` — use `h:panelGroup` (project JSF convention). Plain `div`s are fine inside composite implementations.
- Every new actionable button gets a stable `id`.
- **Sign convention (correction discovered during investigation, supersedes the spec's wording):** payment-bill *items* store positive `netValue` (fee values), while the payment *bill* totals are negative. So per-group subtotals are displayed **as-is** (no negation); grand totals on the summary page are negated (`-bill.total`, `-bill.tax`, `-bill.netTotal`) exactly like the existing vouchers.
- Commit after each task with conventional-commit messages ending in `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`.

---

### Task 1: Grouping model + service method (TDD)

**Files:**
- Create: `src/main/java/com/divudi/core/data/ProfessionalPaymentVoucherGroup.java`
- Modify: `src/main/java/com/divudi/service/ProfessionalPaymentService.java` (add one `@EJB` field + two methods; touch nothing else)
- Test: `src/test/java/com/divudi/service/ProfessionalPaymentServiceVoucherGroupTest.java`

**Interfaces:**
- Consumes: `BillItemFacade.findByJpql(String, Map<String,Object>)` (exists in `AbstractFacade`), `BillItem.getPaidForBillFee()/.getReferanceBillItem()/.getReferenceBill()/.getNetValue()`, `PatientEncounter.getBhtNo()`, `Patient.getPhn()`, `Person.getNameWithTitle()`.
- Produces: `List<ProfessionalPaymentVoucherGroup> ProfessionalPaymentService.groupPaymentBillItemsByPatientOrBht(Bill paymentBill)`; group getters `getPatient()`, `getPatientEncounter()`, `getBillItems()`, `getSubtotal()`, `getDisplayName()`, `getDisplayIdentifier()`.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/divudi/service/ProfessionalPaymentServiceVoucherGroupTest.java`:

```java
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
```

Note: if `Patient.setPhn(String)` or `Person.setName(String)` differ, check the entity and adjust the test helpers — do NOT change the entities.

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q test -Dtest=ProfessionalPaymentServiceVoucherGroupTest 2>&1 | tail -20`
Expected: COMPILATION ERROR — `ProfessionalPaymentVoucherGroup` does not exist / method not found.

- [ ] **Step 3: Create the group class**

Create `src/main/java/com/divudi/core/data/ProfessionalPaymentVoucherGroup.java`:

```java
package com.divudi.core.data;

import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One per-patient (OPD) or per-BHT (inward) block of a professional payment
 * bill, used to print individual payment vouchers separated by page breaks.
 */
public class ProfessionalPaymentVoucherGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private Patient patient;
    private PatientEncounter patientEncounter;
    private List<BillItem> billItems = new ArrayList<>();
    private double subtotal;

    public void addBillItem(BillItem billItem) {
        if (billItem == null) {
            return;
        }
        billItems.add(billItem);
        subtotal += billItem.getNetValue();
    }

    public String getDisplayName() {
        if (patientEncounter != null
                && patientEncounter.getPatient() != null
                && patientEncounter.getPatient().getPerson() != null) {
            return patientEncounter.getPatient().getPerson().getNameWithTitle();
        }
        if (patient != null && patient.getPerson() != null) {
            return patient.getPerson().getNameWithTitle();
        }
        return "Miscellaneous";
    }

    public String getDisplayIdentifier() {
        if (patientEncounter != null) {
            return "BHT : " + (patientEncounter.getBhtNo() == null ? "" : patientEncounter.getBhtNo());
        }
        if (patient != null) {
            return "MRN : " + (patient.getPhn() == null ? "" : patient.getPhn());
        }
        return "";
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
```

- [ ] **Step 4: Add the service method**

In `src/main/java/com/divudi/service/ProfessionalPaymentService.java`:

Add imports (keep existing ones):

```java
import com.divudi.core.data.ProfessionalPaymentVoucherGroup;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillItemFacade;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
```

Add the facade field below the existing `@EJB BillFacade billFacade;`:

```java
    @EJB
    BillItemFacade billItemFacade;
```

Add the two methods at the end of the class (before the closing brace):

```java
    /**
     * Groups a settled professional payment bill's items into one group per
     * BHT (inward) or per patient (OPD), preserving insertion order. Items
     * whose source bill has neither a patient encounter nor a patient (e.g.
     * miscellaneous staff fees) fall into a single "Miscellaneous" group.
     */
    public List<ProfessionalPaymentVoucherGroup> groupPaymentBillItemsByPatientOrBht(Bill paymentBill) {
        List<ProfessionalPaymentVoucherGroup> groups = new ArrayList<>();
        if (paymentBill == null) {
            return groups;
        }
        String jpql = "select bi from BillItem bi "
                + " where bi.retired=:ret "
                + " and bi.bill=:b "
                + " order by bi.id";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("b", paymentBill);
        List<BillItem> paymentBillItems = billItemFacade.findByJpql(jpql, params);
        if (paymentBillItems == null) {
            return groups;
        }
        Map<String, ProfessionalPaymentVoucherGroup> groupsByKey = new LinkedHashMap<>();
        for (BillItem paymentBillItem : paymentBillItems) {
            Bill sourceBill = resolveSourceBillOfPaymentBillItem(paymentBillItem);
            PatientEncounter encounter = sourceBill == null ? null : sourceBill.getPatientEncounter();
            Patient patient = null;
            String key;
            if (encounter != null) {
                key = "bht:" + encounter.getId();
            } else {
                patient = sourceBill == null ? null : sourceBill.getPatient();
                key = patient == null ? "misc" : "pt:" + patient.getId();
            }
            ProfessionalPaymentVoucherGroup group = groupsByKey.get(key);
            if (group == null) {
                group = new ProfessionalPaymentVoucherGroup();
                group.setPatientEncounter(encounter);
                group.setPatient(encounter != null ? encounter.getPatient() : patient);
                groupsByKey.put(key, group);
            }
            group.addBillItem(paymentBillItem);
        }
        groups.addAll(groupsByKey.values());
        return groups;
    }

    private Bill resolveSourceBillOfPaymentBillItem(BillItem paymentBillItem) {
        if (paymentBillItem.getPaidForBillFee() != null
                && paymentBillItem.getPaidForBillFee().getBill() != null) {
            return paymentBillItem.getPaidForBillFee().getBill();
        }
        if (paymentBillItem.getReferanceBillItem() != null
                && paymentBillItem.getReferanceBillItem().getBill() != null) {
            return paymentBillItem.getReferanceBillItem().getBill();
        }
        return paymentBillItem.getReferenceBill();
    }
```

(`Bill`, `BillItem` are already imported by the existing service.)

- [ ] **Step 5: Run test to verify it passes**

Run: `mvn -q test -Dtest=ProfessionalPaymentServiceVoucherGroupTest 2>&1 | tail -20`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/core/data/ProfessionalPaymentVoucherGroup.java \
        src/main/java/com/divudi/service/ProfessionalPaymentService.java \
        src/test/java/com/divudi/service/ProfessionalPaymentServiceVoucherGroupTest.java
git commit -m "feat(professional-payments): add per-patient/BHT voucher grouping service

- ProfessionalPaymentVoucherGroup data class with display helpers
- groupPaymentBillItemsByPatientOrBht in ProfessionalPaymentService
- JUnit tests: OPD patient grouping, BHT grouping, miscellaneous fallback

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Controller getters (all 4 controllers)

**Files:**
- Modify: `src/main/java/com/divudi/bean/common/StaffPaymentBillController.java`
- Modify: `src/main/java/com/divudi/bean/inward/InwardStaffPaymentBillController.java`
- Modify: `src/main/java/com/divudi/bean/inward/InwardSurgeryPaymentBillController.java`
- Modify: `src/main/java/com/divudi/bean/channel/ChannelStaffPaymentBillController.java`

**Interfaces:**
- Consumes: `ProfessionalPaymentService.groupPaymentBillItemsByPatientOrBht(Bill)` from Task 1; each controller's existing `current` field (type `Bill`).
- Produces: EL property `#{<controller>.individualVoucherGroups}` returning `List<ProfessionalPaymentVoucherGroup>` — used by the composites in Task 3.

- [ ] **Step 1: Add the identical block to each of the 4 controllers**

In EACH controller add imports (skip any already present):

```java
import com.divudi.core.data.ProfessionalPaymentVoucherGroup;
import com.divudi.service.ProfessionalPaymentService;
```

(`java.util.List` and `javax.ejb.EJB` are already imported in all four.)

Next to the other `@EJB` fields add:

```java
    @EJB
    private ProfessionalPaymentService professionalPaymentService;
```

Note: `InwardStaffPaymentBillController` may already inject `ProfessionalPaymentService` — check first (`grep -n "ProfessionalPaymentService" <file>`); if present, reuse the existing field name.

Then add fields + getter (place near the other getters). The cache is keyed on the
`current` bill reference, so it refreshes automatically after each settle and
needs no changes to existing methods:

```java
    private List<ProfessionalPaymentVoucherGroup> individualVoucherGroups;
    private Bill individualVoucherGroupsBill;

    public List<ProfessionalPaymentVoucherGroup> getIndividualVoucherGroups() {
        if (individualVoucherGroups == null || individualVoucherGroupsBill != current) {
            individualVoucherGroups = professionalPaymentService
                    .groupPaymentBillItemsByPatientOrBht(current);
            individualVoucherGroupsBill = current;
        }
        return individualVoucherGroups;
    }
```

Caution: in `ChannelStaffPaymentBillController` confirm the current-bill field is
named `current` (`grep -n "private Bill current" <file>`); if it is named
differently, use that field.

- [ ] **Step 2: Compile**

Run: `mvn -q compile 2>&1 | tail -5`
Expected: BUILD SUCCESS (no output on -q success, or only warnings).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/common/StaffPaymentBillController.java \
        src/main/java/com/divudi/bean/inward/InwardStaffPaymentBillController.java \
        src/main/java/com/divudi/bean/inward/InwardSurgeryPaymentBillController.java \
        src/main/java/com/divudi/bean/channel/ChannelStaffPaymentBillController.java
git commit -m "feat(professional-payments): expose individual voucher groups on payment controllers

- Lazy getIndividualVoucherGroups() on OPD, inward, surgery, channel controllers
- Cache keyed on current bill reference, refreshes per settle

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: The three "individual vouchers" composites

**Files:**
- Create: `src/main/webapp/resources/bill/staff_payment_individual_a4.xhtml`
- Create: `src/main/webapp/resources/bill/staff_payment_individual_five_five.xhtml`
- Create: `src/main/webapp/resources/bill/staff_payment_individual_pos.xhtml`

**Interfaces:**
- Consumes: `#{cc.attrs.controller.individualVoucherGroups}` (Task 2) with per-group `displayName`, `displayIdentifier`, `billItems`, `subtotal`; `#{cc.attrs.bill}` for header/totals. Fee-line EL uses null-safe chains: `bip.paidForBillFee.bill.deptId` (source bill no), `bip.referanceBillItem.item.name` (service), `bip.paidForBillFee.fee.feeType` (fee type), `bip.netValue` (value).
- Produces: composites invoked as `<bi:staff_payment_individual_a4 controller="..." bill="..."/>` etc. (namespace `bi` = `http://xmlns.jcp.org/jsf/composite/bill`, already declared on all 4 pages).

- [ ] **Step 1: Create the A4 composite**

Create `src/main/webapp/resources/bill/staff_payment_individual_a4.xhtml` (full file):

```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:p="http://primefaces.org/ui">

    <!-- INTERFACE -->
    <cc:interface>
        <cc:attribute name="controller" required="true" />
        <cc:attribute name="bill" type="com.divudi.core.entity.Bill" required="true" />
    </cc:interface>

    <!-- IMPLEMENTATION -->
    <cc:implementation>
        <h:outputStylesheet library="css" name="inwardpayments.css" />
        <h:panelGroup id="gpIndividualVouchersA4" layout="block">

            <!-- One mini-voucher per patient / BHT -->
            <ui:repeat value="#{cc.attrs.controller.individualVoucherGroups}" var="vg">
                <div class="a4bill1" style="page-break-after: always; margin: 0 auto; padding: 0; max-width: 210mm">

                    <div class="institutionName">
                        <h:outputLabel value="#{cc.attrs.bill.department.printingName}" />
                    </div>
                    <div class="institutionContact">
                        <div><h:outputLabel value="#{cc.attrs.bill.department.address}" /></div>
                        <div>
                            <h:outputLabel value="Phone : #{cc.attrs.bill.department.telephone1}" />
                        </div>
                    </div>

                    <div class="billline"><hr/></div>

                    <div style="text-align: center; font-weight: bold; font-size: 13pt">
                        <h:outputLabel value="PAYMENT VOUCHER" />
                    </div>

                    <div class="billline"><hr/></div>

                    <div class="billDetailsFiveFive">
                        <table style="width: 95%!important;" align="center">
                            <tr>
                                <td><h:outputLabel value="Bill ID" class="billDetailsFiveFive" /></td>
                                <td style="text-align: center;">:</td>
                                <td><h:outputLabel value="#{cc.attrs.bill.deptId}" class="billDetailsFiveFive" /></td>
                                <td width="2%" />
                                <td><h:outputLabel value="Payment Type" class="billDetailsFiveFive" /></td>
                                <td style="text-align: center;">:</td>
                                <td><h:outputLabel value="#{cc.attrs.bill.paymentMethod}" class="billDetailsFiveFive" /></td>
                            </tr>
                            <tr>
                                <td><h:outputLabel value="Date" class="billDetailsFiveFive" /></td>
                                <td style="text-align: center;">:</td>
                                <td>
                                    <h:outputLabel value="#{cc.attrs.bill.billDate}" class="billDetailsFiveFive">
                                        <f:convertDateTime pattern="#{sessionController.applicationPreference.longDateFormat}" />
                                    </h:outputLabel>
                                </td>
                                <td width="2%" />
                                <td><h:outputLabel value="Time" class="billDetailsFiveFive" /></td>
                                <td style="text-align: center;">:</td>
                                <td>
                                    <h:outputLabel value="#{cc.attrs.bill.billTime}" class="billDetailsFiveFive">
                                        <f:convertDateTime timeZone="Asia/Colombo" pattern="#{sessionController.applicationPreference.longTimeFormat}" />
                                    </h:outputLabel>
                                </td>
                            </tr>
                            <tr>
                                <td><h:outputLabel value="Doctor Name" class="billDetailsFiveFive" /></td>
                                <td style="text-align: center;">:</td>
                                <td>
                                    <h:outputLabel value="#{cc.attrs.bill.staff.person.nameWithTitle}" style="text-transform: capitalize;" class="billDetailsFiveFive" />
                                </td>
                                <td width="2%" />
                                <td><h:outputLabel value="Speciality" class="billDetailsFiveFive" /></td>
                                <td style="text-align: center;">:</td>
                                <td><h:outputLabel value="#{cc.attrs.bill.staff.speciality.name}" class="billDetailsFiveFive" /></td>
                            </tr>
                        </table>
                    </div>

                    <div class="billline"><hr/></div>

                    <!-- Patient / BHT identity -->
                    <div style="text-align: center; font-weight: bold; font-size: 12pt; margin: 6px 0;">
                        <h:outputLabel value="#{vg.displayName}" />
                        <h:outputLabel value=" — #{vg.displayIdentifier}" rendered="#{not empty vg.displayIdentifier}" />
                    </div>

                    <div class="billline"><hr/></div>

                    <!-- Fee lines of this patient / BHT -->
                    <div class="billDetailsFiveFive">
                        <h:dataTable value="#{vg.billItems}" var="bip"
                                     style="margin: auto; padding: 1%; width: 95%;">
                            <h:column>
                                <f:facet name="header"><h:outputLabel value="Bill No" /></f:facet>
                                <h:outputLabel value="#{bip.paidForBillFee.bill.deptId}" style="font-size: 10pt!important;" />
                            </h:column>
                            <h:column>
                                <f:facet name="header"><h:outputLabel value="Investigation/Service" /></f:facet>
                                <h:outputLabel value="#{bip.referanceBillItem.item.name}" style="font-size: 10pt!important;" />
                            </h:column>
                            <h:column>
                                <f:facet name="header"><h:outputLabel value="Fee Type" /></f:facet>
                                <h:outputLabel value="#{bip.paidForBillFee.fee.feeType}" style="font-size: 10pt!important;" />
                            </h:column>
                            <h:column>
                                <f:facet name="header"><h:outputLabel value="Value" /></f:facet>
                                <h:outputLabel value="#{bip.netValue}" style="font-size: 10pt!important; text-align: right!important;">
                                    <f:convertNumber pattern="#,##0.00" />
                                </h:outputLabel>
                            </h:column>
                        </h:dataTable>
                    </div>

                    <div class="billline"><hr/></div>

                    <!-- Patient subtotal (item values are stored positive) -->
                    <div>
                        <table style="width: 100%;">
                            <tr>
                                <td style="text-align: left; width: 60%; font-size: 16px!important; font-weight: bold;">
                                    &nbsp;&nbsp;<h:outputLabel value="Total for #{vg.displayName}" />
                                </td>
                                <td style="text-align: right!important; width: 40%; padding-right: 32px; font-size: 16px!important; font-weight: bold;">
                                    <h:outputLabel value="#{vg.subtotal}">
                                        <f:convertNumber pattern="#,##0.00" />
                                    </h:outputLabel>
                                </td>
                            </tr>
                        </table>
                    </div>

                    <div class="billline"><hr/></div>

                    <!-- Signature footer on every page -->
                    <div class="footer">
                        <div class="row">
                            <div class="col-4">
                                <h:outputLabel value="--------------------------" /><br/>
                                <h:outputLabel value="Cashier : #{cc.attrs.bill.creater.webUserPerson.name}" />
                            </div>
                            <div class="col-4">
                                <br/>
                                <h:outputLabel value="Payment Received" />
                            </div>
                            <div class="col-4">
                                <h:outputLabel value="--------------------------" /><br/>
                                <h:outputLabel value="Professional : #{cc.attrs.bill.toStaff.person.nameWithTitle}" />
                            </div>
                        </div>
                    </div>
                </div>
            </ui:repeat>

            <!-- Final summary page (grand totals; bill totals are stored negative) -->
            <div class="a4bill1" style="margin: 0 auto; padding: 0; max-width: 210mm">
                <div class="institutionName">
                    <h:outputLabel value="#{cc.attrs.bill.department.printingName}" />
                </div>
                <div class="billline"><hr/></div>
                <div style="text-align: center; font-weight: bold; font-size: 13pt">
                    <h:outputLabel value="PAYMENT VOUCHER — SUMMARY" />
                </div>
                <div class="billline"><hr/></div>
                <div class="billDetailsFiveFive">
                    <table style="width: 95%!important;" align="center">
                        <tr>
                            <td><h:outputLabel value="Bill ID" class="billDetailsFiveFive" /></td>
                            <td style="text-align: center;">:</td>
                            <td><h:outputLabel value="#{cc.attrs.bill.deptId}" class="billDetailsFiveFive" /></td>
                            <td width="2%" />
                            <td><h:outputLabel value="Doctor" class="billDetailsFiveFive" /></td>
                            <td style="text-align: center;">:</td>
                            <td><h:outputLabel value="#{cc.attrs.bill.staff.person.nameWithTitle}" class="billDetailsFiveFive" /></td>
                        </tr>
                    </table>
                </div>
                <div class="billline"><hr/></div>
                <div>
                    <table style="width: 100%;">
                        <tr>
                            <td style="text-align: left; width: 60%; font-size: 20px!important; font-weight: bold;">
                                &nbsp;&nbsp;<h:outputLabel value="Gross Total" />
                            </td>
                            <td style="text-align: right!important; width: 40%; padding-right: 32px; font-size: 20px!important; font-weight: bold;">
                                <h:outputLabel value="#{-cc.attrs.bill.total}">
                                    <f:convertNumber pattern="#,##0.00" />
                                </h:outputLabel>
                            </td>
                        </tr>
                        <tr>
                            <td style="text-align: left; width: 60%; font-size: 20px!important; font-weight: bold;">
                                &nbsp;&nbsp;<h:outputLabel value="Withholding Tax" />
                            </td>
                            <td style="text-align: right!important; width: 40%; padding-right: 32px; font-size: 20px!important; font-weight: bold;">
                                <h:outputLabel value="#{-cc.attrs.bill.tax}">
                                    <f:convertNumber pattern="#,##0.00" />
                                </h:outputLabel>
                            </td>
                        </tr>
                        <tr>
                            <td style="text-align: left; width: 60%; font-size: 20px!important; font-weight: bold;">
                                &nbsp;&nbsp;<h:outputLabel value="Net Total" />
                            </td>
                            <td style="text-align: right!important; width: 40%; padding-right: 32px; font-size: 20px!important; font-weight: bold;">
                                <h:outputLabel value="#{-cc.attrs.bill.netTotal}">
                                    <f:convertNumber pattern="#,##0.00" />
                                </h:outputLabel>
                            </td>
                        </tr>
                    </table>
                </div>
                <div class="billline"><hr/></div>
                <div class="footer">
                    <div class="row">
                        <div class="col-4">
                            <h:outputLabel value="--------------------------" /><br/>
                            <h:outputLabel value="Cashier : #{cc.attrs.bill.creater.webUserPerson.name}" />
                        </div>
                        <div class="col-4">
                            <br/>
                            <h:outputLabel value="Payment Received" />
                        </div>
                        <div class="col-4">
                            <h:outputLabel value="--------------------------" /><br/>
                            <h:outputLabel value="Professional : #{cc.attrs.bill.toStaff.person.nameWithTitle}" />
                        </div>
                    </div>
                </div>
            </div>

        </h:panelGroup>
    </cc:implementation>
</html>
```

- [ ] **Step 2: Create the 5x5 composite**

Create `src/main/webapp/resources/bill/staff_payment_individual_five_five.xhtml`. Identical inner content to the A4 version with these differences (produce the full file — copy the A4 file and apply):
- Stylesheet: `<h:outputStylesheet library="css" name="opd_five_five.css" />` instead of `inwardpayments.css`.
- Root panel id: `gpIndividualVouchersFiveFive`.
- Per-group wrapper: `<div class="fiveinchbill" style="page-break-after: always;">` and an inner `<div style="margin-left: 5%; margin-right: 5%">` wrapping the whole voucher content (matches `staff_payment_five_five_paper_with_headings.xhtml`).
- Summary-page wrapper: `<div class="fiveinchbill">` (no page break) with the same inner margin div.
- Font sizes: use the existing `billDetailsFiveFive` classes as-is; drop the `max-width: 210mm` styles.

- [ ] **Step 3: Create the POS composite**

Create `src/main/webapp/resources/bill/staff_payment_individual_pos.xhtml`. Same content again with these differences:
- Stylesheet: `<h:outputStylesheet library="css" name="pharmacypos.css" />`.
- Root panel id: `gpIndividualVouchersPos`.
- Per-group wrapper: `<div class="posbillBreak">` (this class already includes the page break — verify in `pharmacypos.css`; if it does not contain `page-break-after`, add `style="page-break-after: always;"` on the div).
- Summary-page wrapper: `<div class="posbill">` (no break). If `posbill` is not defined in `pharmacypos.css`, use `<div class="posbillBreak" style="page-break-after: auto!important;">`.
- Replace the fee-lines `h:dataTable` with a plain narrow table (POS width): keep columns Bill No, Service, Value (drop Fee Type on POS width).
- Signature footer: keep Cashier and Professional lines stacked vertically instead of the 3-column row.

- [ ] **Step 4: Commit**

XHTML-only change — no compilation needed (project rule #12).

```bash
git add src/main/webapp/resources/bill/staff_payment_individual_a4.xhtml \
        src/main/webapp/resources/bill/staff_payment_individual_five_five.xhtml \
        src/main/webapp/resources/bill/staff_payment_individual_pos.xhtml
git commit -m "feat(professional-payments): individual per-patient voucher composites (A4/5x5/POS)

- One mini-voucher per patient/BHT with page-break-after separation
- Patient identity line, fee lines, patient subtotal, signature footer
- Final summary page with gross / WHT / net grand totals

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: Wire the OPD page

**Files:**
- Modify: `src/main/webapp/opd/professional_payments/payment_staff_bill.xhtml`

**Interfaces:**
- Consumes: composites from Task 3; `#{staffPaymentBillController.individualVoucherGroups}` (Task 2). Namespace `bi` is already declared on the page.

- [ ] **Step 1: Add the button**

In the print-preview header (`d-flex gap-2` div, after the "Print Summary" button and before the "Settings" button), add:

```xhtml
                                    <p:commandButton
                                        id="btnPrintIndividual"
                                        value="Print Individual"
                                        class="ui-button-danger"
                                        icon="fas fa-print"
                                        ajax="false"
                                        action="#" >
                                        <p:printer target="gpIndividualPreview" />
                                    </p:commandButton>
```

- [ ] **Step 2: Add the preview panel**

After the closing tag of `<h:panelGroup id="gpSummaryPreview">` (Format 3 block), add:

```xhtml
                        <!-- Format 4: Individual per-patient vouchers -->
                        <h:panelGroup id="gpIndividualPreview" >
                            <h:panelGroup class="mb-4" rendered="#{configOptionApplicationController.getBooleanValueByKey('OPD Doctor payment bill is A4 paper', true)}">
                                <h:panelGroup class="d-flex justify-content-center">
                                    <bi:staff_payment_individual_a4 controller="#{staffPaymentBillController}" bill="#{staffPaymentBillController.current}"/>
                                </h:panelGroup>
                            </h:panelGroup>

                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('OPD Doctor payment bill is five five paper.')}">
                                <bi:staff_payment_individual_five_five controller="#{staffPaymentBillController}" bill="#{staffPaymentBillController.current}"/>
                            </h:panelGroup>

                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('OPD Doctor payment bill is POS paper')}">
                                <bi:staff_payment_individual_pos controller="#{staffPaymentBillController}" bill="#{staffPaymentBillController.current}"/>
                            </h:panelGroup>
                        </h:panelGroup>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/opd/professional_payments/payment_staff_bill.xhtml
git commit -m "feat(professional-payments): Print Individual button on OPD payment page

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: Wire the inward professional payment page

**Files:**
- Modify: `src/main/webapp/inward/inward_bill_professional_payment.xhtml`

**Interfaces:**
- Consumes: composites from Task 3; `#{inwardStaffPaymentBillController.individualVoucherGroups}` (Task 2). Check the page declares `xmlns:bi="http://xmlns.jcp.org/jsf/composite/bill"`; it does (it already uses `bi:staffBill`).

- [ ] **Step 1: Add the button**

In the print-preview panel header (`d-flex gap-2` div, after the existing "Print" button), add:

```xhtml
                                    <p:commandButton
                                        id="btnPrintIndividual"
                                        value="Print Individual"
                                        icon="fa fa-print"
                                        class="ui-button-danger"
                                        ajax="false" action="#" >
                                        <p:printer target="gpIndividualPreview" ></p:printer>
                                    </p:commandButton>
```

- [ ] **Step 2: Add the preview panel**

After the closing `</h:panelGroup>` of `gpBillPreview`, add:

```xhtml
                        <h:panelGroup id="gpIndividualPreview">
                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Doctor payment bill is a A4 paper.', true)}">
                                <bi:staff_payment_individual_a4 controller="#{inwardStaffPaymentBillController}" bill="#{inwardStaffPaymentBillController.current}" />
                            </h:panelGroup>

                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Doctor payment bill is a five five paper.',false)}">
                                <bi:staff_payment_individual_five_five controller="#{inwardStaffPaymentBillController}" bill="#{inwardStaffPaymentBillController.current}" />
                            </h:panelGroup>
                        </h:panelGroup>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/inward/inward_bill_professional_payment.xhtml
git commit -m "feat(professional-payments): Print Individual button on inward payment page

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: Wire the inward surgery payment page

**Files:**
- Modify: `src/main/webapp/inward/inward_bill_surgery_payment.xhtml`

**Interfaces:**
- Consumes: composites from Task 3; `#{inwardSurgeryPaymentBillController.individualVoucherGroups}` (Task 2).

- [ ] **Step 1: Add button + preview panel**

Exactly as Task 5 but with controller `inwardSurgeryPaymentBillController`:
- Button (same markup as Task 5 Step 1) after the existing "Print" button in the preview header.
- Preview panel after `gpBillPreview`'s closing tag:

```xhtml
                        <h:panelGroup id="gpIndividualPreview">
                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Doctor payment bill is a A4 paper.', true)}">
                                <bi:staff_payment_individual_a4 controller="#{inwardSurgeryPaymentBillController}" bill="#{inwardSurgeryPaymentBillController.current}" />
                            </h:panelGroup>

                            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Inward Doctor payment bill is a five five paper.',false)}">
                                <bi:staff_payment_individual_five_five controller="#{inwardSurgeryPaymentBillController}" bill="#{inwardSurgeryPaymentBillController.current}" />
                            </h:panelGroup>
                        </h:panelGroup>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/inward/inward_bill_surgery_payment.xhtml
git commit -m "feat(professional-payments): Print Individual button on surgery payment page

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Wire the channel payment page

**Files:**
- Modify: `src/main/webapp/channel/channel_payment_staff_bill.xhtml`

**Interfaces:**
- Consumes: A4 composite from Task 3; `#{channelStaffPaymentBillController.individualVoucherGroups}` (Task 2). Channel voucher is A4-only (no paper-format config keys on this page).

- [ ] **Step 1: Add the button**

In the print-preview header (`d-flex gap-2` div, after the existing "Print" button), add:

```xhtml
                                    <p:commandButton
                                        id="btnPrintIndividual"
                                        value="Print Individual"
                                        class="ui-button-danger"
                                        icon="fas fa-print"
                                        ajax="false"
                                        action="#" >
                                        <p:printer target="gpIndividualPreview" ></p:printer>
                                    </p:commandButton>
```

- [ ] **Step 2: Add the preview panel**

After the closing tag of `<h:panelGroup id="gpBillPreview" ...>`, add:

```xhtml
                        <h:panelGroup id="gpIndividualPreview" class="d-flex justify-content-center">
                            <bi:staff_payment_individual_a4 controller="#{channelStaffPaymentBillController}" bill="#{channelStaffPaymentBillController.current}"/>
                        </h:panelGroup>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/channel/channel_payment_staff_bill.xhtml
git commit -m "feat(professional-payments): Print Individual button on channel payment page

Refs #17489

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 8: Build, deploy, and verify end-to-end

**Files:** none (verification task; fixes loop back into earlier tasks' files).

**Interfaces:**
- Consumes: everything above; local Payara domain `rh` (`/home/carecode/payara`, app at `http://localhost:9080/rh/`), Playwright MCP (login + department selection first — see `developer_docs/testing/playwright-e2e-workflow.md`).

- [ ] **Step 1: Full build**

Run: `mvn -q clean package -DskipTests 2>&1 | tail -5`
Expected: BUILD SUCCESS, `target/*.war` produced.

- [ ] **Step 2: Deploy to local Payara**

Use the project's usual redeploy (asadmin against domain `rh`); verify startup in `/home/carecode/payara/glassfish/domains/rh/logs/server.log` with no deployment errors.

- [ ] **Step 3: Playwright E2E — OPD**

1. Login, select department (mandatory), navigate to OPD → Professional Payments → To Pay (`/faces/opd/professional_payments/payment_staff_bill.xhtml`).
2. Pick a doctor with due fees for **at least 2 different patients**; select their fees; Settle (handle the JS confirm).
3. On the preview: click nothing yet — assert the page contains `gpIndividualPreview` with **2 patient blocks** (patient names + MRN lines) **+ 1 summary block**, correct fee lines and subtotals, and that each patient block's wrapper carries `page-break-after: always`.
4. Assert grand totals in the summary block equal the existing Detailed preview totals.

- [ ] **Step 4: Playwright E2E — Inward**

Same flow on `/faces/inward/inward_bill_professional_payment.xhtml` with fees from **2 BHTs**; assert BHT numbers appear as identifiers.
If no suitable due inward fees exist in local data, verify with 1 BHT and note it.

- [ ] **Step 5: DB cross-check**

Query the created payment bill's items (local MySQL, user `buddhika`): sum of `netValue` grouped by source-bill patient must equal each on-screen subtotal, and `-bill.netTotal` (+ WHT relationship) must match the summary block.

- [ ] **Step 6: Regression glance**

Existing Print Detailed / Summarised / Summary previews (OPD) and Print (inward) still render unchanged.

- [ ] **Step 7: Fix-loop**

Any failure: fix in the owning task's files, rebuild, redeploy, re-verify. Commit fixes with `fix(professional-payments): ...` messages.

---

### Task 9: Push and create the PR

**Files:** none.

- [ ] **Step 1: Verify persistence.xml is not staged**

Run: `git status --short src/main/resources/META-INF/persistence.xml`
Expected: ` M ...persistence.xml` (unstaged modification only). If ever staged: `git restore --staged src/main/resources/META-INF/persistence.xml`.

- [ ] **Step 2: Push**

```bash
git push
```

- [ ] **Step 3: Confirm local JNDI still in working tree** (project rule #17)

Run: `grep -n 'jta-data-source' src/main/resources/META-INF/persistence.xml`
Expected: `jdbc/coop` and `jdbc/ruhunuAudit` (the local working-tree values were never committed; nothing to restore — but if they were overwritten, restore them now, unstaged).

- [ ] **Step 4: Create the PR targeting `development`**

```bash
gh pr create --repo hmislk/hmis --base development \
  --title "feat(professional-payments): one-click individual per-patient/BHT payment vouchers" \
  --body "$(cat <<'EOF'
## Summary
- Adds a **Print Individual** button to all 4 professional-payment settle pages (OPD, inward, inward surgery, channel)
- Prints one full mini-voucher per OPD patient / BHT — header, patient identity, fee lines, subtotal, signature block — separated by page breaks, plus a final summary page with gross / withholding tax / net grand totals
- New `ProfessionalPaymentVoucherGroup` + grouping method in `ProfessionalPaymentService` (JPQL, unit-tested); three reusable voucher composites (A4 / 5x5 / POS) honoring each page's existing paper-format config keys
- Purely additive: existing print buttons, vouchers, and settle flows untouched

## Design
`developer_docs/specs/2026-07-11-individual-professional-payment-print-design.md`

## Test plan
- [x] JUnit: grouping by patient (OPD), by BHT (inward), miscellaneous fallback
- [x] Playwright E2E: OPD settle with 2 patients → 2 vouchers + summary; inward with BHT identifiers
- [x] DB cross-check of subtotals vs bill netTotal
- [x] Existing prints regression-checked

Closes #17489

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

(Only tick the test-plan boxes that Task 8 actually verified.)

- [ ] **Step 5: Report PR URL to the user**

---

## Self-Review (done at plan time)

- **Spec coverage:** 4 pages (Tasks 4–7) ✔; grouping helper + POJO (Task 1) ✔; controller getters (Task 2) ✔; 3 format composites with page breaks + summary page + signature-on-every-page (Task 3) ✔; config-key gating per page (Tasks 4–6, channel A4-only Task 7) ✔; miscellaneous group (Task 1 test) ✔; testing plan (Task 8) ✔.
- **Deviation from spec (documented in Global Constraints):** per-group subtotals display positive item values as-is; only grand totals are negated — matches how the payment bill actually stores values (items positive, bill totals negative).
- **Type consistency:** `getIndividualVoucherGroups()` (Tasks 2→3), `ProfessionalPaymentVoucherGroup` getters (Tasks 1→3), composite names (Tasks 3→4/5/6/7) all match.
