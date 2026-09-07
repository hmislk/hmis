# Inpatient Professional Payment Report — Design

**Date:** 2026-08-09
**Related pages:** `inward/inward_bill_professional.xhtml` (surgery professional fee entry/payment), `inward/inward_reports.xhtml` (Inpatient Analytics)

---

## 1. Goal

There is currently no report that shows, per admission, which doctors were charged
professional fees, how much was charged vs. actually paid, and which payment bills paid
them. Finance/admin staff need a single admission-grouped view: for each admission, one
header block (BHT, admission/discharge dates, final bill numbers) followed by one row per
doctor+speciality summarizing their professional fee activity for that admission.

## 2. Data model (established by investigation)

- **Fee charge & payment amounts live on `BillFee`** (`com.divudi.core.entity.BillFee`):
  `staff` (consultant), `speciality`, `feeValue` (charged), `paidValue` (paid so far).
  Charge-side `BillFee` rows belong to bills with `billType = BillType.InwardProfessional`
  and are scoped to an admission via `BillFee.bill.patientEncounter`.
- **Payment/settlement** is done by `InwardStaffPaymentBillController`
  (`saveBillCompo`/`saveBillItemForPaymentBill`, lines ~465-500): creates a new payment
  `Bill`, a `BillItem` with `paidForBillFee = originalBillFee`, and cross-links
  `originalBillFee.referenceBillFee` → the new settlement-side `BillFee` on the payment
  bill. `originalBillFee.paidValue` is updated on the **original charge-side** `BillFee`,
  so summing `paidValue` per doctor+speciality on charge-side rows already gives "paid so
  far" without needing to walk the settlement bills for the amount. Payment bill number
  (`deptId`) and date (`createdAt`) still need to be collected from the distinct payment
  bills reachable via `referenceBillFee.bill` for the "Payment Bill Numbers" / "Payment
  Dates" columns.
- **Scope of "professional fee" for this report:** all `BillType.InwardProfessional`
  fees for the admission — covers both the surgery/theatre path
  (`BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL`, entered via
  `inward_bill_professional.xhtml` / `InwardProfessionalBillController`) and the older
  general inpatient professional-fee path. Not restricted to surgery-only.
- **Query base to reuse:** `InwardProfessionalBillController.fetchEncounterProfessionalFees()`
  (lines 1069-1091):
  ```jpql
  select bf from BillFee bf
  where bf.retired=false and bf.bill.retired=false and bf.bill.cancelled=false
  and bf.bill.patientEncounter=:pe and bf.bill.billType=:bt
  order by bf.createdAt desc
  ```
  with `bt = BillType.InwardProfessional`. Extend the equivalent JPQL to be
  department+date-range scoped (no single `:pe` param) rather than one-encounter-at-a-time,
  following the pattern in
  `InwardReportControllerBht.fetchPaymentBillDtosForDepartment()`.
- **Final bill resolution:**
  - "Confirmed" final bill: `patientEncounter.finalBill` (authoritative pointer),
    equivalently `inwardBeanController.fetchFinalBill(patientEncounter)` — reuse this
    method, do not re-query.
  - "First" final bill: earliest `Bill.finalBillVersionSerial` for the admission among
    non-retired `BillType.InwardFinalBill` / `BillTypeAtomic.INWARD_FINAL_BILL` bills —
    same query shape as `InwardSearch.fetchFinalBillVersions()` (filter on
    `billTypeAtomic`, not just `billType`, to exclude cancellation-copy records), taking
    `order by finalBillVersionSerial asc` with the first result.
- **Bill number field:** `Bill.deptId` (String, printed/department-scoped bill number,
  e.g. `"Inward/INWFINAL/70/2"`). There is no field literally named `depId`.
- **Admission fields:** `PatientEncounter.bhtNo` (String), `dateOfAdmission` (Date),
  `dateOfDischarge` (Date, null if not discharged).

## 3. Report scope & filters

