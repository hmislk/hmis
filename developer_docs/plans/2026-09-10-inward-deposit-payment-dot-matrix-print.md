# Inward Deposit & Payment — Dot-Matrix / Raw-Text Print Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give the inward Deposit and Payment 5×5 receipts two new print buttons — a monospace "Print (Dot-Matrix)" and a raw-bytes "Print (Raw Text)" — so Coop's Epson LQ-310 output is legible.

**Architecture:** Track A is a new PrimeFaces composite component (`FiveFiveDotMatrixPaymentBill`) plus a scoped stylesheet (`five_five_dotmatrix.css`): one fixed-width font, `pt` sizing, literal character rules, no `text-transform`, a fixed 40-column grid, and ConfigOptions to suppress the app header / drop the body below pre-printed stationery. Track B adds a method to `InwardDepositController` and `InwardPaymentController` that builds the same 40-column layout as plain text (optionally wrapped in ESC/P control codes) and streams it as an `application/octet-stream` `.prn` download; a watched-folder PowerShell agent on the cashier PC raw-copies it to the printer. All four pages (Deposit + Payment, live + reprint) get both buttons.

**Tech Stack:** JSF 2.x / Facelets composite components, PrimeFaces (`p:printer`, `p:commandButton`), CDI `@SessionScoped` controllers, `ConfigOptionApplicationController` (lazy per-key + per-department config), `FacesContext` → `HttpServletResponse` streaming (same pattern as `SymptomController.java:107-115`), MySQL/EclipseLink via existing facades (read-only here).

**Spec:** `developer_docs/specs/2026-09-10-inward-deposit-payment-dot-matrix-print-design.md`

## Global Constraints

- **Branch:** `inward-deposit-payment-dot-matrix-print`, based on `origin/development`. PR targets `development`, never `master`.
- **No mock data.** Text builder reads the real `Bill` / `PatientEncounter` / `Department`.
- **Never modify existing constructors.** Only add methods; do not change existing component `cc:interface` attributes.
- **ConfigOptions are lazy-created** on first `getBooleanValueByKey(key, default)` / `getIntegerValueByKey(key, default)` call — there is no seed file to edit. Resolve per-department-first only where the spec says (stationery + top-margin options); the page-default toggles use the plain accessors like the existing `Inward Payment Bill * Paper` keys.
- **Do not hardcode a hospital name** to gate behaviour. Everything is ConfigOption-driven.
- **JPQL only** — but this feature issues no new queries; it reads already-loaded entity graphs.
- **persistence.xml:** leave the local JNDI (`jdbc/coop`, `jdbc/ruhunuAudit`) as-is while developing; swap to `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` immediately before each `git push`, then restore, unstaged.
- **Exact ConfigOption keys (verbatim):**
  - `Inward Payment Bill Dot Matrix Paper` (boolean, default `false`)
  - `Inward Dot Matrix Receipt Preprinted Stationery` (boolean, default `false`, per-department-first)
  - `Inward Dot Matrix Receipt Top Margin Lines` (integer, default `8`, per-department-first)
  - `Print Barcode on Inward Dot Matrix Receipt` (boolean, default `false`)
  - `Inward Raw Text Receipt Preprinted Stationery` (boolean, default `false`, per-department-first)
  - `Inward Raw Text Receipt Top Margin Lines` (integer, default `8`, per-department-first)
  - `Inward Raw Text Receipt Emit ESC/P Codes` (boolean, default `true`)
- **Column width:** exactly **40 characters** for both tracks.
- **Testing rule:** pure-XHTML/CSS changes need no compile; any `.java` change needs `mvn compile` + local redeploy + Playwright per the `playwright-e2e` skill.

---

## File Structure

**Create:**

| File | Responsibility |
|---|---|
| `src/main/webapp/resources/inward/bill/payment/FiveFiveDotMatrixPaymentBill.xhtml` | Track A composite component: monospace 40-col receipt for Deposit & Payment. |
| `src/main/webapp/resources/css/five_five_dotmatrix.css` | Track A stylesheet, all selectors `dmx-` / `.dmxbill` prefixed, `@media print` + `@media screen`. |
| `src/main/java/com/divudi/core/util/InwardReceiptTextRenderer.java` | Pure static helper: `Bill` → 40-col `String` (+ optional ESC/P wrap). No CDI, no DB — unit-testable. |
| `src/test/java/com/divudi/core/util/InwardReceiptTextRendererTest.java` | Unit tests for the renderer. |
| `developer_docs/printing/raw-text-print-agent.ps1` | Watched-folder PowerShell agent for the cashier PC. |
| `hmis.wiki/Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts.md` | End-user + on-site-tech doc: both buttons, Track A driver/Chrome settings, Track B agent setup + troubleshooting. |

**Modify:**

| File | Change |
|---|---|
| `src/main/webapp/inward/inward_bill_deposit.xhtml` | Add `gpBillPreviewDmx` group + `Print (Dot-Matrix)` + `Print (Raw Text)` buttons (heading "Deposit Receipt"). |
| `src/main/webapp/inward/inward_bill_payment.xhtml` | Same (heading "Payment Receipt"). |
| `src/main/webapp/inward/inward_reprint_bill_deposit.xhtml` | Same two buttons + preview group, `duplicate="true"`. |
| `src/main/webapp/inward/inward_reprint_bill_payment.xhtml` | Same. |
| `src/main/java/com/divudi/bean/inward/InwardDepositController.java` | Add `streamCurrentDepositReceiptAsRawText()`. |
| `src/main/java/com/divudi/bean/inward/InwardPaymentController.java` | Add `streamCurrentPaymentReceiptAsRawText()`. |
| `src/main/java/com/divudi/bean/common/SearchController.java` **or** `InwardSearch.java` | Add `streamReprintReceiptAsRawText()` for the two reprint pages (see Task 8 — decide which bean the reprint pages already use for `inwardSearch.bill`). |

---

## Task 1: Track A stylesheet — `five_five_dotmatrix.css`

**Files:**
- Create: `src/main/webapp/resources/css/five_five_dotmatrix.css`

**Interfaces:**
- Consumes: nothing.
- Produces: CSS classes used by Task 2 — `.dmxbill`, `.dmx-header`, `.dmx-heading`, `.dmx-rule`, `.dmx-row`, `.dmx-label`, `.dmx-value`, `.dmx-amount-row`, `.dmx-amount-label`, `.dmx-amount-value`, `.dmx-footer`, `.dmx-cashier`, `.dmx-pre`.

- [ ] **Step 1: Write the stylesheet**

```css
/* Dot-matrix / impact-printer profile for inward Deposit & Payment 5x5 receipts.
   One fixed-width font, pt sizing, no text-transform, character rules only.
   Target: Epson LQ-310 at 10 CPI / 6 LPI, 40-column body (~4in) inside a 5in form.
   All selectors are prefixed dmx- / .dmxbill so this cannot affect other 5x5 bills. */

.dmxbill {
    font-family: "Courier New", "Courier", monospace;
    font-size: 12pt;
    line-height: 1;
    color: #000;
    width: 40ch;
    margin: 0;
    padding: 0;
    letter-spacing: 0;
    /* NO text-transform. NO % / px font sizes. */
}

.dmxbill .dmx-header,
.dmxbill .dmx-heading {
    text-align: center;
    white-space: pre;
}

.dmxbill .dmx-heading {
    font-size: 13pt;
    font-weight: bold;
}

.dmxbill .dmx-rule {
    white-space: pre;
    overflow: hidden;
    letter-spacing: 0;
}

.dmxbill .dmx-row {
    display: flex;
    width: 40ch;
}

.dmxbill .dmx-label {
    flex: 0 0 16ch;
    white-space: pre;
}

.dmxbill .dmx-value {
    flex: 1 1 auto;
    white-space: pre-wrap;
    word-break: break-word;
}

.dmxbill .dmx-amount-row {
    display: flex;
    width: 40ch;
    font-size: 13pt;
    font-weight: bold;
}

.dmxbill .dmx-amount-label { flex: 1 1 auto; white-space: pre; }
.dmxbill .dmx-amount-value { flex: 0 0 14ch; text-align: right; white-space: pre; }

.dmxbill .dmx-cashier,
.dmxbill .dmx-footer {
    white-space: pre-wrap;
    text-align: left;
}

.dmxbill .dmx-footer { text-align: center; }

.dmxbill .dmx-pre {
    white-space: pre;
    font-family: "Courier New", "Courier", monospace;
    font-size: 12pt;
}

@media print {
    .dmxbill {
        width: 40ch;
    }
    /* Page/form geometry is left to the printer driver (custom 5x5, tractor feed).
       Do NOT set negative margins or fixed cm boxes here. */
    @page {
        margin: 0;
    }
}

@media screen {
    .dmxbill {
        width: 40ch;
        border: 1px dashed #999;
        background: #fff;
        padding: 4px;
    }
}
```

