# Inpatient Dashboard Redesign — Design Spec

Date: 2026-07-05
Status: Approved by user, pending implementation plan

## Problem Statement

The Inpatient Dashboard (`/inward/admission_profile.xhtml`, `BhtSummeryController.navigateToInpatientProfile()`) is the central hub for a single patient encounter (BHT). Two independent problems exist today:

1. **Wrong data scope on 6 pharmacy links.** Six buttons in the "Pharmaceuticals & Consumables" panel navigate to pages designed for the Nursing Workbench (`/nurse/index.xhtml`) — generic, all-patient, date-range-filtered search screens (`SearchController`-backed). When reached from the dashboard, a single `patientEncounter` is already known, but the target pages still force the user through From/To Date pickers and a Search click, and in most cases never actually filter by that encounter at all (confirmed by code investigation — the `patientEncounter` property set via `f:setPropertyActionListener` is dead data in 5 of the 6 query methods).
2. **No visual distinction between actions and reports**, and patient identity (name/age/sex/BHT) is not prominent — it's one row among many in a small column-1 panel, competing visually with everything else.

## Scope

This spec covers exactly the items the user identified:
- The 6 pharmacy-related dashboard links that wrongly reuse nurse-workbench search pages.
- The visual/layout redesign of `admission_profile.xhtml` (patient banner + action/report panel separation).

Out of scope (explicitly deferred by user decision during brainstorming): auditing non-pharmacy report links (Lab, Investigation, Professional Fee, Deposit searches) for the same date-filter problem. If the same issue exists there, it will be handled as a separate follow-up.

All existing generic/nurse-workbench pages (`ward_pharmacy_bht_issue_request_list_for_issue.xhtml`, `ward_pharmacy_bht_issue_request_bill_search.xhtml`, `pharmacy_search_sale_bill_bht.xhtml`, `pharmacy_search_sale_bill_item_bht.xhtml`, `pharmacy_search_return_bill_bht.xhtml`) remain **unchanged** — they are still needed for their original multi-patient use case (Nursing Workbench and general pharmacy search menus).

---

## Part A — Encounter-Scoped Pharmacy Pages

### Current State (investigated)

| Dashboard button | Target page | Backing query | `patientEncounter` filter? |
|---|---|---|---|
| BHT Issue Requests | `ward/ward_pharmacy_bht_issue_request_list_for_issue.xhtml` | `SearchController.createInwardBHTForIssueTable()` | No — filters by `toDepartment` + date range only |
| View Pharmacy Requests | `ward/ward_pharmacy_bht_issue_request_bill_search.xhtml` | `SearchController.createInwardBHTRequestTable()` | No — same gap |
| Search Direct Issues by Bill | `inward/pharmacy_search_sale_bill_bht.xhtml` | `SearchController.createTableBht(PharmacyBhtPre)` | No — filters by institution/department + date range |
| Search Direct Issues by Item | `inward/pharmacy_search_sale_bill_item_bht.xhtml` | `SearchController.createBillItemTableBht(PharmacyBhtPre)` | No — only `admissionType` used |
| Search Issue Returns by Bill | `inward/pharmacy_search_return_bill_bht.xhtml` | `SearchController.createReturnBhtBills(PharmacyBhtPre)` | No |
| Search Issue Returns by Item | `inward/pharmacy_search_return_bill_item_bht.xhtml` | **Page and query do not exist** — dead link today | N/A |

These 6 collapse into 3 real data sets:
- **Pharmacy Requests** (items 1+2): both query `Bill` where `billType=InwardPharmacyRequest`; item 1 adds an issued/not-issued split via `PharmacySaleBhtController.checkBillComponent()`.
- **Direct Issues** (items 3+4): both query `PharmacyBhtPre` bills — bill-level vs. item-level rendering of the same data.
- **Issue Returns** (items 5+6): item 5 queries `RefundBill` of type `PharmacyBhtPre`; item 6 has no existing implementation.

### New Design

**New controller**: `com.divudi.bean.inward.InwardPharmacyEncounterReportController` (`@Named`, `@ViewScoped`), following the established pattern in `InwardReportControllerBht` (`patientEncounter` field, guard-in-navigate methods, DTO-JPQL fetch methods, `navigateToAdmissionProfile()` "back" method).

