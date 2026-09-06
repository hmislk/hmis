# Baby Admission: Independent Discharge + Interim Bill Guard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Block interim/estimated bill generation directly against a baby (child) admission, keep physical discharge reachable for babies once that guard is in place, and add a visual cue distinguishing a baby's dashboard from its mother's.

**Architecture:** Three small, independent changes to existing controller methods and one XHTML page — no new entities, no schema changes, no new abstractions. Discharge independence (clinical/room/nursing) already works today and needs no code change (verified in investigation, re-verified via Playwright after this plan).

**Tech Stack:** Java EE 8 / Jakarta, JSF 2.3 + PrimeFaces, CDI `@SessionScoped` beans, EclipseLink JPA, Maven, Payara.

## Global Constraints

- JPQL first — no native SQL needed for this change (no new queries).
- Never modify existing constructors — not applicable, no entity/DTO constructor changes in this plan.
- Follow the existing audit-logging convention (before/after state maps via `auditService.logEncounterAudit`) — Task 2 preserves the existing call, no new audit call needed since the guard change only affects a precondition, not what gets persisted.
- No automated JUnit test harness exists for these `@SessionScoped` inward controllers in this codebase (confirmed: `src/test/java` has 23 files, none touching `BhtSummeryController`/`NursingDischargeController`/inward discharge flows). Per-task verification is `mvn compile` for fast feedback; full behavioral verification is a single Playwright + DB pass after all 3 tasks are implemented (dev-issue skill step 7), not a per-task browser cycle — redeploying Payara per task would be wasteful and the codebase has no faster feedback loop for this bean layer.

---

### Task 1: Guard interim/estimated bill generation against baby (child) encounters

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java:3133-3148` (`createIntrimBillTable()`)
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java:3186-3224` (`createTablesWithEstimatedProfessionalFees()`)
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java:3396-3400` (`navigateToIntrimBillFromPatientProfile()`)

**Interfaces:**
- Consumes: `PatientEncounter.getParentEncounter()` (existing getter, `src/main/java/com/divudi/core/entity/PatientEncounter.java:171`, returns `PatientEncounter`, `null` for a top-level/mother admission, non-null for a baby/child admission).
- Produces: no new public methods — the three existing methods keep their exact signatures (`public String createIntrimBillTable()`, `public void createTablesWithEstimatedProfessionalFees()`, `public String navigateToIntrimBillFromPatientProfile()`), just gain an early-return guard. Nothing downstream needs new interfaces.

This closes the three real gaps found during investigation:
1. `createIntrimBillTable()` — reached by staff picking a baby directly on the Interim Bill search page's autocomplete (`inward_bill_intrim.xhtml:181`).
2. `createTablesWithEstimatedProfessionalFees()` — reached the same way from the Estimated Bill search page (`inward_bill_intrim_estimate.xhtml:66,100,206`).
3. `navigateToIntrimBillFromPatientProfile()` — reached from the Room Details page's unconditional "Interim Bill" button (`inward_patient_room_details.xhtml:67-72`), which has no `parentEncounter` check of its own.

(The dashboard's own Interim Bill/Estimated Bill buttons are already unreachable for babies — the whole "Billing" panel is hidden via `rendered="#{admissionController.current.parentEncounter == null and ...}"` at `admission_profile.xhtml:300` — so this task is defense-in-depth for that path and the real fix for the three above.)

- [ ] **Step 1: Add the guard to `createIntrimBillTable()`**

Current code (`BhtSummeryController.java:3133-3148`):

```java
    public String createIntrimBillTable() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Restrict Access to Intrim Bill if Provisional Bill is Created")) {
            if (admissionController.isAddmissionHaveProvisionalBill((Admission) patientEncounter)) {
                JsfUtil.addErrorMessage("There is a Provisional Bill For This Admission");
                clear();
                return "";
            }
        }
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }
```

Replace with:

```java
    public String createIntrimBillTable() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No Admission Selected");
            return "";
        }
        if (patientEncounter.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Interim bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            clear();
            return "";
        }
        if (configOptionApplicationController.getBooleanValueByKey("Restrict Access to Intrim Bill if Provisional Bill is Created")) {
            if (admissionController.isAddmissionHaveProvisionalBill((Admission) patientEncounter)) {
                JsfUtil.addErrorMessage("There is a Provisional Bill For This Admission");
                clear();
                return "";
            }
        }
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }
```

- [ ] **Step 2: Add the guard to `createTablesWithEstimatedProfessionalFees()`**

Current code (`BhtSummeryController.java:3186-3224`, first 11 lines shown, rest unchanged):

```java
    public void createTablesWithEstimatedProfessionalFees() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        makeNull();
        estimatedBillView = true;

        if (patientEncounter == null) {
            return;
        }

        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
