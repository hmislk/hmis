# Package Admission Menu Relocation + Change Package Screen

**Date:** 2026-09-29
**Branch:** to be created from `origin/development` via `start-issue`
**Related issue:** to be filed via `dev-issue` skill (issue #24127 is a separate, already-merged, prior piece of work — configuration-only tab move + per-charge-category pricing on `InpatientPackage`; not to be reopened)

## Problem

The user asked for three things, believing none of them existed yet:

1. A menu item for "Package Admission" under the Inpatient → Admissions submenu.
2. A place to record which `InpatientPackage` an admission (`PatientEncounter`) was
   admitted under, populated only via a dedicated package-admission path, not
   editable via the normal admission route.
3. A button on `admission_profile.xhtml`'s Admission panel to navigate to a new
   page where staff can change the package on an existing admission, with an
   audit trail.

Investigation (see below) found that **items 1 and 2 already exist** on
`development`, built in an earlier, separate piece of work (commit `7282a0c954`
and the wider package-billing series starting at PR #22089) that predates and is
independent of issue #24127. Only item 3 — changing the package on an *existing*
admission — is genuinely missing. This spec covers relocating the existing menu
item to match the user's original intent, plus building item 3.

## What already exists (verified, not to be rebuilt)

- **Menu item** "Package Admission" in `menu.xhtml`, currently under the
  **Appointment** submenu (not Admissions), gated by privilege
  `InwardPackageAdmission`, action
  `inpatientPackageAdmissionController.navigateToPackageAdmitFromMenu()`.
- **`package_admit.xhtml`** + **`InpatientPackageAdmissionController`**
  (`src/main/java/com/divudi/bean/inward/InpatientPackageAdmissionController.java`) —
  search-and-select a patient + package, then `navigatePackageAdmit()` builds a
  new `Admission`, sets `admissionType` and `inpatientPackage` from the chosen
  package, stashes it on the session-scoped `AdmissionController.current`, and
  redirects to `inward_admission.xhtml` — the same session-state "carry"
  mechanism `appointment_admit.xhtml` uses.
- **`PatientEncounter.inpatientPackage`** (`src/main/java/com/divudi/core/entity/PatientEncounter.java:106`) —
  `@ManyToOne InpatientPackage`, already persisted. `inward_admission.xhtml` has
  **no UI referencing it at all**, confirming it rides along invisibly and is not
  editable through the normal admission route — this matches what the user asked
  for and needs no change.
- **Billing application** — `InpatientPackageApplicationBean.applyPackageToAdmission(...)`
  runs from `AdmissionController.saveSelected()` whenever
  `getCurrent().getInpatientPackage() != null`, locking in room/service/professional-fee/timed-item
  billing rows from the package's components, with a compensating-transaction
  rollback (retire room + admission) if it fails. This is mature, actively
  hardened code (room-charge locking, pharmacy rate overrides, staff-assignment
  mode) with a long commit history — **not touched by this spec**.

## Explicitly out of scope

The user separately described a much larger change: moving from "lock charges at
admission time" to "bill normally through the stay, compare actual charges
against the package price only at final bill creation." That is a re-architecture
of `InpatientPackageApplicationBean` and the final-bill flow, independent of the
menu/change-screen work here, and is deliberately **not** part of this spec — it
is the next piece of development after this one.

**Accepted, documented gap:** because this pass does not touch the billing
engine, if an admission's current package already has locked billing rows,
changing the package via the new screen does **not** reverse or reapply those
rows. The recorded `inpatientPackage` and the actual locked charges can disagree
until the billing redesign lands. Per explicit user instruction, no in-app
warning is shown for this — it's called out here, and will be called out in the
PR/issue and a wiki note, so it isn't a surprise later.

## Design

### 1. Menu relocation

In `src/main/webapp/resources/ezcomp/menu.xhtml`:
- Remove the `Package Admission` `p:menuitem` from the **Appointment** submenu
  (`InwardAppointmentMenu`).
- Add the identical `p:menuitem` (same action, same privilege
  `InwardPackageAdmission`, same icon `fa fa-box-archive`) to the **Admissions**
  submenu (`InwardAdmissions`), alongside "Patient Admit" / "Edit Admission
  Details".

No controller change needed — same action method.

### 2. New "Package" panel on `admission_profile.xhtml`

A new panel (own `p:panel`, not folded into the existing Admission panel — the
user chose a dedicated panel, mirroring Room Management's own panel), gated by a
new privilege `InpatientDashboardPanelPackage`:

- Displays the current `admissionController.current.inpatientPackage` name, or
  "No Package" if null.
- One button, **Change Package**, gated by a new privilege
  `InwardPackageChange`, `disabled="#{admissionController.current.discharged eq
  true}"` (matches the Room Change button's convention — no point changing the
  package on a closed encounter).
- `f:setPropertyActionListener value="#{admissionController.current}"
  target="#{packageChangeController.current}"` hands off the current
  `Admission`, then `action="#{packageChangeController.navigateToPackageChange}"`
  navigates to the new page (`ajax="false"`, matching Room Change).

### 3. New page `inward/inward_package_change.xhtml` + `PackageChangeController`

File naming follows the confirmed `inward_room_change.xhtml` convention.

**Page contents:**
- Read-only admission context: BHT no, patient name, admission type, room
  category (for staff to confirm they're changing the right encounter).
- Current package (name, or "None").
- A package picker — `p:autoComplete` against `InpatientPackage`, same unfiltered
  search pattern as `package_admit.xhtml` (staff may deliberately choose a
  package that doesn't match the admission's type/room category — that's a
  business judgment call, not one this screen should block).
- An explicit way to clear the package back to "None" (e.g. a "Remove Package"
  button alongside Save, or an empty-selection state accepted by Save — pick
  whichever the `jsf-frontend-dev` implementer finds cleanest against the
  autocomplete's existing clear-selection UX).
- Save button → `PackageChangeController.change()`.

**Controller** (`src/main/java/com/divudi/bean/inward/PackageChangeController.java`,
new, `@Named @SessionScoped`), mirroring `RoomChangeController.change()`'s
shape:

```java
public void change() {
    Map<String, Object> beforeState = packageStateMap(getCurrent().getInpatientPackage());
    getCurrent().setInpatientPackage(newPackage); // newPackage may be null (explicit removal)
    getEjbFacade().edit(getCurrent());
    Map<String, Object> afterState = packageStateMap(getCurrent().getInpatientPackage());
    recordPackageAuditEvent(getCurrent(), "Package Changed", beforeState, afterState);
    // navigate back to admission_profile.xhtml
}

private Map<String, Object> packageStateMap(InpatientPackage p) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("package", p != null ? p.getName() : null);
    m.put("admissionType", p != null && p.getAdmissionType() != null ? p.getAdmissionType().getName() : null);
    m.put("roomCategory", p != null && p.getRoomCategory() != null ? p.getRoomCategory().getName() : null);
    m.put("totalPrice", p != null ? p.getTotalPrice() : null);
    return m;
}

private void recordPackageAuditEvent(PatientEncounter pe, String trigger,
        Map<String, Object> before, Map<String, Object> after) {
    try {
        auditService.logEncounterAudit(pe, trigger, before, after,
                sessionController.getLoggedUser(), pe.getClass().getSimpleName(), pe.getId(),
                sessionController.getInstitution() != null ? sessionController.getInstitution().getId() : null,
                sessionController.getDepartment() != null ? sessionController.getDepartment().getId() : null);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

No call to `InpatientPackageApplicationBean` anywhere in this controller — this
is the explicit "record-only" boundary agreed with the user.

### 4. New privileges

In `Privileges.java`, under the existing `"Inward"` category (same grouping as
`InwardPackageAdmission` / `InpatientDashboardPanelRoomManagement`):
- `InpatientDashboardPanelPackage` — gates the new panel's visibility.
- `InwardPackageChange` — gates the Change Package button/action.

### No DDL changes

`PatientEncounter.inpatientPackage` already exists as a persisted field — this
spec adds no new entity fields or tables, so `generate-ddl` does not need to run
for this work.

## Files touched

- `src/main/webapp/resources/ezcomp/menu.xhtml` — move menu item
- `src/main/webapp/inward/admission_profile.xhtml` — new Package panel
- `src/main/webapp/inward/inward_package_change.xhtml` — new page
- `src/main/java/com/divudi/bean/inward/PackageChangeController.java` — new controller
- `src/main/java/com/divudi/core/data/Privileges.java` (or wherever the
  `Privileges` class actually lives — confirm path during implementation) — 2
  new privilege constants

## Not touched (explicitly out of scope)

- `InpatientPackageApplicationBean` and the locked-charge billing model
- `package_admit.xhtml` / `InpatientPackageAdmissionController` (entry-point flow) — works as-is
- `inward_admission.xhtml` — stays free of any package UI, as already confirmed
- Any reversal/reapplication of billing rows when a package is changed

## Verification plan

1. `mvn clean package -DskipTests` build check, then local redeploy.
2. Playwright: log in, select department, navigate **Inpatient → Admissions →
   Package Admission** (never by URL) — confirm it's gone from the Appointment
   submenu and present under Admissions, and still reaches
   `inward_admission.xhtml` correctly via the existing flow.
3. Playwright: open an existing admission's `admission_profile.xhtml`, confirm
   the new Package panel shows the current package (or "No Package"), click
   Change Package, confirm the new page shows the right admission context,
   change to a different package, Save, confirm the panel now reflects the new
   package.
4. Repeat for removing a package (set to None) and for adding one to an
   admission that had none.
5. Verify in local DB: query `AUDITEVENT` for `eventTrigger = 'Package Changed'`
   rows tied to the test encounter's `patientEncounterId`, confirm
   `beforeJson`/`afterJson` reflect the actual change.
6. Confirm no new billing rows (`PHARMACEUTICALBILLITEM`, `BILLITEM`, etc.) are
   created purely by the package change — the change is record/audit-only.