**New DTO**: reuse `com.divudi.core.data.dto.BillListReportDTO` (already has `billId`, `billNumber`, `billTypeAtomic`, `patientName`, `createdAt`, `createdUserName`, `retired/cancelled/refunded`, `total/discount/netTotal`, `bhtNo`, `deptId`) for all 3 bill-level lists — it already supports the "View" reprint navigation pattern used by the encounter-scoped Service Bill list (issue #21247 precedent). Use the enum-accepting constructor (avoids the documented `COALESCE`-on-enum EclipseLink binding gotcha).

**New item-level DTO**: `com.divudi.core.data.dto.InpatientPharmacyBillItemDTO` (new) for the nested/expandable item rows within each bill — fields: `billId` (join key), `itemName`, `itemCode`, `qty`, `netValue`. Populated by a second JPQL query keyed by the list of bill IDs from the bill-level fetch, then grouped in Java by `billId` for the expandable row-toggle UI (PrimeFaces `p:dataTable` with `p:rowToggler` / expansion row).

**Three new pages**, all under `src/main/webapp/inward/reports/`, all following the established shell from `inpatient_pharmacy_item_list_dto.xhtml` (growl → panel header with Download/Print/"Inpatient Dashboard" back button → `col-3` Patient Details + `common:admission_details` → `col-9` results table):

1. **`inpatient_pharmacy_requests_list.xhtml`**
   - Bill-level `p:dataTable`, one row per `InwardPharmacyRequest` bill for this `patientEncounter`.
   - Columns: Bill No, Requested Department, Requested At, Requested By, Status (Issued / Not Issued, derived from whether any issued bill references it).
   - Status filter dropdown (All / Not Issued / Issued Only) replaces the old 3 separate search buttons — client-side or re-query on change.
   - Row expansion shows the nested "Issued" bills (Bill No, Date, Issued By, To Staff, Net Total).
   - Row actions: "View Request" → existing `ward_pharmacy_reprint_bht_issue_request.xhtml` (unchanged). "Issue Medicines" → existing `PharmacySaleBhtController.navigateToIssueMedicinesDirectlyForBhtRequest()` flow (unchanged), rendered only when not yet issued and user has `PharmacyBHTIssueAccept`. Nested "View Bill" → existing `ward_pharmacy_reprint_bht_issue_bill_reprint.xhtml` (unchanged).

2. **`inpatient_pharmacy_direct_issues_list.xhtml`**
   - Bill-level `p:dataTable`, one row per `PharmacyBhtPre` sale bill for this `patientEncounter`.
   - Columns: Bill No, Billed At, Billed By, Net Value.
   - Row expansion shows item-level detail (Item Name, Code, Qty, Net Value) via the new `InpatientPharmacyBillItemDTO`.
   - Row action: "View Bill" → existing `pharmacy_reprint_bill_sale_bht.xhtml` (unchanged).

3. **`inpatient_pharmacy_returns_list.xhtml`**
   - Bill-level `p:dataTable`, one row per `RefundBill` of type `PharmacyBhtPre` for this `patientEncounter`.
   - Columns: Sale Bill No (via `billedBill`), Return Bill No, Returned At, Returned By, Net Value.
   - Row expansion shows returned item-level detail.
   - Row action: "View Bill" → existing `pharmacy_reprint_bill_return_bht.xhtml` (unchanged), disabled per existing `checkActiveReturnCashBill()`/`cancelled` guard logic.

**Dashboard changes** (`admission_profile.xhtml`, "Pharmaceuticals & Consumables" panel, lines ~928-1028): the 6 search/report buttons ("BHT Issue Requests," "View Pharmacy Requests," "Search Direct Issues by Bill," "Search Direct Issues by Item," "Search Issue Returns by Bill," "Search Issue Returns by Item") are replaced by 3 buttons ("Pharmacy Requests," "Direct Issues," "Issue Returns"), each navigating straight to one of the new pages with `patientEncounter` set via `f:setPropertyActionListener` — no filters, no search click, data loads immediately. These 3 buttons move into the new "Pharmacy Reports & History" sub-panel (see Part B).

The action-oriented pharmacy buttons ("Request from Pharmacy," "Direct Issue to BHTs," "Issue Discharge Medicines," "Direct Issue to Theatre Cases," "Receive Medicines from Pharmacy," "Return Medicines to Pharmacy") are unchanged and move into the new "Pharmacy Actions" sub-panel.

---

## Part B — Dashboard Visual Redesign

### 1. Patient Identity Banner

A new full-width strip inside the existing outer `p:panel`, placed directly below the current header row (title + Back to Search/Nursing WorkBench buttons) and above the 4-column row. Shows, in large/bold text: **Name — Age — Sex — BHT No**. The existing encounter-flag badges (`ON_ADMISSION_DEATH`, `RAPID_TEMP_AE`) move here from the header facet, so all patient-identifying/flagging information is in one glanceable strip.

The existing "Patient Details" panel in column 1 remains, now scoped to secondary fields only (Mobile No, Phone No, NIC, Registration Source) — Name/Gender/Age are no longer duplicated there since they're now in the banner. (BHT No also drops from `common:admission_details` header duplication — it stays there too since that panel is reused elsewhere; no change needed to the composite component itself.)

### 2. Action vs. Report Panel Split

Three panels currently mix stateful actions with read-only reports/searches. Each is split into two sub-panels with visually distinct headers — **Actions** sub-panels use an accent color (e.g. `ui-button-success`/blue-green header background) with a bolt or plus-style icon; **Reports** sub-panels use a neutral/grey header with a list or chart-style icon.

- **"Pharmaceuticals & Consumables"** →
  - **"Pharmacy Actions"**: Request from Pharmacy, Direct Issue to BHTs, Issue Discharge Medicines, Direct Issue to Theatre Cases, Receive Medicines from Pharmacy, Return Medicines to Pharmacy.
  - **"Pharmacy Reports & History"**: Pharmacy Requests, Direct Issues, Issue Returns (the 3 consolidated links from Part A).

- **"Room Management"** →
  - **"Room Status"**: current room assignment display (the `ui:repeat` over `activePatientRooms`), Room Details link.
  - **"Room Actions"**: Add New Room, Add Guardian Room, Room Change, Guardian Room Change, Initiate Transfer, Accept Patients.

- **"Clinical Data"** →
  - **"Clinical Actions"**: Clinical Discharge, Nursing Discharge, Physical Discharge (stateful workflow transitions with color-coded complete/incomplete state — unchanged behavior).
  - **"Clinical Reports & Records"**: Patient History, Clinical Notes, Ward Medications, Medicine Timeline, Discharge Medications, Investigations, Images, Diagnosis Card.

Panels that are already single-purpose ("Admission," "Billing," "Documents," "Reports," "Operation Theatre") are unchanged.

### 3. No Functional Regressions

Part B is purely a rendering/layout change plus the button consolidation from Part A. No new privileges, no changed navigation targets other than the 3 pharmacy links, no changed business logic.

---

## Testing Plan

- Playwright E2E: log in, navigate to an active admission's Inpatient Dashboard, verify the patient banner renders Name/Age/Sex/BHT prominently, verify the 3 new pharmacy links land directly on populated lists (no filter step), verify each list's row actions (View Request/View Bill/Issue Medicines) still navigate to the correct existing reprint pages, verify Action vs Report sub-panels render with distinct styling.
- DB verification: confirm the new JPQL queries return the same bills as the old `SearchController` methods would for a given encounter (cross-check row counts against the legacy pages for a test BHT with pharmacy requests, direct issues, and returns).

## Documentation

- New wiki page(s) in `../hmis.wiki` covering the redesigned Inpatient Dashboard and the 3 new pharmacy history pages, per user's end-to-end requirement.
- Update `developer_docs/navigation/inward_navigation.md` — replace the 6-row pharmacy table entries under "Pharmacy (privilege: `InwardPharmacyMenu`)" / "Ward Pharmacy" sections with the 3 new consolidated pages, and note the new controller in the Key Controllers table.

## GitHub Issue

No existing issue covers this (searched `hmislk/hmis` for dashboard/pharmacy-request/encounter-scoped terms — no match). A new issue will be created before implementation begins, following the `/start-issue` workflow.