```

Replace the `if (patientEncounter == null) { return; }` block with:

```java
        if (patientEncounter == null) {
            return;
        }
        if (patientEncounter.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Estimated bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            clear();
            return;
        }

        if (childPatientEncouters == null || childPatientEncouters.isEmpty()) {
```

(everything else in the method is unchanged).

- [ ] **Step 3: Add the guard to `navigateToIntrimBillFromPatientProfile()`**

Current code (`BhtSummeryController.java:3396-3400`):

```java
    public String navigateToIntrimBillFromPatientProfile() {
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }
```

Replace with:

```java
    public String navigateToIntrimBillFromPatientProfile() {
        if (patientEncounter != null && patientEncounter.getParentEncounter() != null) {
            JsfUtil.addErrorMessage("Interim bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            clear();
            return "";
        }
        childPatientEncouters = null;
        createTables();
        return "/inward/inward_bill_intrim?faces-redirect=true";
    }
```

- [ ] **Step 4: Compile to verify no syntax/type errors**

Run: `mvn -q -pl . compile`
Expected: `BUILD SUCCESS`, no errors referencing `BhtSummeryController.java`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java
git commit -m "$(cat <<'EOF'
fix(inward): block interim/estimated bill generation against baby encounters

Adds a parent-only guard to the three entry points that can generate an
interim/estimated bill directly against a baby (child) admission,
bypassing the dashboard's already-hidden billing panel: the Interim
Bill search page, the Estimated Bill search page, and the Room
Details page's unconditional Interim Bill button. Charge aggregation
from child encounters into the parent's bill is unchanged.

Refs #22294
EOF
)"
```

---

### Task 2: Let a baby inherit its parent's administrative discharge for physical discharge

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/NursingDischargeController.java:271-274`

**Interfaces:**
- Consumes: `PatientEncounter.getDischarged()` (existing getter, returns `Boolean`, `PatientEncounter.java:740`), `PatientEncounter.getParentEncounter()` (same as Task 1).
- Produces: no signature change to `confirmPhysicalDischarge()` (`public void confirmPhysicalDischarge()`), only its internal precondition changes.

Without this fix, once Task 1 ships, a baby's own `discharged` flag can never become `true` (it's only ever set via the now-parent-only final-billing flow in `BhtSummeryController.discharge()`), so a baby could never pass this precondition and would be permanently stuck before physical discharge.

- [ ] **Step 1: Update the administrative-discharge precondition**

Current code (`NursingDischargeController.java:258-274`):

```java
    public void confirmPhysicalDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Physical discharge already confirmed.");
            return;
        }
        if (!currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: nursing discharge has not been completed.");
            return;
        }
        if (!Boolean.TRUE.equals(currentEncounter.getDischarged())) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: administrative discharge (final bill) has not been completed.");
            return;
        }
```

Replace the last `if` block with:

```java
        boolean administrativelyDischarged = Boolean.TRUE.equals(currentEncounter.getDischarged())
                || (currentEncounter.getParentEncounter() != null
                    && Boolean.TRUE.equals(currentEncounter.getParentEncounter().getDischarged()));
        if (!administrativelyDischarged) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: administrative discharge (final bill) has not been completed.");
            return;
        }
```

(the rest of the method — audit logging, setting `physicalDischarged`, etc. — is unchanged).

- [ ] **Step 2: Compile to verify no syntax/type errors**

Run: `mvn -q -pl . compile`
Expected: `BUILD SUCCESS`, no errors referencing `NursingDischargeController.java`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/NursingDischargeController.java
git commit -m "$(cat <<'EOF'
fix(inward): let baby inherit parent's administrative discharge

A baby's own \`discharged\` flag can only ever be set via the
final-billing flow, which is now parent-only (previous commit). Without
this change, a baby could never pass confirmPhysicalDischarge()'s
administrative-discharge precondition. Mirrors the existing
nursingDischarged-inheritance pattern already used for
ClinicalAssessment records.

Refs #22294
EOF
)"
```