- [ ] **Step 2: Verify it is valid CSS and loads**

Run: `npx --yes csslint@1.0.5 src/main/webapp/resources/css/five_five_dotmatrix.css || true`
Expected: no parse errors (csslint style warnings are acceptable).

- [ ] **Step 3: Commit**

```bash
git add src/main/webapp/resources/css/five_five_dotmatrix.css
git commit -m "feat(inward): add dot-matrix stylesheet for 5x5 deposit/payment receipts"
```

---

## Task 2: Track A component — `FiveFiveDotMatrixPaymentBill.xhtml`

**Files:**
- Create: `src/main/webapp/resources/inward/bill/payment/FiveFiveDotMatrixPaymentBill.xhtml`

**Interfaces:**
- Consumes: `five_five_dotmatrix.css` classes from Task 1; `configOptionApplicationController` (already a `@Named` bean), `sessionController`, `configOptionController`.
- Produces: composite tag `bill:FiveFiveDotMatrixPaymentBill` with attributes:
  - `bill` — `com.divudi.core.entity.Bill` (required)
  - `duplicate` — `java.lang.Boolean` (optional)
  - `heading` — `java.lang.String` (optional, default `"Deposit Receipt"`)
  Namespace is the existing `xmlns:bill="http://xmlns.jcp.org/jsf/composite/inward/bill/payment"`.

- [ ] **Step 1: Write the component**

```xml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
      xmlns:p="http://primefaces.org/ui">

    <cc:interface>
        <cc:attribute name="bill" type="com.divudi.core.entity.Bill" required="true"/>
        <cc:attribute name="duplicate" type="java.lang.Boolean"/>
        <cc:attribute name="heading" type="java.lang.String" default="Deposit Receipt"/>
    </cc:interface>

    <cc:implementation>
        <h:outputStylesheet library="css" name="five_five_dotmatrix.css"/>

        <ui:param name="preprinted"
                  value="#{configOptionApplicationController.getBooleanValueByKey('Inward Dot Matrix Receipt Preprinted Stationery', false)}"/>
        <ui:param name="topMarginLines"
                  value="#{configOptionApplicationController.getIntegerValueByKey('Inward Dot Matrix Receipt Top Margin Lines', 8)}"/>

        <div class="dmxbill" style="page-break-after: always;">

            <!-- Leading blank lines so the body clears pre-printed letterhead -->
            <h:panelGroup rendered="#{preprinted}">
                <ui:repeat value="#{configOptionApplicationController.integerList(topMarginLines)}" var="i">
                    <div class="dmx-pre"> </div>
                </ui:repeat>
            </h:panelGroup>

            <!-- App-rendered text header, only on blank continuous paper -->
            <h:panelGroup rendered="#{not preprinted}">
                <div class="dmx-header">
                    <h:outputText value="#{cc.attrs.bill.department.printingName}"/>
                </div>
                <div class="dmx-header">
                    <h:outputText value="#{cc.attrs.bill.department.address}"/>
                </div>
                <div class="dmx-header">
                    <h:outputText value="#{cc.attrs.bill.department.telephone1}"/>
                    <h:outputText value=" / #{cc.attrs.bill.department.telephone2}"
                                  rendered="#{not empty cc.attrs.bill.department.telephone2}"/>
                </div>
                <div class="dmx-header" rendered="#{not empty cc.attrs.bill.department.fax}">
                    <h:outputText value="Fax: #{cc.attrs.bill.department.fax}"/>
                </div>
            </h:panelGroup>

            <div class="dmx-heading">
                <h:outputText value="#{cc.attrs.heading}"/>
                <h:outputText value=" **Duplicate**" rendered="#{cc.attrs.duplicate eq true}"/>
                <h:outputText value=" **Cancelled**" rendered="#{cc.attrs.bill.cancelled eq true}"/>
            </div>

            <div class="dmx-rule">----------------------------------------</div>

            <div class="dmx-row"><span class="dmx-label">Admission Type :</span><span class="dmx-value">#{cc.attrs.bill.patientEncounter.admissionType.name}</span></div>
            <div class="dmx-row"><span class="dmx-label">Name          :</span><span class="dmx-value">#{cc.attrs.bill.patientEncounter.patient.person.nameWithTitle}</span></div>
            <div class="dmx-row">
                <span class="dmx-label">Age / Gender  :</span>
                <span class="dmx-value">
                    <h:outputText value="#{cc.attrs.bill.patientEncounter.patient.age} "/>
                    <h:outputText value="#{cc.attrs.bill.patientEncounter.patient.person.sex}"/>
                </span>
            </div>
            <div class="dmx-row"><span class="dmx-label">Address       :</span><span class="dmx-value">#{cc.attrs.bill.patientEncounter.patient.person.address}</span></div>
            <div class="dmx-row"><span class="dmx-label">Phone         :</span><span class="dmx-value">#{cc.attrs.bill.patientEncounter.patient.person.phone}</span></div>
            <div class="dmx-row"><span class="dmx-label">BHT No        :</span><span class="dmx-value">#{cc.attrs.bill.patientEncounter.bhtNo}</span></div>
            <div class="dmx-row"><span class="dmx-label">Bill No       :</span><span class="dmx-value">#{cc.attrs.bill.deptId}</span></div>
            <div class="dmx-row">
                <span class="dmx-label">Bill Date     :</span>
                <span class="dmx-value">
                    <h:outputText value="#{cc.attrs.bill.createdAt}">
                        <f:convertDateTime pattern="dd/MMM/yyyy" timeZone="Asia/Colombo"/>
                    </h:outputText>
                </span>
            </div>
            <div class="dmx-row">
                <span class="dmx-label">Bill Time     :</span>
                <span class="dmx-value">
                    <h:outputText value="#{cc.attrs.bill.createdAt}">
                        <f:convertDateTime pattern="hh:mm a" timeZone="Asia/Colombo"/>
                    </h:outputText>
                </span>
            </div>
            <div class="dmx-row"><span class="dmx-label">Payment       :</span><span class="dmx-value">#{cc.attrs.bill.paymentMethod}</span></div>

            <!-- Multiple payment method breakdown as plain monospace rows -->
            <h:panelGroup rendered="#{cc.attrs.bill.paymentMethod eq 'MultiplePaymentMethods'}">
                <div class="dmx-rule">- - - - - - - - - - - - - - - - - - - - </div>
                <ui:repeat value="#{billSearch.fetchBillPayments(cc.attrs.bill)}" var="ps">
                    <div class="dmx-amount-row">
                        <span class="dmx-amount-label">#{ps.paymentMethod}<h:outputText value=" (#{ps.creditCardRefNo})" rendered="#{ps.paymentMethod eq 'Card'}"/></span>
                        <span class="dmx-amount-value"><h:outputText value="#{ps.paidValue}"><f:convertNumber pattern="#,##0.00"/></h:outputText></span>
                    </div>
                </ui:repeat>
            </h:panelGroup>

            <div class="dmx-rule">========================================</div>
            <div class="dmx-amount-row">
                <span class="dmx-amount-label">Paying Amount</span>
                <span class="dmx-amount-value"><h:outputText value="#{cc.attrs.bill.total}"><f:convertNumber pattern="#,##0.00"/></h:outputText></span>
            </div>
            <div class="dmx-rule">========================================</div>

            <h:panelGroup rendered="#{configOptionApplicationController.getBooleanValueByKey('Show Comment on Inward Deposit Bill', false) and not empty cc.attrs.bill.comments}">
                <div class="dmx-row"><span class="dmx-label">Comment       :</span><span class="dmx-value">#{cc.attrs.bill.comments}</span></div>
            </h:panelGroup>

            <h:panelGroup rendered="#{configOptionController.getBooleanValueByKey('Print Barcode on Inward Dot Matrix Receipt', false)}">
                <div class="dmx-header" style="font-size: 10px;">
                    <p:barcode value="#{cc.attrs.bill.idStr}" type="code39" cache="false"/>
                </div>
            </h:panelGroup>

            <div class="dmx-pre"> </div>
            <div class="dmx-cashier">Cashier : #{cc.attrs.bill.creater.webUserPerson.name}</div>
            <div class="dmx-pre"> </div>
            <div class="dmx-footer"><h:outputText value="#{sessionController.userPreference.pharmacyBillFooter}"/></div>
        </div>
    </cc:implementation>
</html>
```

- [ ] **Step 2: Add the `integerList` helper the leading-lines loop needs**

