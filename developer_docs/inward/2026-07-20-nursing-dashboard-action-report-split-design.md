# Nursing Dashboard — Action/Report Panel Split Design Spec

Date: 2026-07-20
Status: Approved by user, pending implementation

## Problem Statement

The Inpatient Dashboard (`/inward/admission_profile.xhtml`) was already redesigned (#21852) to separate stateful **Actions** from read-only **Reports & History** within each panel, using colored sub-panel strips (green-tinted Actions, grey-tinted Reports). It also consolidated 6 stale multi-patient pharmacy search links into 4 encounter-scoped report buttons backed by `InwardPharmacyEncounterReportController`, since a single admission is already selected on that page — no date-range/department filter is needed.

The Nursing Dashboard (`/nurse/index.xhtml`, `NursingWorkBenchController`) serves a similar per-selected-admission view (the nurse picks one room/BHT from the sidebar, then acts on it — see `selectAdmission()`), but never received the same treatment:

- **Pharmaceuticals & Consumables panel** (`col-6`, lines ~526-665): 12 buttons flat, no grouping. Includes the 6 actions (BHT Request, Direct Issue, Discharge Issue, Theatre Issue, Receive, Return) mixed with 6 report/search buttons that are stale duplicates of pages meant for the general multi-patient Nursing Workbench queue — not for the one patient already selected here. One of the six, "Search IP Direct Issue Returns by Item" (`/inward/pharmacy_search_return_bill_item_bht`), is a **dead link** — no such page exists in the codebase.
- **Clinical Data panel** (`col-3`, lines ~159-292): mixes discharge-workflow actions (Clinical/Nursing/Physical Discharge, Hold Professional Payments) with read-only history/report links (Patient History, Clinical Notes, Ward Medications, Medicine Timeline, Discharge Medications, Investigations, Images, Diagnosis Card).
- **Room Management panel** (`col-2`, lines ~293-359): mixes the Room Details report link with 4 room-change actions (Add New Room, Add Guardian Room, Initiate Transfer, Accept Patients).

This is the same defect class already fixed once on the Inpatient Dashboard side (#21852) and again for BHT Issue Receive (#22154: "separate nurse-workbench (filtered) vs admission-scoped (unfiltered) lists"). This spec applies the identical fix to the 3 remaining mixed panels on the Nursing Dashboard.

## Scope

In scope — `src/main/webapp/nurse/index.xhtml` only, three panels:
1. Pharmaceuticals & Consumables
2. Clinical Data
3. Room Management

No new controller logic. `InwardPharmacyEncounterReportController`'s 4 navigate methods (`navigateToInpatientPharmacyRequestsList`, `navigateToInpatientPharmacyRequestDraftsList`, `navigateToInpatientPharmacyDirectIssuesList`, `navigateToInpatientPharmacyReturnsList`) already work against any `patientEncounter` set via `f:setPropertyActionListener`; the Nursing Dashboard already holds the selected admission in `admissionController.current` (set by `NursingWorkBenchController.selectAdmission()`), so the same buttons wire up directly.

Out of scope: Service panel, Operation Theatre panel, Laboratory panel, Edit panel — these are already single-purpose (all-actions or already narrow) and don't mix concerns. The Inpatient Dashboard itself is unchanged (already correct). No new privileges.

## Design

### 1. Pharmaceuticals & Consumables → split into two sub-panels

Reuse the exact visual pattern from `admission_profile.xhtml` (colored strip, uppercase small muted label, `row g-2` / `col-6` button grid):

**Pharmacy Actions** (green-tinted strip `#e8f5ee`):
- Pharmacy BHT Request (`admissionController.navigateToPharmacyBhtRequest`)
- Direct Issue to BHTs
- Issue Discharge Medicines
- Direct Issue to Theatre Cases
- Receive Medicines from Pharmacy
- Return Medicines to Pharmacy

(All 6 keep their existing action methods/listeners unchanged — this is a pure markup regroup, no logic change.)

**Pharmacy Reports & History** (grey-tinted strip `#eef1f4`) — replaces the 6 stale buttons with the 4 already-built encounter-scoped report buttons, copied verbatim from `admission_profile.xhtml` lines 1047-1102, retargeted at `admissionController.current`:
- Pharmacy Requests → `inwardPharmacyEncounterReportController.navigateToInpatientPharmacyRequestsList()`
- Draft Pharmacy Requests → `navigateToInpatientPharmacyRequestDraftsList()`
- Direct Issues → `navigateToInpatientPharmacyDirectIssuesList()`
- Issue Returns → `navigateToInpatientPharmacyReturnsList()`

Each carries `<f:setPropertyActionListener value="#{admissionController.current}" target="#{inwardPharmacyEncounterReportController.patientEncounter}" />`.

Removed entirely (stale, multi-patient-scoped, superseded by the above): "BHT Issue Requests", "View Pharmacy Requests", "Draft BHT Issue Requests" (old `SearchController`-backed variant), "Search IP Direct Issues by Bill", "Search IP Direct Issues by Item", "Search IP Direct Issue Returns by Bill", "Search IP Direct Issue Returns by Item" (the dead link). These pages remain unchanged/untouched — they're still reachable from the general Pharmacy menu (`InwardPharmacyMenu`) for actual multi-patient search use; only the dashboard shortcuts are removed, per the same precedent as #21852's Part A scope note.

### 2. Clinical Data → split into two sub-panels

**Clinical Actions** (green-tinted strip):
- Clinical Discharge, Nursing Discharge, Physical Discharge (existing color-coded complete/incomplete `styleClass` logic unchanged)
- Hold Professional Payments

**Clinical Reports & Records** (grey-tinted strip):
- Patient History, Clinical Notes, Ward Medications, Medicine Timeline, Discharge Medications, Investigations, Images, Diagnosis Card

All existing `action`/`rendered`/`f:setPropertyActionListener` attributes carry over unchanged — markup regroup only.

### 3. Room Management → split into two sub-panels

**Room Actions** (green-tinted strip):
- Add New Room, Add Guardian Room, Initiate Transfer, Accept Patients

**Room Status** (grey-tinted strip):
- Room Details

Existing attributes unchanged.

### Column widths

Splitting each panel into two vertically-stacked sub-panels doesn't need wider columns — the existing `col-2`/`col-3`/`col-6` panel widths are kept, matching the Inpatient Dashboard's own `col-3` pharmacy panel which fits both sub-panels comfortably. Only the internal button grid changes (flat list → two grouped `row g-2` grids with a header label and background tint each).

## No functional regressions

Pure rendering/grouping change plus removal of 7 stale/redundant navigation shortcuts (6 replaced by existing encounter-scoped equivalents, all pointing at controller methods/pages that remain fully intact for their original general-search use elsewhere in the app). No new privileges, no changed business logic, no changed target pages except the dashboard's own button wiring.

## Testing Plan

- Playwright E2E: log in, open Nursing Workbench, select a room/BHT, verify Pharmacy/Clinical Data/Room Management panels render with distinct Actions vs Reports sections, verify the 4 pharmacy report buttons land directly on the encounter-scoped list pages (same as Inpatient Dashboard equivalents) with data pre-loaded for the selected admission, verify no dead links remain.
- Manual/code check: confirm none of the 7 removed buttons' target pages/controllers are now orphaned (they're still reachable via the general Pharmacy menu — `InwardPharmacyMenu` items in `developer_docs/navigation/inward_navigation.md`).

## Documentation

- Update `developer_docs/navigation/inward_navigation.md` if the Nursing Dashboard section needs it (currently the doc doesn't enumerate nurse/index.xhtml's internal panel buttons in detail — check during implementation whether an update is warranted).

## GitHub Issue

No existing open issue covers this exact scope (checked `hmislk/hmis` issues — #22154 and #21852 are the precedent/related closed issues, not duplicates). A new issue will be created before implementation, following `/start-issue`.