---

### Task 3: Add a "Baby Admission" badge to the Inpatient Dashboard header

**Files:**
- Modify: `src/main/webapp/inward/admission_profile.xhtml:17-45` (header facet)

**Interfaces:**
- Consumes: `admissionController.current.parentEncounter` (EL binding, non-null when the currently-loaded admission is a baby), `admissionController.current.parentEncounter.patient.person.nameWithTitle` (existing EL path already used elsewhere in this file, e.g. `admission_profile.xhtml:290` for baby names in the "Baby Admissions" panel), `admissionController.current.parentEncounter.bhtNo` (existing property, used at `admission_profile.xhtml:291` pattern).
- Produces: no new controller methods or bean properties — pure EL/markup addition.

This is a JSF-only (XHTML-only) change — per project convention it does not require a Java compile step, only a redeploy + visual check, which happens during the combined Task 4 verification pass.

- [ ] **Step 1: Add the badge next to the header label**

Current code (`admission_profile.xhtml:17-22`):

```xhtml
                <f:facet name="header" >
                    <div class="d-flex justify-content-between">
                        <div>
                            <h:outputText styleClass="fa fa-id-card"/>
                            <p:outputLabel value="Inpatient Dashboard" class="mt-2 mx-3"/>
                        </div>
```

Replace with:

```xhtml
                <f:facet name="header" >
                    <div class="d-flex justify-content-between">
                        <div>
                            <h:outputText styleClass="fa fa-id-card"/>
                            <p:outputLabel value="Inpatient Dashboard" class="mt-2 mx-3"/>
                            <h:panelGroup rendered="#{admissionController.current.parentEncounter != null}">
                                <p:tag value="Baby Admission — Mother: #{admissionController.current.parentEncounter.patient.person.nameWithTitle} (BHT #{admissionController.current.parentEncounter.bhtNo})"
                                       icon="fa fa-baby" severity="warning" class="mx-2"/>
                            </h:panelGroup>
                        </div>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/webapp/inward/admission_profile.xhtml
git commit -m "$(cat <<'EOF'
feat(inward): show a "Baby Admission" badge on the baby's own dashboard

The mother's and baby's Inpatient Dashboard pages were visually
identical, risking staff discharging/billing the wrong encounter.
Adds a badge naming the mother and her BHT number whenever the
currently-loaded admission has a parentEncounter.

Refs #22294
EOF
)"
```

---

### Task 4: Guard the interim/estimated bill pages against direct navigation with a baby encounter loaded

**Files:**
- Modify: `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` (new method, placed immediately before `createIntrimBillTable()` at line 3133)
- Modify: `src/main/webapp/inward/inward_bill_intrim.xhtml` (add `f:event` inside `f:metadata` near the top of the page)
- Modify: `src/main/webapp/inward/inward_bill_intrim_estimate.xhtml` (same)

**Context — why this task exists:** Task 1 guards the three action methods (`createIntrimBillTable()`, `createTablesWithEstimatedProfessionalFees()`, `navigateToIntrimBillFromPatientProfile()`). Found during Task 4 Playwright verification: `inward_bill_intrim.xhtml:475` binds its Room Details table directly to `#{bhtSummeryController.patientRooms}`, whose getter (`BhtSummeryController.java:448-451`) lazily self-populates via `createPatientRooms()` on first access — bypassing all three guarded methods. Since `AdmissionController.navigateToAdmissionProfilePage()` unconditionally mirrors `admissionController.current` into `bhtSummeryController.patientEncounter` (confirmed in the original investigation) every time *any* Inpatient Dashboard is viewed, `bhtSummeryController.patientEncounter` ends up pointing at a baby merely from visiting the baby's own dashboard — no button click required. A user who then reaches `inward_bill_intrim.xhtml` by browser back/forward, a stale tab, or a bookmark (not via the guarded action methods) sees the baby's charges computed and rendered anyway.