`ui:repeat` needs a collection. Add to `ConfigOptionApplicationController` (a `@Named` bean already in scope on these pages):

```java
/**
 * Returns a list of {@code count} zero-based Integers, for {@code ui:repeat}
 * loops that just need to render N copies of something (e.g. blank leading
 * lines above a pre-printed dot-matrix letterhead). Clamps to [0, 40].
 */
public java.util.List<Integer> integerList(Integer count) {
    int n = count == null ? 0 : Math.max(0, Math.min(40, count));
    java.util.List<Integer> out = new java.util.ArrayList<>(n);
    for (int i = 0; i < n; i++) {
        out.add(i);
    }
    return out;
}
```

- [ ] **Step 3: Compile**

Run: `"C:\Program Files\OpenLogic\jdk-11.0.24.8-hotspot\bin\java.exe" -version && mvn -q -o compile` (Maven via NetBeans-20; see `developer_docs` reference-maven-path). If offline mode fails, drop `-o`.
Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/webapp/resources/inward/bill/payment/FiveFiveDotMatrixPaymentBill.xhtml src/main/java/com/divudi/bean/common/ConfigOptionApplicationController.java
git commit -m "feat(inward): add FiveFiveDotMatrixPaymentBill component + integerList helper"
```

---

## Task 3: Wire Track A + Track B buttons into `inward_bill_deposit.xhtml`

**Files:**
- Modify: `src/main/webapp/inward/inward_bill_deposit.xhtml` (button facet ~line 435-442; preview group `gpBillPreview` ~line 468-490)

**Interfaces:**
- Consumes: `bill:FiveFiveDotMatrixPaymentBill` (Task 2); `inwardDepositController.streamCurrentDepositReceiptAsRawText` (Task 6 — the button is added now, the method lands in Task 6; between the two commits the button will 500 if clicked, which is acceptable mid-plan and covered by Task 6's test).
- Produces: nothing consumed downstream.

- [ ] **Step 1: Add the two buttons** immediately after the existing `Print` button (the one with `<p:printer target="gpBillPreview">`), inside the same `<div class="d-flex gap-2" style="float: right;">`:

```xml
<p:commandButton
    value="Print (Dot-Matrix)"
    ajax="false"
    icon="fa fa-print"
    class="ui-button-info"
    action="#"  >
    <p:printer target="gpBillPreviewDmx" ></p:printer>
</p:commandButton>
<p:commandButton
    value="Print (Raw Text)"
    ajax="false"
    icon="fa fa-file-lines"
    class="ui-button-help"
    action="#{inwardDepositController.streamCurrentDepositReceiptAsRawText}" >
</p:commandButton>
```

- [ ] **Step 2: Add the hidden dot-matrix preview group** inside the `<div class="row">` that holds `gpBillPreview`, right after the closing `</h:panelGroup>` of `gpBillPreview`:

```xml
<h:panelGroup id="gpBillPreviewDmx" style="display:none;">
    <div class="d-flex justify-content-center">
        <bill:FiveFiveDotMatrixPaymentBill bill="#{inwardDepositController.current}" heading="Deposit Receipt" />
    </div>
</h:panelGroup>
```

Note: `p:printer` prints the target's markup even when it is `display:none` on screen. Keep it hidden so it does not double the on-screen preview.

- [ ] **Step 3: Verify the `bill:` namespace is declared** at the top of the file. It is (`xmlns:bill="http://xmlns.jcp.org/jsf/composite/inward/bill/payment"`). No change needed.

- [ ] **Step 4: Redeploy and smoke-test (JSF-only change, no compile)**

Follow the `playwright-e2e` skill: login → select Department → menu Inpatient → Inward → Deposit. Create a small cash deposit. On the print-preview panel confirm: 3 print buttons visible; clicking **Print (Dot-Matrix)** opens the browser print dialog showing the monospace receipt; the on-screen preview is not duplicated.

- [ ] **Step 5: Commit**

```bash
git add src/main/webapp/inward/inward_bill_deposit.xhtml
git commit -m "feat(inward): add Dot-Matrix and Raw Text print buttons to inward deposit bill"
```

---

## Task 4: Wire the same two buttons into `inward_bill_payment.xhtml`

**Files:**
- Modify: `src/main/webapp/inward/inward_bill_payment.xhtml` (button facet ~line 418-425; preview group `gpBillPreview` ~line 451-473)

**Interfaces:**
- Consumes: `bill:FiveFiveDotMatrixPaymentBill` (Task 2); `inwardPaymentController.streamCurrentPaymentReceiptAsRawText` (Task 7).
- Produces: nothing.

- [ ] **Step 1: Add the two buttons** right after the existing `Print` button (`<p:printer target="gpBillPreview">`):

```xml
<p:commandButton
    value="Print (Dot-Matrix)"
    ajax="false"
    icon="fa fa-print"
    class="ui-button-info"
    action="#"  >
    <p:printer target="gpBillPreviewDmx" ></p:printer>
</p:commandButton>
<p:commandButton
    value="Print (Raw Text)"
    ajax="false"
    icon="fa fa-file-lines"
    class="ui-button-help"
    action="#{inwardPaymentController.streamCurrentPaymentReceiptAsRawText}" >
</p:commandButton>
```

- [ ] **Step 2: Add the hidden preview group** after the closing `</h:panelGroup>` of `gpBillPreview`:

```xml
<h:panelGroup id="gpBillPreviewDmx" style="display:none;">
    <div class="d-flex justify-content-center">
        <bill:FiveFiveDotMatrixPaymentBill bill="#{inwardPaymentController.current}" heading="Payment Receipt" />
    </div>
</h:panelGroup>
```

- [ ] **Step 3: Redeploy and smoke-test**

`playwright-e2e`: menu Inpatient → Inward → Payment. Make a payment. Confirm the 3 buttons; **Print (Dot-Matrix)** shows the monospace receipt with heading "Payment Receipt".

- [ ] **Step 4: Commit**

```bash
git add src/main/webapp/inward/inward_bill_payment.xhtml
git commit -m "feat(inward): add Dot-Matrix and Raw Text print buttons to inward payment bill"
```

---

## Task 5: Text renderer — `InwardReceiptTextRenderer` (TDD)

**Files:**
- Create: `src/main/java/com/divudi/core/util/InwardReceiptTextRenderer.java`
- Test: `src/test/java/com/divudi/core/util/InwardReceiptTextRendererTest.java`

**Interfaces:**
- Consumes: `com.divudi.core.entity.Bill` (getters: `getDeptId()`, `getTotal()`, `getCreatedAt()`, `getPaymentMethod()`, `getComments()`, `isCancelled()`, `getPatientEncounter()`, `getDepartment()`, `getCreater().getWebUserPerson().getName()`); `PatientEncounter` (`getBhtNo()`, `getAdmissionType().getName()`, `getPatient().getAge()`, `getPatient().getPerson()` → `getNameWithTitle()`, `getSex()`, `getAddress()`, `getPhone()`); `Department` (`getPrintingName()`, `getAddress()`, `getTelephone1()`, `getTelephone2()`, `getFax()`).
- Produces:
  ```java
  public static String render(Bill bill, String heading, boolean duplicate,
                              boolean preprintedStationery, int topMarginLines,
                              boolean emitEscP)
  ```
  Returns the full receipt text. When `emitEscP` is true, the string starts with ESC/P init bytes and ends with a form feed. Column width is a `public static final int WIDTH = 40;`.

- [ ] **Step 1: Write failing tests**

```java
package com.divudi.core.util;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import java.util.Calendar;
import org.junit.Test;
import static org.junit.Assert.*;

public class InwardReceiptTextRendererTest {

