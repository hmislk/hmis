# Baby Admission: Independent Discharge + Interim Bill Guard

Issue: [hmislk/hmis#22294](https://github.com/hmislk/hmis/issues/22294)
Branch: `22294-baby-admission-independent-discharge`
Date: 2026-07-20

## Background

A baby admission is linked to its mother's admission via the generic
`PatientEncounter.parentEncounter`/`childEncounters` self-reference
(`AdmissionController.navigateToAddBabyAdmission()`). Both are real
`Admission` entities (JPA subtype of `PatientEncounter`, no extra fields).
There is no dedicated "baby" discriminator column — `parentEncounter != null`
plus `instanceof Admission` is the only reliable way to identify "this is a
baby (child) admission" versus a top-level (mother) admission versus the
unrelated `ClinicalAssessment`/`ClinicalDischarge` documentation-stub child
records that also set `parentEncounter`.

Requirements from the issue:
1. A baby admission must be dischargeable (clinical/room/nursing)
   independently of the parent.
2. The parent admission must likewise discharge independently.
3. An interim bill can only be generated against the parent encounter, never
   directly against a baby encounter — but when generated for the parent it
   must include all child-encounter charges (already works).

## Investigation findings

### 1. Discharge independence — already works, no code change needed

Since issue #20198, `admission_profile.xhtml` (the "Inpatient Dashboard")
has a "Baby Admissions" panel that lets staff navigate into a baby's own
dashboard via `AdmissionController.navigateToAdmissionProfileById(babyId)`.
This re-points every downstream controller field
(`AdmissionController.current`, `BhtSummeryController.patientEncounter`,
`RoomChangeController.current` via `f:setPropertyActionListener`,
`NursingDischargeController.currentEncounter`) at the baby's own `Admission`
row.

None of `InpatientClinicalDataController` (clinical discharge),
`RoomChangeController` (room discharge), or `NursingDischargeController`
(nursing discharge) special-case parent/child — they operate uniformly on
whatever encounter reference they're handed. The relevant status fields
(`clinicallyDischarged`, `roomDischargeDateTime`, `nursingDischarged`,
`physicalDischarged`) all live directly on `PatientEncounter`/`Admission`,
so each baby already has its own independent copies.

**Conclusion**: requirements 1 and 2 are functionally already met. This will
be verified with Playwright (not built) — see Testing below.

### 2. Interim/Estimated Bill guard — 3 real unguarded entry points

The dashboard's "Billing" panel (which contains the Interim Bill, Estimated
Bill, Final Bill, Payments, and Hold Professional Payments buttons) is
**already entirely hidden** for a baby's own dashboard page via an existing
condition:

```xhtml
<!-- admission_profile.xhtml:300 -->
<p:panel header="Billing" ...
    rendered="#{admissionController.current.parentEncounter == null and ...}">
```

So those dashboard buttons are not actually reachable for babies today —
this is a non-issue, and no dashboard `rendered`-attribute change is needed.

However, three other entry points bypass this panel entirely and have **no
guard at all**:

| Entry point | XHTML | Controller method |
|---|---|---|
| Interim Bill search page autocomplete (`completeAdmissionNotFinalized`) lets staff pick any admission incl. a baby's, then hit Generate | `inward_bill_intrim.xhtml:181` | `BhtSummeryController.createIntrimBillTable()` (line 3133) |
| Estimated Bill search page autocomplete (`completePatientDishcargedNotFinalized`), plus 2 other buttons on the same page that call the same method directly | `inward_bill_intrim_estimate.xhtml:66,100,206` | `BhtSummeryController.createTablesWithEstimatedProfessionalFees()` (line 3186) |
| Room Details page's own unconditional "Interim Bill" button (no `parentEncounter` check) | `inward_patient_room_details.xhtml:67-72` | `BhtSummeryController.navigateToIntrimBillFromPatientProfile()` (line 3396) |

