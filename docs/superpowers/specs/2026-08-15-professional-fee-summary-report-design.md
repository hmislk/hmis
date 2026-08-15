# Professional & Other Fee Summary Report — Design

**Date:** 2026-08-15
**Related pages:** `inward/inward_report_invoice_journal.xhtml` (Inpatient Invoice Journal, sibling report),
`inward/inward_reports.xhtml` (Inpatient Analytics index — this report's new home)

---

## 1. Goal

Finance/admin staff want a per-admission report that shows, in three columns, how much of
the total bill was professional (consultant) fees vs. everything else: **Professional Fee
Total**, **Other Fee Total**, **Net Total**. Today this breakdown only exists buried inside
the Inpatient Invoice Journal's 16-column table (one column per `InwardChargeType`), which
is hard to scan for this specific question.

This ships as a **new, separate report** (not a modification of the Journal), plus two
unrelated small UI fixes bundled into the same piece of work at the user's request:

1. Button widths on the inward reports index page are inconsistent — fix to a reliable
   full-width layout.
2. The Invoice Journal's wide table currently compresses columns to fit the visible area
   (values wrap, BHT No / Patient Name become hard to read) instead of letting the user
   scroll horizontally — fix to natural column widths + horizontal scroll.

## 2. Data model / calculation rules (agreed with user)

- **Professional Fee Total** = gross `InwardChargeType.ProfessionalCharge` only (Consultant
  fees). `DoctorAndNurses` (assisting staff) is **not** professional fee for this report —
  it counts as Other.
- **Other Fee Total** = sum of every other `InwardChargeType` (AdmissionFee, RoomCharges,
  DoctorAndNurses, MOCharges, NursingCharges, LinenCharges, AdministrationCharge,
  MedicalCareICU, MaintainCharges, Medicine, Laboratory, etc. — everything
  `InwardInvoiceJournalController` already fetches via `fetchChargesByEncounter()`, minus
  `ProfessionalCharge`).
- **Net per bucket** (not gross-then-netted-once): each bucket already has its own
  discount/service-charge applied, split by whether the underlying `BillFee.bill.billType`
  is `InwardProfessional` (→ Professional bucket) or anything else (→ Other bucket):
  - `Professional Net = ProfessionalCharge gross − professional-bill discount + professional-bill service charge`
  - `Other Net = (sum of all other charge types) − non-professional-bill discount + non-professional-bill service charge`
  - `Net Total = Professional Net + Other Net`
- **Cross-check property:** for the same admission and filters, `Net Total` on this report
  must equal the Invoice Journal's existing `Net Total` column (both derive from the same
  live `BillItem`/`BillFee` data, same excluded snapshot bill types
  `INWARD_FINAL_BILL`/`INWARD_ORIGINAL_FINAL_BILL`). Use this for verification.