**Interfaces:**
- Consumes: `patientEncounter.getParentEncounter()` (same as Task 1), `com.divudi.core.util.JsfUtil.addErrorMessage(String)` (existing, already imported in `BhtSummeryController.java`).
- Produces: `public void redirectIfEncounterIsBabyAdmission()` on `BhtSummeryController` — a `void`, no-arg method callable from JSF EL as an `f:event` listener. Uses `javax.faces.context.FacesContext` (not yet imported in this file — add the import).

This follows the exact existing precedent at `FinancialTransactionController.java:358-379` (`redirectIfShiftNotStarted()`), which already uses `f:event type="preRenderView"` as a pure validation/redirect guard (not state initialization) on a `@SessionScoped` bean — the one documented exception to this project's "never use `f:viewAction`/`preRenderView` for init on `@SessionScoped` beans" rule (see `.claude/skills/jsf-ajax/SKILL.md` § Navigation Pattern). Skip the check on postbacks (AJAX form submits within the page) exactly like the precedent does, since the encounter was already validated on the initial GET.

- [ ] **Step 1: Add the guard method to `BhtSummeryController.java`**

Add this import near the other `javax.faces.*` imports (check with `grep -n "^import javax.faces" src/main/java/com/divudi/bean/inward/BhtSummeryController.java` first — add only if missing):

```java
import javax.faces.context.FacesContext;
```

Add this method immediately before `createIntrimBillTable()` (`BhtSummeryController.java:3133`):

```java
    /**
     * Guards inward_bill_intrim.xhtml / inward_bill_intrim_estimate.xhtml against
     * being reached (e.g. via browser back/forward or a stale tab) while
     * patientEncounter still points at a baby (child) admission — closes the gap
     * where visiting any Inpatient Dashboard unconditionally mirrors the current
     * admission into patientEncounter, independent of the createIntrimBillTable()/
     * createTablesWithEstimatedProfessionalFees()/navigateToIntrimBillFromPatientProfile()
     * guards. Safe to call on every page load.
     */
    public void redirectIfEncounterIsBabyAdmission() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc.isPostback()) {
            return;
        }
        if (patientEncounter == null || patientEncounter.getParentEncounter() == null) {
            return;
        }
        try {
            fc.getExternalContext().getFlash().setKeepMessages(true);
            JsfUtil.addErrorMessage("Interim/Estimated bills can only be generated for the parent (mother) encounter. "
                    + "Generate it from the mother's admission — it will automatically include this baby's charges.");
            fc.getExternalContext().redirect(
                    fc.getExternalContext().getRequestContextPath() + "/faces/inward/admission_profile.xhtml");
        } catch (java.io.IOException e) {
            // redirect failed — nothing further we can do at render time
        }
    }
```

- [ ] **Step 2: Wire the guard into `inward_bill_intrim.xhtml`**

Find this file's `<ui:define name="content">` (or equivalent) opening section — check with `grep -n "ui:define\|f:metadata" src/main/webapp/inward/inward_bill_intrim.xhtml` first. Add, as the first child inside the `<ui:define name="content">` block (mirroring exactly how `inward_bill_payment.xhtml:17` places its `f:event`, i.e. NOT inside `f:metadata` — the existing precedent places it directly in the content body):

```xhtml
        <f:event type="preRenderView" listener="#{bhtSummeryController.redirectIfEncounterIsBabyAdmission()}"/>
```

- [ ] **Step 3: Wire the guard into `inward_bill_intrim_estimate.xhtml`**

Same single line, in the same relative position, in `src/main/webapp/inward/inward_bill_intrim_estimate.xhtml`.

- [ ] **Step 4: Compile to verify no syntax/type errors**

Run: `mvn -q -pl . compile`
Expected: `BUILD SUCCESS`, no errors referencing `BhtSummeryController.java`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/divudi/bean/inward/BhtSummeryController.java src/main/webapp/inward/inward_bill_intrim.xhtml src/main/webapp/inward/inward_bill_intrim_estimate.xhtml
git commit -m "$(cat <<'EOF'
fix(inward): block interim/estimated bill pages when loaded with a baby encounter