The underlying charge-aggregation logic
(`InwardBeanController.fetchChildPatientEncounter()` and the
`fetchXxx(patientEncounter, childPatientEncouters)` family called from
`BhtSummeryController.createTables()`) already correctly folds all
child-encounter (baby) charges into whichever encounter is passed as
`patientEncounter` — **no change needed there**.

**Fix**: add an early guard at the top of each of the three methods above:

```java
if (patientEncounter.getParentEncounter() != null) {
    JsfUtil.addErrorMessage("Interim/Estimated bills can only be generated "
        + "for the parent (mother) encounter — it will automatically "
        + "include this baby's charges.");
    clear();
    return "";
}
```

(exact wording per call site — `createIntrimBillTable`/
`navigateToIntrimBillFromPatientProfile` return `String`,
`createTablesWithEstimatedProfessionalFees` returns `void`, adjust return
statement accordingly). This mirrors the existing provisional-bill guard
already present in `createIntrimBillTable()`.

### 3. Physical discharge edge case

`NursingDischargeController.confirmPhysicalDischarge()` requires
`currentEncounter.getDischarged() == true`. That administrative `discharged`
flag is only ever set via `BhtSummeryController.discharge()` (the
final-billing flow), which — once §2's guard is in place — can only ever
run against a parent encounter. Without a fix, a baby could never complete
physical discharge.

**Fix**: in `confirmPhysicalDischarge()`, treat a baby as administratively
discharged once its parent is:

```java
boolean administrativelyDischarged = Boolean.TRUE.equals(currentEncounter.getDischarged())
        || (currentEncounter.getParentEncounter() != null
            && Boolean.TRUE.equals(currentEncounter.getParentEncounter().getDischarged()));
if (!administrativelyDischarged) {
    JsfUtil.addErrorMessage("Cannot confirm physical discharge: administrative discharge (final bill) has not been completed.");
    return;
}
```

This mirrors the existing `nursingDischarged`-inheritance pattern already
used for `ClinicalAssessment` records at
`InpatientClinicalDataController.java:3245-3246`.

### 4. "Baby Admission" badge

No existing visual cue distinguishes a baby's dashboard from the mother's —
both look identical apart from the (already-hidden) Billing panel. Add a
small `p:tag` next to the "Inpatient Dashboard" header label
(`admission_profile.xhtml:21`), rendered when
`admissionController.current.parentEncounter != null`, showing e.g. "Baby
Admission — Mother: {name} (BHT {no})".

## Scope decisions (confirmed with user)

- Estimated Bill is guarded alongside Interim Bill (same bug, same code
  family), even though only "interim bill" is named in the issue text.
- Physical discharge inheritance fix is included, even though physical
  discharge isn't one of the three discharge types literally named in the
  issue — it's a direct consequence of the interim-bill guard and would
  otherwise silently strand babies.
- UI: add the "Baby Admission" badge. No button-hiding changes needed since
  the dashboard's Billing panel is already fully hidden for babies.

## Testing plan

Playwright + DB verification (dev-issue skill step 7), no automated test
suite exists for this controller family in this codebase:

1. Navigate to a baby admission via the "Baby Admissions" panel.
2. Run clinical, room, and nursing discharge on the baby; confirm each
   succeeds and the mother's own discharge status is unaffected.
3. Attempt to generate an interim bill directly against the baby via the
   Interim Bill search page autocomplete; confirm the guard blocks it with
   the new error message.
4. Same for the Estimated Bill search page.
5. Generate an interim bill on the mother; confirm the baby's charges are
   included in the total (regression check on existing aggregation).
6. Complete the mother's final bill (administrative discharge), then confirm
   the baby can complete physical discharge.
7. Verify DB state (`patientencounter` table: `clinicallydischarged`,
   `roomdischargedatetime`, `nursingdischarged`, `physicaldischarged`,
   `discharged` columns) for mother and baby rows at each step.
