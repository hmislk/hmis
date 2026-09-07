# Inpatient Direct Issue Bill — Service Charge, Discount & A4/FiveFive/POS Print Formats

**Date:** 2026-07-13
**Branch:** `22035-view-bill-missing-items-bht-return` (updates PR #22054)
**Related issue:** #22035 (View Bill missing Item/Qty on Direct Issue to BHT Return)

## Problem

The print/preview bills for **inpatient pharmacy direct issue** (and the related
BHT return / reprint pages) do not consistently show, per item and at bill level:

- **Gross value**
- **Margin** — must be labelled **"Service Charge"** at every location
- **Discount**
- **Net value**

Additional gaps on this bill family:

1. Margin is labelled inconsistently — "Service Charge" on the entry page but
   **"Matrix Value"** on `pharmacy_reprint_bill_sale_bht.xhtml` (View Bill table).
2. There is **no A4 print format** anywhere in this family — only FiveFive and a
   POS-with-header format.
3. The print-format chooser is incomplete/inconsistent: the reprint page's gear
   dialog offers only POS + 5x5 (and uses the **deprecated** `pharmacyBillPaperType`
   / `departmentPreference` mechanism), while the direct-issue page has **no gear
   dialog at all** (plain Print button, two hard-wired composites).

The user wants, across the direct-issue print **and** the BHT return/reprint prints:

- Per-item **Gross / Service Charge / Discount / Net** on **all three** formats.
- Bill-level **Gross / Service Charge / Discount / Net**.
- "Service Charge" label everywhere (never "Margin"/"Matrix Value").
- A **print-config gear button** supporting **A4, FiveFive, and POS**.

## Scope

**In scope** (chosen: "Both issue + return/reprint pages"):

- `src/main/webapp/inward/pharmacy_bill_issue_bht.xhtml` — DTO-native direct issue (primary).
- `src/main/webapp/inward/pharmacy_reprint_bill_sale_bht.xhtml` — issue #22035 page (entity family).
- `src/main/webapp/inward/pharmacy_bill_return_bht_issue.xhtml` — BHT issue-return (entity family).
- `src/main/webapp/inward/pharmacy_reprint_bill_return_bht.xhtml` — reprint return (entity family).

**Explicitly out of scope / must NOT regress:**

- `pharmacy_bill_retail_sale_native.xhtml`, `pharmacy_bill_wholesale_sale_native.xhtml`
  — these share the DTO-native composites. Their output must be **unchanged**.
- Any non-inpatient sale-bill composite.

## Key finding: the data already exists (no controller/query changes)

Both composite families already have all four values available:

| Level | DTO family (issue page) | Entity family (return/reprint pages) |
|-------|-------------------------|--------------------------------------|
| Item gross | `BillItemData.grossValue` | `BillItem.grossValue` |
| Item margin (Service Charge) | `BillItemData.marginValue` | `BillItem.marginValue` |
| Item discount | `BillItemData.discountValue` | `BillItem.discount` |
| Item net | `BillItemData.netValue` | `BillItem.netValue` |
| Bill gross | `PrintBillData.total` | `Bill.total` |
| Bill margin (Service Charge) | `PrintBillData.margin` | `Bill.margin` |
| Bill discount | `PrintBillData.discount` | `Bill.discount` |
| Bill net | `PrintBillData.netTotal` | `Bill.netTotal` |

Therefore this is a **print-template + print-config feature only**. No JPQL, no
DTO constructors, no facade changes.

## Two composite families

- **DTO-native family** (takes `PrintBillData` + `List<BillItemData>`):
  `inward_direct_issue_bill_native_five_five_custom_3`, `saleBill_Header_Inward_native`.
  Used by the issue page **and the retail/wholesale sale pages** — shared, must not regress.
- **Entity family** (takes a `Bill`): `inward_direct_issue_bill_five_five_custom_3`,
  `saleBill_Header_Inward`, `saleBill_Header_Return`, `returnBill`, etc.
  Used by the return/reprint pages.

## Approach (chosen: Approach A — new dedicated composites)

Create **new** print composites dedicated to the inpatient direct-issue/return bills
so the widely-shared sale composites are never touched. Each family gets three new
layout composites showing **Gross / Service Charge / Discount / Net** per item and at
bill level. Wire them behind a standard gear/Settings config dialog supporting **A4 /
FiveFive / POS**.

### New composites

**DTO family** (`src/main/webapp/resources/pharmacy/`), attrs `bill` (`PrintBillData`) +
`items` (`List<BillItemData>`) + optional `duplicate`:

1. `inward_direct_issue_bill_native_a4.xhtml` — A4 full table.
2. `inward_direct_issue_bill_native_five_five.xhtml` — FiveFive (all 4 money columns; user chose "all four columns on every format").
3. `inward_direct_issue_bill_native_pos.xhtml` — POS (all 4 money columns).

**Entity family** (`src/main/webapp/resources/pharmacy/inward/` namespace to match
existing `ph:`/`phi:` usage), attr `bill` (`Bill`) + optional `duplicate`:

4. `inward_direct_issue_bill_a4.xhtml`
5. `inward_direct_issue_bill_five_five.xhtml`
6. `inward_direct_issue_bill_pos.xhtml`

> Naming/namespace of each new file will be confirmed against the existing
> `resources/pharmacy` vs `resources/pharmacy/inward` folders during implementation so
> the `xmlns` prefixes on each page resolve correctly.

Each composite renders:

- Header (department/institution/patient/BHT/room/bill no/date) — reuse the layout
  already present in the corresponding existing composite.
- Item rows: `No | Item | Qty | Rate | Gross | Service Charge | Discount | Net`.
- Bill totals block: `Gross | Service Charge | Discount | Net`.
- Money columns gated by the **existing** guard (user chose "keep existing gating"):
  `configOptionApplicationController.getBooleanValueByKey('Nursing IP Billing - Show Rate and Value', false) and webUserController.hasPrivilege('NursingIPBillingViewRates')`.
- Every margin label reads exactly **"Service Charge"**.

### Print-format config (gear dialog)

Follow the documented standard (`developer_docs/configuration/printer-configuration-system.md`):
gear/Settings button (privilege `ChangeReceiptPrintingPaperTypes`) → `p:dialog` → **three
`h:selectBooleanCheckbox`** (A4 / FiveFive / POS), each format's `panelGroup` rendered on
its **own** boolean key. Application-wide scope (`configOptionApplicationController`) to
match the keys already on these pages.

**New config keys** (application-wide):

- `Pharmacy Inward Direct Issue Bill is A4`
- `Pharmacy Inward Direct Issue Bill is FiveFive`
- `Pharmacy Inward Direct Issue Bill is POS`

Defaults: A4 = false, FiveFive = true, POS = false (FiveFive is the current de-facto default).

> The existing keys `...is FiveFiveCustom3` and `...is PosHeaderPaper` remain in place
> for backward compatibility and continue to render the **old** composites. The new keys
> drive the **new** composites. The gear dialog exposes the three new keys. Existing
> render conditions for the old composites are left as-is so any deployment already
> depending on them is unaffected; new deployments enable the new keys. (If, on review,
> the user prefers to retire the old composites on these four pages, that becomes a small
> follow-up — kept out of the first pass to limit risk.)

The reprint page's current deprecated `pharmacyBillPaperType` checkboxes
(`isReprintBhtIssuePosPaper` / `FiveFivePaper` in `PharmacyBillSearch`) are **left
untouched** for now to avoid behavioural regression on POS/5x5 selection; the new A4/
FiveFive/POS gear options are added alongside, driving the new composites. Consolidating
the two mechanisms is a candidate follow-up, flagged but not done here.

### Wiring per page

- `pharmacy_bill_issue_bht.xhtml` (print preview header): add the gear button + dialog;
  add three `h:panelGroup`s (one per new key) inside `gpBillPreview` rendering the three
  new **DTO** composites with `bill="#{...printBill}"` / `items="#{...printBillItems}"`.
  Keep existing composites rendered on their old keys (both can be off/on independently;
  by default only one new key is true).
- The three entity pages: add the gear button + dialog (reuse the reprint page's dialog
  shape) and three `h:panelGroup`s rendering the new **entity** composites bound to the
  page's existing `Bill` (`pharmacyBillSearch.bill`, `bhtIssueReturnController.bill` /
  `.returnBill`). On `pharmacy_reprint_bill_sale_bht.xhtml`, also relabel the existing
  "Matrix Value" column in the left View Bill `tbl` to **"Service Charge"** and add the
  missing per-item Gross/Discount columns there so the on-screen View Bill (the literal
  subject of #22035) matches the printed bill.

### Config backing bean

Add three boolean properties + load/save to a controller. To match the existing keys'
application-wide scope and the doc's guidance that the backing bean lives in the owning
module, add them to **`PharmacyConfigController`** (or, if simpler and consistent with the
issue page's controller wiring, a small config method on the inpatient direct-issue
controller). Uses `configOptionApplicationController.setBooleanValueByKey` /
`getBooleanValueByKey`. Save method shows a `JsfUtil` success message and reloads.

## CSS

- A4 composites: a print stylesheet with `@media print` page sizing; reuse existing
  `inward_direct_issue_bill_*` CSS conventions where possible.
- FiveFive/POS: reuse the existing thermal CSS (`sale_bill_five_five_custom_3.css`,
  `pharmacypos_header.css`) as a base; widen/adjust only as needed for the four money
  columns. Because the user chose "all four columns on every format", the thermal
  templates use a compact font and right-aligned narrow numeric columns; verify legibility
  in the browser during E2E.

## Error handling / edge cases

- Negative values (returns/cancellations): the existing pages render abs() in places
  (e.g. `#{bid.grossValue < 0 ? -bid.grossValue : bid.grossValue}`). New composites follow
  the same convention per family so returns print positive magnitudes consistently.
- When the rate/value guard is off (no privilege), item rows still print Item + Qty; the
  four money columns render empty (matching current behaviour) — bill still prints.
- No selected format key true → nothing prints; defaults guarantee FiveFive is on, so this
  can only happen if an admin unchecks all three. Acceptable (matches existing multi-key
  pattern app-wide).

## Testing

Manual E2E via Playwright against local Payara (`http://localhost:9090/rh`), per
`developer_docs/testing/playwright-e2e-workflow.md`:

1. Direct-issue a couple of items to an inpatient on `pharmacy_bill_issue_bht.xhtml`,
   settle, and verify the preview prints with per-item + bill-level Gross/Service
   Charge/Discount/Net under each of A4, FiveFive, POS (toggle via gear).
2. From the reprint page (#22035), open View Bill and confirm Item + Qty are present
   (the original bug), the margin column reads "Service Charge", and the printed bill shows
   all four columns in each format.
3. Process a return and reprint; confirm return bill shows the four columns and correct
   (positive-magnitude) values.
4. Regression: open `pharmacy_bill_retail_sale_native.xhtml` and confirm its printed sale
   bill is **unchanged** (shared composites untouched).

## Files touched (summary)

- **New:** 6 composite `.xhtml` (3 DTO + 3 entity) + up to 3 small CSS (or reuse existing).
- **Edit:** 4 page `.xhtml` (gear button + dialog + new panelGroups; relabel "Matrix Value"
  → "Service Charge" and add Gross/Discount columns on the reprint View Bill table).
- **Edit:** 1 config controller (3 boolean props + load/save).
- No Java query/DTO/facade changes.

## Out of scope (deliberate, YAGNI)

- Retiring old `FiveFiveCustom3` / `PosHeaderPaper` composites.
- Consolidating the deprecated `pharmacyBillPaperType` chooser into the new gear dialog.
- Any change to retail/wholesale sale bills.