- **"Confirmed final bill" concern:** no special handling needed. Both this report and the
  Journal compute totals from live `BillItem`/`BillFee` rows on the real transactional
  bills, explicitly excluding the final-bill snapshot bill types. Since final-bill
  snapshots are copies for printing/audit (not the source of truth), the multi-final-bill-
  version feature (#22282) does not affect these numbers — there's nothing to "pick a
  version" of. `Final Bill No` on the row still comes from `PatientEncounter.finalBill`
  (the confirmed one), matching what the Journal report shows.

## 3. New report

- **Display name:** "Professional & Other Fee Summary"
- **New files:**
  - `src/main/webapp/inward/inward_report_professional_fee_summary.xhtml`
  - `src/main/java/com/divudi/bean/inward/InwardProfessionalFeeSummaryController.java` (`@Named @SessionScoped`)
  - `src/main/java/com/divudi/core/data/dto/InwardProfessionalFeeSummaryRowDto.java`
- **Filters** (identical set/behavior to Invoice Journal, delegated to the shared service —
  see §4): From/To date, Date Basis (admission/discharge), Admission Status, Admission
  Type, Payment Method, Institution, Site, Department.
- **Columns:** BHT No, Patient Name, Admitted, Discharged, Final Bill No, Admission Type,
  Professional Fee Total, Other Fee Total, Net Total. Footer row with column totals plus a
  grand-total summary line below the table (same layout pattern as the Journal report).
  Print / Excel / PDF export buttons via `p:printer` / `p:dataExporter`, same as Journal.
- **Menu wiring:** added to `inward_reports.xhtml` under the "BHT Summary Reports" tab
  (next to Inpatient Invoice Journal) **and** the ⭐ Favorites-tab block, each wrapped in
  `fav:favoriteStar` per the project's Report Favorites convention. New `reportKey`:
  `inpatientAnalytics_professionalFeeSummary` (must be globally unique — verified against
  existing `reportKey` values in `inward_reports.xhtml` and `reports/index.xhtml`).

## 4. Shared charge-aggregation service (refactor)

New `@Stateless` EJB: `com.divudi.service.inward.InwardBhtChargeAggregationService`.

**Moved out of `InwardInvoiceJournalController`** (method bodies unchanged except where
noted):
- `fetchEncounters()` — the filter-driven `PatientEncounter` search
- `fetchChargesByEncounter()` and its sub-fetchers: `fetchBillItemCharges`,
  `fetchAdmissionFeeCharges`, `fetchPatientRoomCalculatedCharges`,
  `fetchPatientRoomServiceItemCharges`, `fetchProfessionalFeeCharges`,
  `fetchAssistingFeeCharges`, `fetchPharmacyBillCharges`, `fetchStoreBillCharges`,
  `chargeTypesByCalculationMethod`, `mergeChargeMaps`, `collectChargeTypeRows`,
  `mergeIfNonZero`
- `fetchDiscountAndMarginByEncounter()` — kept as-is (single combined total, used by
  Journal) **plus** a new sibling method
  `fetchDiscountAndMarginSplitByProfessional()` that returns
  `Map<Long, double[]>` keyed the same way but with `double[]{professionalDiscount,
  professionalMargin, otherDiscount, otherMargin}`, adding `and bf.bill.billType = :btp` /
  `!= :btp` (`BillType.InwardProfessional`) to the existing WHERE clause. Only the new
  report calls this; Journal is untouched.

**Stay in `InwardInvoiceJournalController`** (Journal-only, no other consumer):
`fetchDepositTotalsByEncounter`, `fetchCreditSettlementByEncounter`,
`fetchCreditCompanyNamesByEncounter`.

Both controllers get `@EJB InwardBhtChargeAggregationService` and call it instead of having
private copies. `InwardInvoiceJournalController`'s filter fields (`fromDate`, `toDate`,
`dateBasis`, etc.) are passed into the service's `fetchEncounters(...)` as parameters
(service stays stateless/session-free); the controller keeps owning the fields themselves.

**Regression check:** after the refactor, the Invoice Journal report must produce identical
output to before. Verify with the same filter combination used in the current Playwright
session (Aug 1–15 2026, "Discharged and final bill completed") and confirm all figures
(BHT/56753, BHT/56754, OPDCARD/43473 rows + grand totals) are unchanged.

## 5. Button-width fix (`inward_reports.xhtml`)

Every report button row currently looks like:

```xml
<h:panelGroup layout="block" styleClass="d-flex align-items-center gap-1 w-100">
    <p:commandButton styleClass="w-100" .../>
    <fav:favoriteStar .../>
</h:panelGroup>
```

`width:100%` on the button (a flex item sharing the row with the fixed-size star button)
fights the flex-shrink algorithm instead of reliably filling the remaining space, producing
inconsistent widths across rows on real screens. Fix: give the button `flex: 1 1 auto` (via
an added inline style or a small new CSS class, e.g. `.report-btn-grow`) so it grows to fill
whatever space the star button doesn't need, and drop the now-redundant `w-100` on the
button. Applied consistently to all report-button rows in `inward_reports.xhtml` (both the
Favorites-tab and category-tab copies of every entry, plus the new report's two rows added
in §3).

**Scope:** `inward_reports.xhtml` only, not `reports/index.xhtml` or other report index
pages.

## 6. Horizontal scroll for the Invoice Journal table

In `inward_report_invoice_journal.xhtml`:
- Wrap the `p:dataTable` (`id="tblReport"`) in `<div style="overflow-x:auto;">`.
- Remove `styleClass="w-100"` from the `p:dataTable` itself so columns keep their natural
  content width instead of being compressed to fit the visible container.
- Add `style="white-space:nowrap;"` to the two columns that currently lack it and can wrap
  (Patient Name, Credit Company) — every other column already has `white-space:nowrap`.

**Scope:** this table only, not applied to other report tables in this pass.

## 7. Out of scope

- No changes to the multi-final-bill-version feature or its data model.
- No changes to `reports/index.xhtml` (the general, non-inward reports index).
- No deposit / credit-settlement columns on the new report (Journal already covers that).
- No changes to how discount/service-charge are computed at the `BillFee` level — only how
  they're bucketed for display.