Task 1's guards on createIntrimBillTable()/createTablesWithEstimatedProfessionalFees()/
navigateToIntrimBillFromPatientProfile() don't cover the case where a
user reaches these pages directly (back/forward, stale tab, bookmark)
after bhtSummeryController.patientEncounter was already set to a baby
by simply viewing the baby's own Inpatient Dashboard — the page's Room
Details table lazily self-populates via getPatientRooms(), bypassing
all three action-method guards. Adds a preRenderView redirect guard,
following the existing redirectIfShiftNotStarted() precedent.

Found during Playwright verification. Refs #22294
EOF
)"
```

---

### Task 5: Full build, redeploy, and Playwright + DB verification

**Files:** none (verification only, no code changes).

**Interfaces:** none.

This is the single combined behavioral test cycle for all of Tasks 1-3, run once rather than per-task, per the Global Constraints note above.

- [ ] **Step 1: Clean build**

Run: `mvn clean package -DskipTests`
Expected: `BUILD SUCCESS`, produces `target/rh-3.0.0.war`.

- [ ] **Step 2: Redeploy to local Payara**

Run: `/home/buddhika/payara/glassfish/bin/asadmin redeploy --name rh target/rh-3.0.0.war`
Expected: `Command redeploy executed successfully.` Then check `/home/buddhika/payara/glassfish/domains/domain1/logs/server.log` for deployment errors before proceeding.

- [ ] **Step 3: Playwright — verify independent discharge (regression check, no code change expected to be needed)**

Using the `playwright-e2e` skill workflow against the redeployed local app:
1. Log in, select department (per dev-issue step 4's chosen department/records).
2. Navigate to the chosen mother admission's Inpatient Dashboard, then into a baby admission via the "Baby Admissions" panel.
3. Run Clinical Discharge, Room Discharge (via Room Change), and Nursing Discharge on the baby. Confirm each succeeds and the mother's own discharge status fields are unaffected (check by navigating back to the mother's dashboard).
4. Screenshot each stage into the project `tmp/` folder.

- [ ] **Step 4: Playwright — verify the interim/estimated bill guard**

1. From the Interim Bill search page (`inward_bill_intrim.xhtml`), use the autocomplete to search for the baby's own BHT number/name, select it, and click Generate.
   Expected: growl error "Interim bills can only be generated for the parent (mother) encounter. Generate it from the mother's admission — it will automatically include this baby's charges." — no interim bill table is generated.
2. Repeat on the Estimated Bill search page (`inward_bill_intrim_estimate.xhtml`).
   Expected: growl error "Estimated bills can only be generated for the parent (mother) encounter. Generate it from the mother's admission — it will automatically include this baby's charges."
3. Generate an interim bill against the mother instead. Confirm the baby's charges (e.g. any pharmacy issues/department bill items posted against the baby) are included in the totals — this is the existing aggregation behavior and must still work (regression check).
4. Screenshot each stage into the project `tmp/` folder.

- [ ] **Step 5: Playwright — verify the physical discharge fix**

1. Complete the mother's final bill / administrative discharge (`BhtSummeryController.discharge()` flow, reached from Final Bill on the mother's dashboard).
2. Navigate to the baby's dashboard, complete Nursing Discharge if not already done, then attempt Physical Discharge.
   Expected: succeeds (previously would have been permanently blocked once Task 1's guard is in place, since the baby itself never gets a final bill).
3. Screenshot each stage into the project `tmp/` folder.

- [ ] **Step 6: Visual check — badge**

1. While on the baby's dashboard, confirm the "Baby Admission — Mother: {name} (BHT {no})" badge is visible next to the "Inpatient Dashboard" header.
2. Confirm the badge is absent on the mother's own dashboard.
3. Screenshot both states into the project `tmp/` folder.

- [ ] **Step 7: Verify DB state**

Using local MySQL credentials (see `local_mysql_credentials.md` memory), query the `patientencounter` table for the mother's and baby's row IDs used above and confirm `clinicallydischarged`, `roomdischargedatetime`, `nursingdischarged`, `physicaldischarged`, and `discharged` columns reflect the independent values observed in Steps 3-5 (baby's `discharged` column should remain `NULL`/`false` even though its physical discharge succeeded, since that flag itself is never set on the baby — only inherited at the precondition-check level).

If any step in this task reveals a bug, fix it in the relevant Task 1-3 file, re-run Step 1-2 (rebuild/redeploy), and re-run the failing verification step. Do not proceed to PR creation until all of Steps 3-7 pass.