    private Bill sampleBill() {
        Person person = new Person();
        person.setName("H K Isali Lisansa");
        person.setSex(com.divudi.core.data.Sex.Female);
        person.setAddress("Rathgama");
        person.setPhone("0770000000");

        Patient patient = new Patient();
        patient.setPerson(person);

        AdmissionType at = new AdmissionType();
        at.setName("BHT");

        PatientEncounter pe = new PatientEncounter();
        pe.setBhtNo("BHT/57939");
        pe.setAdmissionType(at);
        pe.setPatient(patient);

        Department dept = new Department();
        dept.setPrintingName("Galle Co-operative Hospital Ltd.");
        dept.setAddress("No.65, H.W. Amarasooriya Mawatha, Galle");
        dept.setTelephone1("091-2234270");

        Person cashierPerson = new Person();
        cashierPerson.setName("Ziyana");
        WebUser cashier = new WebUser();
        cashier.setWebUserPerson(cashierPerson);

        BilledBill b = new BilledBill();
        b.setDeptId("Inward/26/052052");
        b.setPatientEncounter(pe);
        b.setDepartment(dept);
        b.setPaymentMethod(PaymentMethod.Cash);
        b.setTotal(10000.0);
        b.setCreater(cashier);
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.SEPTEMBER, 9, 21, 39, 0);
        b.setCreatedAt(c.getTime());
        return b;
    }

    @Test
    public void everyLineIsAtMost40CharsWide() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false);
        for (String line : out.split("\n", -1)) {
            assertTrue("line too wide (" + line.length() + "): [" + line + "]",
                    line.length() <= InwardReceiptTextRenderer.WIDTH);
        }
    }

    @Test
    public void containsKeyFields() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false);
        assertTrue(out.contains("Deposit Receipt"));
        assertTrue(out.contains("BHT/57939"));
        assertTrue(out.contains("Inward/26/052052"));
        assertTrue(out.contains("10,000.00"));
        assertTrue(out.contains("Cashier : Ziyana"));
        assertTrue(out.contains("H K Isali Lisansa"));
    }

    @Test
    public void headerPrintedWhenNotPreprinted() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false);
        assertTrue(out.contains("Galle Co-operative Hospital Ltd."));
    }

    @Test
    public void headerSuppressedAndTopMarginAppliedWhenPreprinted() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, true, 5, false);
        assertFalse(out.contains("Galle Co-operative Hospital Ltd."));
        assertTrue("expected 5 leading blank lines",
                out.startsWith("\n\n\n\n\n"));
    }

    @Test
    public void duplicateMarkerShownWhenDuplicate() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                true, false, 0, false);
        assertTrue(out.contains("**Duplicate**"));
    }

    @Test
    public void escPPrologueAndFormFeedWhenEmitEscP() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, true);
        assertEquals("ESC @ init expected at start", 0x1B, out.charAt(0));
        assertEquals('@', out.charAt(1));
        assertTrue("form feed expected at end", out.endsWith("\f"));
    }

    @Test
    public void noEscPBytesWhenEmitEscPFalse() {
        String out = InwardReceiptTextRenderer.render(sampleBill(), "Deposit Receipt",
                false, false, 0, false);
        assertFalse(out.contains("\u001B"));
        assertFalse(out.contains("\f"));
    }
}
```

- [ ] **Step 2: Run tests — expect failure**

Run: `mvn -q -o test -Dtest=InwardReceiptTextRendererTest`
Expected: FAIL — `InwardReceiptTextRenderer` does not exist / does not compile.

- [ ] **Step 3: Implement the renderer**

```java
package com.divudi.core.util;

import com.divudi.core.entity.Bill;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Department;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Renders an inward deposit / payment receipt as fixed-width plain text for
 * impact (dot-matrix) printers. 40-column body. Optionally wrapped in ESC/P
 * control codes for raw printing that bypasses the browser rasteriser.
 *
 * Pure and side-effect free: no CDI, no DB, no FacesContext — unit-testable.
 */
public final class InwardReceiptTextRenderer {

    public static final int WIDTH = 40;
    private static final int LABEL_WIDTH = 15; // "Admission Type " then ':'

    private InwardReceiptTextRenderer() {
    }

    public static String render(Bill bill, String heading, boolean duplicate,
            boolean preprintedStationery, int topMarginLines, boolean emitEscP) {
        StringBuilder sb = new StringBuilder(1024);

        if (emitEscP) {
            sb.append('\u001B').append('@');   // ESC @  — initialise
            sb.append('\u001B').append('x').append('\u0001'); // ESC x 1 — LQ mode
            sb.append('\u001B').append('P');   // ESC P  — 10 CPI
        }

        int margin = Math.max(0, Math.min(40, topMarginLines));
        for (int i = 0; i < margin; i++) {
            sb.append('\n');
        }

        Department dept = bill.getDepartment();
        if (!preprintedStationery && dept != null) {
            centre(sb, safe(dept.getPrintingName()));
            centre(sb, safe(dept.getAddress()));
            String tel = safe(dept.getTelephone1());
            if (notBlank(dept.getTelephone2())) {
                tel = tel + " / " + dept.getTelephone2().trim();
            }
            centre(sb, tel);
            if (notBlank(dept.getFax())) {
                centre(sb, "Fax: " + dept.getFax().trim());
            }
        }

        String head = safe(heading);
        if (duplicate) {
            head = head + " **Duplicate**";
        }
        if (bill.isCancelled()) {
            head = head + " **Cancelled**";
        }
        centre(sb, head);
        rule(sb, '-');

        PatientEncounter pe = bill.getPatientEncounter();
        String admissionType = pe != null && pe.getAdmissionType() != null
                ? safe(pe.getAdmissionType().getName()) : "";
        String name = "", sex = "", address = "", phone = "", age = "";
        if (pe != null && pe.getPatient() != null && pe.getPatient().getPerson() != null) {
            name = safe(pe.getPatient().getPerson().getNameWithTitle());
            sex = pe.getPatient().getPerson().getSex() != null
                    ? pe.getPatient().getPerson().getSex().toString() : "";
            address = safe(pe.getPatient().getPerson().getAddress());
            phone = safe(pe.getPatient().getPerson().getPhone());
        }
        if (pe != null && pe.getPatient() != null) {
            age = String.valueOf(pe.getPatient().getAge());
        }
        String bht = pe != null ? safe(pe.getBhtNo()) : "";

        DecimalFormat money = new DecimalFormat("#,##0.00");
        SimpleDateFormat dfDate = new SimpleDateFormat("dd/MMM/yyyy");
        SimpleDateFormat dfTime = new SimpleDateFormat("hh:mm a");
        Date created = bill.getCreatedAt();

        field(sb, "Admission Type", admissionType);
        field(sb, "Name", name);
        field(sb, "Age / Gender", (age + " " + sex).trim());
        field(sb, "Address", address);
        field(sb, "Phone", phone);
        field(sb, "BHT No", bht);
        field(sb, "Bill No", safe(bill.getDeptId()));
        field(sb, "Bill Date", created == null ? "" : dfDate.format(created));
        field(sb, "Bill Time", created == null ? "" : dfTime.format(created));
        field(sb, "Payment", bill.getPaymentMethod() == null ? ""
                : bill.getPaymentMethod().toString());

        rule(sb, '=');
        String amt = money.format(bill.getTotal());
        String amtLabel = "Paying Amount";
        int pad = WIDTH - amtLabel.length() - amt.length();
        if (pad < 1) {
            pad = 1;
        }
        sb.append(amtLabel).append(spaces(pad)).append(amt).append('\n');
        rule(sb, '=');

        if (notBlank(bill.getComments())) {
            field(sb, "Comment", bill.getComments().trim());
        }

        sb.append('\n');
        String cashier = "";
        if (bill.getCreater() != null && bill.getCreater().getWebUserPerson() != null) {
            cashier = safe(bill.getCreater().getWebUserPerson().getName());
        }
        sb.append(clip("Cashier : " + cashier)).append('\n');

        if (emitEscP) {
            sb.append('\f'); // form feed — advance to next form
        }
        return sb.toString();
    }

    private static void field(StringBuilder sb, String label, String value) {
        String l = label;
        if (l.length() > LABEL_WIDTH) {
            l = l.substring(0, LABEL_WIDTH);
        }
        String prefix = padRight(l, LABEL_WIDTH) + ": ";
        int room = WIDTH - prefix.length();
        String v = value == null ? "" : value;
        if (v.length() <= room) {
            sb.append(prefix).append(v).append('\n');
        } else {
            // wrap continuation lines under the value column
            sb.append(prefix).append(v.substring(0, room)).append('\n');
            String rest = v.substring(room);
            String indent = spaces(prefix.length());
            while (rest.length() > room) {
                sb.append(indent).append(rest.substring(0, room)).append('\n');
                rest = rest.substring(room);
            }
            sb.append(indent).append(rest).append('\n');
        }
    }

    private static void centre(StringBuilder sb, String s) {
        String v = clip(s);
        int lead = (WIDTH - v.length()) / 2;
        if (lead < 0) {
            lead = 0;
        }
        sb.append(spaces(lead)).append(v).append('\n');
    }

    private static void rule(StringBuilder sb, char c) {
        for (int i = 0; i < WIDTH; i++) {
            sb.append(c);
        }
        sb.append('\n');
    }

    private static String clip(String s) {
        String v = s == null ? "" : s;
        return v.length() > WIDTH ? v.substring(0, WIDTH) : v;
    }