- Lives on **Inpatient Analytics** (`inward/inward_reports.xhtml`) →
  **Professional Payment Reports** tab (not Payment Reports — matches the report's
  subject).
- Filters: **From/To date range on `dateOfAdmission`** (department-scoped, matching
  `inward_payment_bill_list_department_dto.xhtml`'s pattern) **plus an optional BHT
  number filter** to narrow to a single admission.
- **Admissions with zero professional fees in range still appear** (header row with an
  empty/placeholder detail row) — supports auditing which admissions have no professional
  fees recorded at all.

## 4. Report shape

One "block" per admission, using the existing `p:dataTable` + `p:subTable` grouping idiom
(confirmed in use elsewhere, e.g. `reports/inventoryReports/grn.xhtml:469`) — outer table
iterates admissions, subTable iterates that admission's doctor+speciality summary rows.

**Group header (once per admission):**
| Column | Source |
|---|---|
| BHT No | `patientEncounter.bhtNo` |
| Date of Admission | `patientEncounter.dateOfAdmission` |
| Date of Discharge | `patientEncounter.dateOfDischarge` (blank if not discharged) |
| First Final Bill No | earliest `finalBillVersionSerial` bill's `deptId`, blank if none |
| Confirmed Final Bill No | `patientEncounter.finalBill.deptId`, blank if none |

**Detail rows (one per distinct doctor+speciality within the admission, merged across all
their `BillType.InwardProfessional` entries — multiple separate charge entries for the
same doctor+speciality are summed into a single row):**
| Column | Source |
|---|---|
| Consultant Name | `BillFee.staff` (person name) |
| Speciality | `BillFee.speciality` |
| Sum of Added Fee | Σ `BillFee.feeValue` across that doctor+speciality's charge rows |
| Sum of Paid Fee | Σ `BillFee.paidValue` across the same rows |
| Payment Bill Numbers | comma-joined distinct `deptId` of payment bills that settled this doctor+speciality (via `referenceBillFee.bill`) |
| Payment Dates | comma-joined dates of those same payment bills |

Aggregation (grouping by doctor+speciality, string-joining bill numbers/dates) happens in
the **controller in Java** after fetching row-level DTOs — JPQL string-aggregation is not
an established pattern in this codebase, and DTO rules prohibit subqueries inside
`SELECT new DTO(...)`.

## 5. Implementation notes

- New DTO, e.g. `InwardProfessionalPaymentReportDTO`, per
  `developer_docs/dto/implementation-guidelines.md`: `Double` money fields (not
  `BigDecimal`), explicit `LEFT JOIN` + `COALESCE` for nullable relations (e.g. speciality
  may be null), no subqueries in the constructor, `findLightsByJpql(...,
  TemporalType.TIMESTAMP)`.
- New controller method(s) in `InwardReportControllerBht`, following the existing
  `navigateToX()` / `fetchXDtos()` naming pattern used by the two sibling payment reports
  added in the same file.
- New XHTML page under `inward/reports/`, e.g.
  `inward_professional_payment_report_dto.xhtml`.
- **Report Favorites (mandatory, per CLAUDE.md):** add the new report button to both the
  Professional Payment Reports tab and the Favorites tab, using
  `reportKey="inpatientAnalytics_professionalPaymentReport"` (Inpatient Analytics'
  established prefix per `developer_docs/feature/report-favorites.md`), `rendered` on the
  `h:panelGroup` row wrapper (not just the button), and the page-scoped empty-state check
  (not the raw `empty userFavoriteReportController.favorites` expression).
- **Excel export:** add per `developer_docs/feature/excel-export-html-table.md`, matching
  the pattern used by other HTML-table-based reports in this app.

## 6. Out of scope for v1

- No literal `depId` field exists to rename or migrate — confirmed as user shorthand for
  `Bill.deptId`.
- Not restricting to confirmed-final-bill-only fee scope — all professional fees for the
  admission are included regardless of final bill version, per user decision.