    private static String padRight(String s, int n) {
        StringBuilder b = new StringBuilder(s == null ? "" : s);
        while (b.length() < n) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String spaces(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.max(0, n); i++) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

Run: `mvn -q -o test -Dtest=InwardReceiptTextRendererTest`
Expected: PASS (7 tests). If `Person.getSex()` returns an enum whose `toString()` is not `"Female"`, adjust the test's `containsKeyFields` / `Age / Gender` assertion to match the enum's actual label — do not change production formatting for the test.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/core/util/InwardReceiptTextRenderer.java src/test/java/com/divudi/core/util/InwardReceiptTextRendererTest.java
git commit -m "feat(inward): add InwardReceiptTextRenderer for raw-text dot-matrix receipts"
```

---

## Task 6: `streamCurrentDepositReceiptAsRawText()` on `InwardDepositController`

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardDepositController.java` (add one method + imports; `configOptionApplicationController` already injected at line 102, `getCurrent()` returns the `BilledBill`)

**Interfaces:**
- Consumes: `InwardReceiptTextRenderer.render(...)` (Task 5); `configOptionApplicationController.getBooleanValueByKeyForDepartment(...)` / `getLongValueByKeyForDepartment(...)` / `getBooleanValueByKey(...)`; `sessionController.getDepartment()`.
- Produces: `public void streamCurrentDepositReceiptAsRawText()` — no return (writes to the servlet response, calls `responseComplete()`). Bound from `inward_bill_deposit.xhtml` Task 3 button.

- [ ] **Step 1: Add imports** (only those not already present):

```java
import com.divudi.core.util.InwardReceiptTextRenderer;
import java.io.OutputStream;
import java.nio.charset.Charset;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
```

- [ ] **Step 2: Add the method** near the other `getCurrent()`-based actions:

```java
/**
 * Streams the current deposit receipt as a raw byte file (.prn) for
 * dot-matrix printing that bypasses the browser rasteriser. A watched-folder
 * agent on the cashier PC raw-copies the file to the LQ-310. See
 * developer_docs/printing/raw-text-print-agent.ps1 and the wiki page
 * "Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts".
 */
public void streamCurrentDepositReceiptAsRawText() {
    if (getCurrent() == null || getCurrent().getId() == null) {
        JsfUtil.addErrorMessage("No saved deposit to print.");
        return;
    }
    com.divudi.core.entity.Department dept = sessionController.getDepartment();
    boolean preprinted = configOptionApplicationController
            .getBooleanValueByKeyForDepartment("Inward Raw Text Receipt Preprinted Stationery", dept, false);
    int topMargin = configOptionApplicationController
            .getLongValueByKeyForDepartment("Inward Raw Text Receipt Top Margin Lines", dept, 8L).intValue();
    boolean emitEscP = configOptionApplicationController
            .getBooleanValueByKey("Inward Raw Text Receipt Emit ESC/P Codes", true);

    String text = InwardReceiptTextRenderer.render(getCurrent(), "Deposit Receipt",
            false, preprinted, topMargin, emitEscP);

    String fileName = "inward-deposit-"
            + (getCurrent().getDeptId() == null ? String.valueOf(getCurrent().getId())
                    : getCurrent().getDeptId().replaceAll("[^A-Za-z0-9._-]", "_"))
            + ".prn";

    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    try (OutputStream os = response.getOutputStream()) {
        // ISO-8859-1 so ESC/P control bytes (0x1B, 0x0C) pass through unchanged.
        os.write(text.getBytes(Charset.forName("ISO-8859-1")));
        os.flush();
    } catch (java.io.IOException e) {
        JsfUtil.addErrorMessage("Could not generate the raw text receipt: " + e.getMessage());
        return;
    }
    context.responseComplete();
}
```

- [ ] **Step 3: Confirm `getLongValueByKeyForDepartment` signature** — `ConfigOptionApplicationController.java:1725` is `public Long getLongValueByKeyForDepartment(String key, Department dept, Long defaultValue)`. Matches. If `getBooleanValueByKeyForDepartment` (line 1632) has a different arg order, adjust the call.

- [ ] **Step 4: Compile**

Run: `mvn -q -o compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Redeploy + Playwright**

`playwright-e2e`: Deposit page → create a cash deposit → click **Print (Raw Text)**. Confirm a file `inward-deposit-<n>.prn` downloads. Open it: 40-column columns line up, `Paying Amount` right-aligned, header present (or N blank lines if the dept has `Inward Raw Text Receipt Preprinted Stationery = true`). With `Emit ESC/P Codes = true` the first bytes are `1B 40`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardDepositController.java
git commit -m "feat(inward): stream deposit receipt as raw .prn for dot-matrix printing"
```

---

## Task 7: `streamCurrentPaymentReceiptAsRawText()` on `InwardPaymentController`

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/InwardPaymentController.java` (`configOptionApplicationController` injected at line 93; `getCurrent()` at line 1084)

**Interfaces:**
- Consumes: same as Task 6.
- Produces: `public void streamCurrentPaymentReceiptAsRawText()`. Bound from `inward_bill_payment.xhtml` Task 4 button.

- [ ] **Step 1: Add the same imports** as Task 6 Step 1 (skip any already present).

- [ ] **Step 2: Add the method** (identical to Task 6 Step 2 except heading, filename prefix, and guard message):

```java
/**
 * Streams the current payment receipt as a raw byte file (.prn) for
 * dot-matrix printing that bypasses the browser rasteriser. See
 * developer_docs/printing/raw-text-print-agent.ps1.
 */
public void streamCurrentPaymentReceiptAsRawText() {
    if (getCurrent() == null || getCurrent().getId() == null) {
        JsfUtil.addErrorMessage("No saved payment to print.");
        return;
    }
    com.divudi.core.entity.Department dept = sessionController.getDepartment();
    boolean preprinted = configOptionApplicationController
            .getBooleanValueByKeyForDepartment("Inward Raw Text Receipt Preprinted Stationery", dept, false);
    int topMargin = configOptionApplicationController
            .getLongValueByKeyForDepartment("Inward Raw Text Receipt Top Margin Lines", dept, 8L).intValue();
    boolean emitEscP = configOptionApplicationController
            .getBooleanValueByKey("Inward Raw Text Receipt Emit ESC/P Codes", true);

    String text = InwardReceiptTextRenderer.render(getCurrent(), "Payment Receipt",
            false, preprinted, topMargin, emitEscP);

    String fileName = "inward-payment-"
            + (getCurrent().getDeptId() == null ? String.valueOf(getCurrent().getId())
                    : getCurrent().getDeptId().replaceAll("[^A-Za-z0-9._-]", "_"))
            + ".prn";

    FacesContext context = FacesContext.getCurrentInstance();
    HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    try (java.io.OutputStream os = response.getOutputStream()) {
        os.write(text.getBytes(java.nio.charset.Charset.forName("ISO-8859-1")));
        os.flush();
    } catch (java.io.IOException e) {
        JsfUtil.addErrorMessage("Could not generate the raw text receipt: " + e.getMessage());
        return;
    }
    context.responseComplete();
}
```

- [ ] **Step 3: Verify `sessionController` is injected** in `InwardPaymentController`. Run: `grep -n "SessionController sessionController" src/main/java/com/divudi/bean/inward/InwardPaymentController.java`. If absent, add `@Inject private SessionController sessionController;` (import `com.divudi.bean.common.SessionController`).

- [ ] **Step 4: Compile**

Run: `mvn -q -o compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Redeploy + Playwright**

`playwright-e2e`: Payment page → make a payment → **Print (Raw Text)** → `inward-payment-<n>.prn` downloads with heading "Payment Receipt".

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardPaymentController.java
git commit -m "feat(inward): stream payment receipt as raw .prn for dot-matrix printing"
```

---

## Task 8: Reprint pages — buttons + reprint raw-text action

**Files:**
- First establish which bean backs `#{inwardSearch.bill}` on the reprint pages. Run:
  `grep -n "inwardSearch" src/main/webapp/inward/inward_reprint_bill_deposit.xhtml | head` and
  `grep -rn "class InwardSearch" src/main/java/`.
- Modify: `src/main/webapp/inward/inward_reprint_bill_deposit.xhtml` (buttons ~line 45-50; preview `gpBillPreview` ~line 166-183)
- Modify: `src/main/webapp/inward/inward_reprint_bill_payment.xhtml` (buttons ~line 45-50; preview ~line 184-199)
- Modify: `src/main/java/com/divudi/bean/inward/InwardSearch.java` (add `streamReprintReceiptAsRawText()`)

**Interfaces:**
- Consumes: `InwardReceiptTextRenderer.render(...)`; `#{inwardSearch.bill}`.
- Produces: `public void streamReprintReceiptAsRawText()` on `InwardSearch` — uses `getBill()`, always `duplicate = true`, heading chosen from `getBill().getBillTypeAtomic()` (Deposit vs Payment) or a simple `"Receipt"` fallback.

- [ ] **Step 1: Add `streamReprintReceiptAsRawText()` to `InwardSearch`**

```java
/**
 * Streams the selected (reprint) inward receipt as a raw .prn for dot-matrix
 * printing. Always a duplicate. Heading derived from the bill type.
 */
public void streamReprintReceiptAsRawText() {
    if (getBill() == null || getBill().getId() == null) {
        JsfUtil.addErrorMessage("Select a bill to reprint first.");
        return;
    }
    com.divudi.core.entity.Department dept = getBill().getDepartment();
    boolean preprinted = configOptionApplicationController
            .getBooleanValueByKeyForDepartment("Inward Raw Text Receipt Preprinted Stationery", dept, false);
    int topMargin = configOptionApplicationController
            .getLongValueByKeyForDepartment("Inward Raw Text Receipt Top Margin Lines", dept, 8L).intValue();
    boolean emitEscP = configOptionApplicationController
            .getBooleanValueByKey("Inward Raw Text Receipt Emit ESC/P Codes", true);

    String heading = "Receipt";
    if (getBill().getBillTypeAtomic() != null) {
        String n = getBill().getBillTypeAtomic().name();
        if (n.contains("DEPOSIT")) {
            heading = "Deposit Receipt";
        } else if (n.contains("PAYMENT")) {
            heading = "Payment Receipt";
        }
    }

    String text = com.divudi.core.util.InwardReceiptTextRenderer.render(getBill(), heading,
            true, preprinted, topMargin, emitEscP);

    String fileName = "inward-reprint-"
            + (getBill().getDeptId() == null ? String.valueOf(getBill().getId())
                    : getBill().getDeptId().replaceAll("[^A-Za-z0-9._-]", "_"))
            + ".prn";

    javax.faces.context.FacesContext context = javax.faces.context.FacesContext.getCurrentInstance();
    javax.servlet.http.HttpServletResponse response =
            (javax.servlet.http.HttpServletResponse) context.getExternalContext().getResponse();
    response.setContentType("application/octet-stream");
    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
    try (java.io.OutputStream os = response.getOutputStream()) {
        os.write(text.getBytes(java.nio.charset.Charset.forName("ISO-8859-1")));
        os.flush();
    } catch (java.io.IOException e) {
        JsfUtil.addErrorMessage("Could not generate the raw text receipt: " + e.getMessage());
        return;
    }
    context.responseComplete();
}
```

If `InwardSearch` does not already inject `ConfigOptionApplicationController`, add
`@Inject private ConfigOptionApplicationController configOptionApplicationController;`
(import `com.divudi.bean.common.ConfigOptionApplicationController`). Confirm `JsfUtil` is imported.

- [ ] **Step 2: Compile**

Run: `mvn -q -o compile`
Expected: BUILD SUCCESS.

- [ ] **Step 3: `inward_reprint_bill_deposit.xhtml` — add buttons** after the existing `Print` button (`<p:printer target="gpBillPreview">`):

```xml
<p:commandButton value="Print (Dot-Matrix)" ajax="false" icon="fa fa-print"
                 class="ui-button-info" action="#" >
    <p:printer target="gpBillPreviewDmx" ></p:printer>
</p:commandButton>
<p:commandButton value="Print (Raw Text)" ajax="false" icon="fa fa-file-lines"
                 class="ui-button-help"
                 action="#{inwardSearch.streamReprintReceiptAsRawText}" />
```

- [ ] **Step 4: `inward_reprint_bill_deposit.xhtml` — add hidden preview** after the `gpBillPreview` group closes:

```xml
<h:panelGroup id="gpBillPreviewDmx" style="display:none;">
    <bill:FiveFiveDotMatrixPaymentBill bill="#{inwardSearch.bill}" heading="Deposit Receipt" duplicate="true" />
</h:panelGroup>
```

Confirm the `bill:` namespace is declared in this file (it uses `bill:FiveFivePaymentBill` already, so yes).

- [ ] **Step 5: `inward_reprint_bill_payment.xhtml` — same buttons** (identical markup to Step 3).

- [ ] **Step 6: `inward_reprint_bill_payment.xhtml` — hidden preview** with `heading="Payment Receipt"`:

```xml
<h:panelGroup id="gpBillPreviewDmx" style="display:none;">
    <bill:FiveFiveDotMatrixPaymentBill bill="#{inwardSearch.bill}" heading="Payment Receipt" duplicate="true" />
</h:panelGroup>
```

- [ ] **Step 7: Redeploy + Playwright**

`playwright-e2e`: menu Inpatient → Inward → Reprint Deposit (and Reprint Payment). Search a bill, open it. Confirm the 3 print buttons; **Print (Dot-Matrix)** shows the monospace duplicate; **Print (Raw Text)** downloads `inward-reprint-<n>.prn`.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/InwardSearch.java src/main/webapp/inward/inward_reprint_bill_deposit.xhtml src/main/webapp/inward/inward_reprint_bill_payment.xhtml
git commit -m "feat(inward): add Dot-Matrix and Raw Text print to inward deposit/payment reprint pages"
```

---

## Task 9: Optional — expose the Dot-Matrix toggle in the Settings dialog

**Files:**
- Modify: `src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java` (add field + load at ~line 388 + save at ~line 856)
- Modify: `src/main/webapp/inward/inward_bill_deposit.xhtml` + `inward_bill_payment.xhtml` (`inwardPaymentConfigForm` card, after the A4 checkbox ~line 507)

**Interfaces:**
- Consumes: `configOptionController` (already in `PharmacyConfigController`).
- Produces: `pharmacyConfigController.inwardPaymentDotMatrixPaper` getter/setter.

- [ ] **Step 1: Add the field + accessors** to `PharmacyConfigController` alongside `inwardPaymentA4Paper`:

```java
private boolean inwardPaymentDotMatrixPaper;

public boolean isInwardPaymentDotMatrixPaper() { return inwardPaymentDotMatrixPaper; }
public void setInwardPaymentDotMatrixPaper(boolean v) { this.inwardPaymentDotMatrixPaper = v; }
```

- [ ] **Step 2: Load it** in `loadCurrentConfig()` next to line 388:

```java
inwardPaymentDotMatrixPaper = configOptionController.getBooleanValueByKey("Inward Payment Bill Dot Matrix Paper", false);
```

- [ ] **Step 3: Save it** in `saveInwardPaymentConfig()` next to line 856:

```java
configOptionController.setBooleanValueByKey("Inward Payment Bill Dot Matrix Paper", inwardPaymentDotMatrixPaper);
```

- [ ] **Step 4: Add the checkbox** to both pages' `inwardPaymentConfigForm`, after the A4 `div.mb-3`:

```xml
<div class="mb-3">
    <h:selectBooleanCheckbox id="inwardPaymentDotMatrix" value="#{pharmacyConfigController.inwardPaymentDotMatrixPaper}"/>
    <h:outputLabel for="inwardPaymentDotMatrix" value="Dot-Matrix Paper (LQ-310)" class="ms-2"/>
</div>
```

- [ ] **Step 5: Make `gpBillPreviewDmx` the default preview when the toggle is on.** In both `inward_bill_deposit.xhtml` and `inward_bill_payment.xhtml`, change the `gpBillPreviewDmx` group so it is visible (not `display:none`) when the config is on, mirroring the other preview groups:

```xml
<h:panelGroup id="gpBillPreviewDmx"
              style="#{configOptionController.getBooleanValueByKey('Inward Payment Bill Dot Matrix Paper', false) ? '' : 'display:none;'}">
    <div class="d-flex justify-content-center">
        <bill:FiveFiveDotMatrixPaymentBill bill="#{inwardDepositController.current}" heading="Deposit Receipt" />
    </div>
</h:panelGroup>
```

(Use `inwardPaymentController.current` / `heading="Payment Receipt"` in the payment page.)

- [ ] **Step 6: Compile + redeploy + Playwright**

Run: `mvn -q -o compile` → redeploy. `playwright-e2e`: open **Settings** on the Deposit page, tick "Dot-Matrix Paper (LQ-310)", Apply & Close. Re-open a deposit preview: the monospace receipt is now the on-screen preview and the plain **Print** button prints it too.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/divudi/bean/pharmacy/PharmacyConfigController.java src/main/webapp/inward/inward_bill_deposit.xhtml src/main/webapp/inward/inward_bill_payment.xhtml
git commit -m "feat(inward): expose Dot-Matrix paper toggle in inward payment/deposit printer settings"
```

---

## Task 10: Raw-text print agent script

**Files:**
- Create: `developer_docs/printing/raw-text-print-agent.ps1`

**Interfaces:**
- Consumes: nothing (standalone). Reads `.prn` files, writes raw bytes to a printer share / LPT port.
- Produces: nothing consumed by code.

- [ ] **Step 1: Write the script**

```powershell
<#
  raw-text-print-agent.ps1
  Watches a folder for inward-*.prn files and raw-copies each to a dot-matrix
  printer (bypassing the Windows print driver / rasteriser), then deletes it.

  Setup on the cashier PC:
    1. Share the Epson LQ-310, e.g. share name "LQ310", OR note its LPT port.
    2. Edit the three settings below.
    3. Set Chrome: Settings > Downloads > Location = the watch folder, and turn
       OFF "Ask where to save each file".
    4. Task Scheduler > Create Task > Trigger "At log on" > Action:
         powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden `
           -File "C:\hmis-print\raw-text-print-agent.ps1"
#>

# ---- settings -------------------------------------------------------------
$WatchFolder = "C:\hmis-print"          # where the browser saves .prn files
$PrinterPath = "\\localhost\LQ310"      # printer share  (or "\\.\LPT1")
$FileGlob    = "inward-*.prn"           # only touch our receipts
# ------------------------------------------------------------------------------

if (-not (Test-Path $WatchFolder)) { New-Item -ItemType Directory -Path $WatchFolder | Out-Null }

function Send-Raw($file) {
    for ($i = 0; $i -lt 5; $i++) {
        try {
            # /b = binary copy, no EOF translation, no driver involvement
            cmd /c copy /b "`"$file`"" "$PrinterPath" | Out-Null
            Remove-Item -LiteralPath $file -Force
            Write-Host ("{0}  printed {1}" -f (Get-Date), (Split-Path $file -Leaf))
            return
        } catch {
            Start-Sleep -Milliseconds 400   # file may still be locked by the browser
        }
    }
    Write-Warning ("Could not print {0} after retries" -f $file)
}

# print anything already waiting
Get-ChildItem -Path $WatchFolder -Filter $FileGlob -File -ErrorAction SilentlyContinue |
    ForEach-Object { Send-Raw $_.FullName }

$fsw = New-Object System.IO.FileSystemWatcher $WatchFolder, $FileGlob
$fsw.EnableRaisingEvents = $true
Register-ObjectEvent $fsw Created -Action {
    Start-Sleep -Milliseconds 500          # let the download finish
    Send-Raw $Event.SourceEventArgs.FullPath
} | Out-Null

Write-Host ("{0}  watching {1} for {2} -> {3}" -f (Get-Date), $WatchFolder, $FileGlob, $PrinterPath)
while ($true) { Start-Sleep -Seconds 3600 }
```

- [ ] **Step 2: Lint**

Run: `pwsh -NoProfile -Command "Invoke-ScriptAnalyzer -Path developer_docs/printing/raw-text-print-agent.ps1 -Severity Warning" 2>/dev/null || echo "PSScriptAnalyzer not installed - skip"`
Expected: no errors (warnings acceptable, or tool absent).

- [ ] **Step 3: Commit**

```bash
git add developer_docs/printing/raw-text-print-agent.ps1
git commit -m "docs(printing): add watched-folder raw-text print agent for dot-matrix receipts"
```

---

## Task 11: Wiki page

**Files:**
- Create: `hmis.wiki/Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts.md` (the wiki is the `../hmis.wiki` sibling working directory — `C:\Development\hmis.wiki`)

**Interfaces:**
- Consumes: nothing.
- Produces: linked from the PR body.

- [ ] **Step 1: Write the page**

```markdown
# Dot-Matrix Printing for Inward Deposit & Payment Receipts

Coop and other sites print inward **deposit** and **payment** receipts on 5×5
continuous stationery using an **Epson LQ-310** impact printer. The normal
browser print path rasterises the page to a bitmap, which prints mushy and
unreadable on an impact head. This page explains the two print options added to
fix that and how to set up the client machine.

## The three print buttons

On the deposit / payment print-preview screen (and their reprint screens):

| Button | What it does | Use when |
|---|---|---|
| **Print** | The existing paper format (POS / 5×5 / A4 as configured). | Laser / inkjet / POS printers. |
| **Print (Dot-Matrix)** | A clean monospace receipt, printed through the normal browser dialog. | LQ-310 with the ESC/P driver set to a native font (see below). Try this first. |
| **Print (Raw Text)** | Downloads a `.prn` file of the receipt as raw text. A small agent on the PC sends it straight to the printer, bypassing the driver. | If **Print (Dot-Matrix)** is still not crisp enough. Matches how standalone billing apps print. |

## Track A — Print (Dot-Matrix): printer & Chrome settings

1. Install the printer with the **Epson LQ-310 ESC/P2** driver (Windows "Add
   Printer" → Epson → LQ-310). Do not use a generic / text-only driver here.
2. **Printer Properties → Printing Preferences → Advanced**:
   - Print Quality = **Draft** or **LQ (Near Letter Quality)** — not "Photo" / "Best".
   - **"Print Text as Graphics" = OFF** (label varies: "Send TrueType as Bitmap =
     No", "Print Mode = Native", "Graphics = Draft").
   - Paper size = custom **5 in × 5 in** (or your form's real length).
   - Paper source = **Tractor / Continuous**.
3. In the **Chrome print dialog** for this printer:
   - Margins = **None**
   - Scale = **100 / Default**
   - **Headers and footers = OFF**
   - **Background graphics = OFF**
   - Set it as the default destination and click **Save**.
4. If the receipt prints over your pre-printed letterhead, set the ConfigOption
   **`Inward Dot Matrix Receipt Preprinted Stationery` = true** (per department)
   and tune **`Inward Dot Matrix Receipt Top Margin Lines`** (default 8) until the
   body clears the pre-printed logo.

## Track B — Print (Raw Text): client-machine setup

The `.prn` file must reach the LQ-310 as **raw bytes**. Pick one option.

### Option 1 — Manual (quick test, no automation)

1. Share the printer (share name e.g. `LQ310`) or note its LPT port.
2. After clicking **Print (Raw Text)**, from a Command Prompt:
   ```
   copy /b "%USERPROFILE%\Downloads\inward-deposit-123.prn" \\localhost\LQ310
   ```
   (or `... \\.\LPT1`). It prints immediately, crisp, with no driver
   rasterisation. Use this to confirm quality before setting up the agent.

### Option 2 — Watched-folder agent (recommended for daily use)

1. Copy `raw-text-print-agent.ps1` (from the repo,
   `developer_docs/printing/raw-text-print-agent.ps1`) to `C:\hmis-print\`.
2. Edit the three settings at the top: `$WatchFolder`, `$PrinterPath`
   (`\\localhost\LQ310` or `\\.\LPT1`), `$FileGlob`.
3. In **Chrome → Settings → Downloads**: set **Location** to `C:\hmis-print\`
   and turn **OFF** "Ask where to save each file".
4. **Task Scheduler → Create Task**:
   - General: "Run whether user is logged on or not" is *not* needed — use
     "Run only when user is logged on".
   - Triggers: **At log on** (of the cashier user).
   - Actions: Start a program —
     `powershell.exe`
     arguments:
     `-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "C:\hmis-print\raw-text-print-agent.ps1"`
5. Log off / on. Now **Print (Raw Text)** → file lands in `C:\hmis-print\` →
   agent prints it → file is deleted. Fully hands-off.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Control codes (`←@`, `←x`) printed literally on the page | The transport is stripping / re-encoding 8-bit bytes. Set ConfigOption **`Inward Raw Text Receipt Emit ESC/P Codes` = false** for pure ASCII. |
| Nothing prints, no error | Printer share name / LPT path wrong in the script. Test with the Option 1 `copy /b` command first. |
| Prints half the receipt then ejects the whole form | Form length in the driver ≠ your stationery. Set the custom paper size to the real form length; the raw path emits one form-feed at the end. |
| Two copies of the receipt on screen | The dot-matrix preview is meant to be hidden until you print. If it shows, the `Inward Payment Bill Dot Matrix Paper` config is on — that is expected (it makes the monospace receipt the default preview). |
| Body still overlaps pre-printed header | Increase `Inward Dot Matrix Receipt Top Margin Lines` (Dot-Matrix button) or `Inward Raw Text Receipt Top Margin Lines` (Raw Text button). |

## ConfigOption reference

| Key | Type | Default | Scope | Effect |
|---|---|---|---|---|
| `Inward Payment Bill Dot Matrix Paper` | bool | false | global/dept | Makes the monospace receipt the default on-screen preview + plain Print target. |
| `Inward Dot Matrix Receipt Preprinted Stationery` | bool | false | dept-first | Suppress the app's text header; drop the body below pre-printed letterhead. |
| `Inward Dot Matrix Receipt Top Margin Lines` | int | 8 | dept-first | Blank leading lines when the above is true. |
| `Print Barcode on Inward Dot Matrix Receipt` | bool | false | global/dept | Include the (bitmap) barcode. Off for LQ. |
| `Inward Raw Text Receipt Preprinted Stationery` | bool | false | dept-first | Same as its Dot-Matrix twin, for the Raw Text file. |
| `Inward Raw Text Receipt Top Margin Lines` | int | 8 | dept-first | Blank leading lines in the Raw Text file. |
| `Inward Raw Text Receipt Emit ESC/P Codes` | bool | true | global/dept | Wrap the file in ESC/P init + form-feed. Turn off if the transport mangles control bytes. |
```

- [ ] **Step 2: Commit (in the wiki working dir)**

```bash
cd C:/Development/hmis.wiki && git add Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts.md && git commit -m "docs: dot-matrix / raw-text printing guide for inward deposit & payment receipts"
```

(Push the wiki separately; it is a different remote. Note the page name in the PR.)

---

## Task 12: Full-branch review, spec sync, PR

**Files:** none new — this is verification + PR.

- [ ] **Step 1: Whole-branch self-review.** Re-read the full diff against `origin/development`. Check specifically (per the "whole-branch review catches untested paths" learning):
  - the `Print (Raw Text)` guard path (no saved bill) shows an error, does not NPE;
  - `getLongValueByKeyForDepartment(...).intValue()` cannot NPE — it has a non-null default;
  - the dot-matrix preview group is `display:none` unless the config toggle is on (Task 9);
  - no existing component `cc:interface` was changed;
  - `ISO-8859-1` encoding is used for the byte stream (so `0x1B`/`0x0C` survive).

- [ ] **Step 2: Run the unit tests once more**

Run: `mvn -q -o test -Dtest=InwardReceiptTextRendererTest`
Expected: PASS.

- [ ] **Step 3: Playwright regression pass** — per `playwright-e2e`, on a real deployment:
  - Deposit: create → all 3 print buttons work; existing **Print** (POS) unchanged.
  - Payment: same.
  - Reprint Deposit + Reprint Payment: open a historic bill → all 3 buttons work.
  - Record every menu path used, in the PR body.

- [ ] **Step 4: Update the spec status.** Edit
  `developer_docs/specs/2026-09-10-inward-deposit-payment-dot-matrix-print-design.md`
  header `Status:` → `Implemented (pending Coop acceptance)`. Commit.

- [ ] **Step 5: Restore CI placeholders in persistence.xml, push, restore local JNDI**

```bash
# swap jdbc/coop -> ${JDBC_DATASOURCE}, jdbc/ruhunuAudit -> ${JDBC_AUDIT_DATASOURCE}
git add src/main/resources/META-INF/persistence.xml
git commit -m "chore: persistence.xml CI placeholders for push"
git push -u origin inward-deposit-payment-dot-matrix-print
# then immediately restore jdbc/coop + jdbc/ruhunuAudit locally, leave UNSTAGED
```

- [ ] **Step 6: Open the PR** targeting `development`:

```bash
gh pr create --base development --repo hmislk/hmis \
  --title "Inward deposit & payment: dot-matrix / raw-text print for Epson LQ-310" \
  --body "$(cat <<'EOF'
## What

Two new print buttons on the inward **Deposit** and **Payment** receipts (live +
reprint):

- **Print (Dot-Matrix)** — new monospace `FiveFiveDotMatrixPaymentBill` component
  + scoped `five_five_dotmatrix.css`: one fixed-width font, pt sizing, literal
  character rules, no `text-transform`, fixed 40-column grid. ConfigOption to
  suppress the app header and drop the body below pre-printed stationery.
- **Print (Raw Text)** — streams the receipt as an `application/octet-stream`
  `.prn` (plain text, optional ESC/P codes) via `InwardReceiptTextRenderer`. A
  watched-folder PowerShell agent on the cashier PC raw-copies it to the LQ-310.

Existing paper formats (POS / 5×5 / A4 / Custom 3) are untouched. All new
ConfigOptions default to current behaviour.

## Why

Coop's LQ-310 output for these receipts is illegible — the browser rasterises
proportional fonts to a bitmap that an impact head cannot resolve. A competitor's
standalone app prints the same data crisply because it sends raw ESC/P text.

Design doc: `developer_docs/specs/2026-09-10-inward-deposit-payment-dot-matrix-print-design.md`

## Client-machine setup

Wiki page **Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts** — driver
+ Chrome settings for Track A; watched-folder agent
(`developer_docs/printing/raw-text-print-agent.ps1`) + Task Scheduler + ESC/P
troubleshooting for Track B.

## Testing

- Unit: `InwardReceiptTextRendererTest` (40-col width, key fields, header
  suppression + top margin, duplicate marker, ESC/P prologue/form-feed).
- Playwright: Deposit + Payment + both reprint pages — all 3 print buttons;
  existing Print unchanged. Menu paths:
  - Menu → Inpatient → Inward → Deposit
  - Menu → Inpatient → Inward → Payment
  - Menu → Inpatient → Inward → Reprint Deposit / Reprint Payment

## Rollout

Coop enables `Inward Dot Matrix Receipt Preprinted Stationery = true` per
department and tunes `Top Margin Lines`. Tests Track A first; escalates to Track B
if not crisp enough.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_0159PRDiWz158pPE29cpQuUW
EOF
)"
```

- [ ] **Step 7: Drive CI to green.** Watch the PR checks; fix any failure (compile, migration validation, branch-name). Re-push (repeat the persistence.xml swap/restore dance each push).

---

## Self-Review

**1. Spec coverage**

| Spec section | Task |
|---|---|
| §2 two new buttons, Deposit + Payment, live + reprint | Tasks 3, 4, 8 |
| §4 `FiveFiveDotMatrixPaymentBill` component | Task 2 |
| §4 `five_five_dotmatrix.css` scoped, print + screen | Task 1 |
| §4 one font / pt / no text-transform / char rules / 40-col | Tasks 1, 2 |
| §4 field list incl. MultiplePaymentMethods, comment guard, footer | Task 2 |
| §4 barcode off by default via ConfigOption | Task 2 |
| §4 pre-printed stationery: suppress header + N blank lines | Tasks 2 (Dot-Matrix), 5 (Raw Text renderer), 6/7/8 (wire) |
| §4 all 4 Track A ConfigOptions, exact keys | Global Constraints + Tasks 2, 9 |
| §4 driver + Chrome config note | Task 11 wiki |
| §5 server streams `.prn`, `application/octet-stream`, `responseComplete()` | Tasks 6, 7, 8 |
| §5 text builder, ESC/P optional, header/margin per ConfigOption | Task 5 |
| §5 3 Track B ConfigOptions, exact keys | Global Constraints + Tasks 6, 7, 8 |
| §5 raw-print agent script + 3 client options + Task Scheduler + troubleshooting | Tasks 10, 11 |
| §6 wiki page + `raw-text-print-agent.ps1` committed | Tasks 10, 11 |
| §6 PR links the wiki | Task 12 Step 6 |
| §7 unit tests for the renderer | Task 5 |
| §7 Playwright end-to-end both pages + reprint | Tasks 3–8, 12 |
| §8 all ConfigOptions default to today's behaviour | Global Constraints; every `getBooleanValueByKey(..., false)` / `..., true` for ESC/P |
| §8 branch from `origin/development`, PR to `development` | Global Constraints, Task 12 |

No gaps.

**2. Placeholder scan** — no "TBD" / "handle edge cases" / "similar to Task N" / bare "write tests". Every code step has real code. Task 8 and Task 9's "confirm which bean / confirm injection" steps are explicit `grep` verifications with a stated fallback, not placeholders.

**3. Type consistency**

- `InwardReceiptTextRenderer.render(Bill, String, boolean, boolean, int, boolean)` and `WIDTH` — defined Task 5, called identically in Tasks 6, 7, 8.
- `integerList(Integer)` — defined Task 2 Step 2, used Task 2 Step 1 markup.
- `streamCurrentDepositReceiptAsRawText()` / `streamCurrentPaymentReceiptAsRawText()` / `streamReprintReceiptAsRawText()` — defined Tasks 6/7/8, bound in Tasks 3/4/8 markup with matching names.
- `gpBillPreviewDmx` — same id in Tasks 3, 4, 8, 9.
- ConfigOption keys — one verbatim list in Global Constraints; every task quotes from it.
- `getLongValueByKeyForDepartment(String, Department, Long)` / `getBooleanValueByKeyForDepartment(String, Department, boolean)` — verified against `ConfigOptionApplicationController.java:1725` / `:1632` in Task 6 Step 3.

Consistent.
