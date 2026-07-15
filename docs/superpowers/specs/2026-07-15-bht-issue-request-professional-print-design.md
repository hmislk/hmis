# BHT Pharmacy Issue Request — Professional POS/FiveFive/A4 Print

**Date:** 2026-07-15

## Problem

The inpatient pharmacy issue **request** document (nurse → pharmacy) has no
professional print formats and no print-config button, unlike sibling bills such as
the Inward BHT Direct Issue Bill. It is shown/printed identically in three places, all
sharing one plain, single-format composite (`ph:pharmacy_bht_issue_request_receipt`):

1. **Create/settle page** — `src/main/webapp/ward/ward_pharmacy_bht_issue_request_bill.xhtml`
   (controller `pharmacyRequestForBhtController`) — Print button after settling a request.
2. **Reprint page** — `src/main/webapp/ward/ward_pharmacy_reprint_bht_issue_request.xhtml`
   (controller `pharmacyBillSearch`) — Reprint button.
3. **Pharmacist's issue page** — `src/main/webapp/ward/ward_pharmacy_bht_issue.xhtml`
   (controller `pharmacySaleBhtController`) — read-only "Original Request" reference
   dialog (no print button here; confirmed via grep as the only 3 usages of the old
   composite).

There is no POS/FiveFive/A4 choice and no gear/settings button anywhere on this
document family.

## Goal

One new professional-print composite, usable from all three places, offering
POS/FiveFive/A4 layouts, plus a print-config gear button/dialog to choose the active
format(s) — following the established pattern in
`developer_docs/configuration/printer-configuration-system.md` (as used by the Inward
BHT Direct Issue Bill, issue #22035) and the DTO/native-SQL pattern in
`developer_docs/billing/native-sql-print-page-guide.md`.

## Scope

**In scope:**
- New DTO pair + native-SQL service to load the print data.
- One new receipt composite (single file, three internal format blocks: POS/FiveFive/A4).
- One new print-config gear-button+dialog composite (single file, self-contained).
- Wiring all 3 existing pages/controllers to the new composites.
- New content vs. today: a status badge (Pending/Completed/Cancelled), a DUPLICATE
  watermark on reprint, and blank "Requested By / Issued By" signature lines.
- Deleting the old entity-based composite `pharmacy_bht_issue_request_receipt.xhtml`
  once all 3 call sites are migrated.

**Explicitly out of scope:**
- Any change to the actual request/issue business logic (creation, completion,
  cancellation).
- Any change to other pharmacy bill families (Direct Issue, Transfer Issue, GRN, etc.).
- A "Legacy View" fallback page/link — unlike the PO native-SQL migration, this feature
  is not creating a new parallel page reached through `BillSearch`'s atomic-type
  routing; it replaces the print markup in place on 3 existing pages. There is nothing
  to route between.

## Data model

Backed by `com.divudi.core.entity.Bill` (`BillTypeAtomic.INWARD_PHARMACY_REQUEST`) and
its `billItems` (`com.divudi.core.entity.BillItem`). Fields already surfaced by the
existing composite (source: `resources/pharmacy/pharmacy_bht_issue_request_receipt.xhtml`):

- Header: `fromDepartment` (printingName, address, telephone1/2, fax, name),
  `toDepartment.name`, `deptId` (Request No), `createdAt`, `patientEncounter`
  (bhtNo, patient.person.nameWithTitle, patient.phn, currentPatientRoom.roomFacilityCharge.name),
  `creater.webUserPerson.nameWithTitle`, `creater.name`, `comments`, `cancelled`.
- New field needed: `completed` (boolean — added recently per "auto-complete BHT
  request on full issuance" work) for the status badge.
- Items: `item.name`, `qty`, `descreption` (directions — the intentional entity typo,
  per project convention, is preserved at the DB/entity level; the DTO field itself
  may be named normally).

## Components

### DTOs — `com.divudi.core.data.dto.pharmacy`

- `BhtIssueRequestPrintDto` — header-level fields listed above (primitives/String/Date
  per the native-SQL guide's rules, never null — `""`/`0`/`false` defaults), plus
  `List<BhtIssueRequestItemPrintDto> items = new ArrayList<>()`.
- `BhtIssueRequestItemPrintDto` — item name, qty, directions.

Plain Java beans, no JPA annotations, per guide Step 2.

### Service — `com.divudi.service.pharmacy.BhtIssueRequestNativeSqlService`

- `@Stateless`, `@PersistenceContext(unitName = "hmisPU") EntityManager em`.
- One method: `BhtIssueRequestPrintDto loadPrintDtoByBillId(long billId)`.
- Two-query pattern (header, items), **LEFT JOIN** on every FK, positional params
  (`?1`, `?2`, ...), `str()`/`toDate()`/`toBool()` null-safety helpers, per guide Step 3.
- Verify `Department.site` → `institution` table and any other non-obvious FK types
  before writing joins (per guide's join-table table).
- Wrapped in try/catch, returns `null` on any exception.

### Composite 1 — the receipt

`src/main/webapp/resources/pharmacy/print/bht_issue_request_receipt.xhtml`
(namespace `xmlns:phprint="http://xmlns.jcp.org/jsf/composite/pharmacy/print"`, tag
`phprint:bht_issue_request_receipt`).

Interface:
- `dto` — `BhtIssueRequestPrintDto`, required.
- `duplicate` — boolean, optional (default false) — shows a DUPLICATE watermark.
- `comment` — String, optional — overrides `dto.comments` if provided (mirrors current
  `cc.attrs.comment` vs `bill.comments` precedence).

Implementation: reads the 3 format config flags directly
(`configOptionApplicationController.getBooleanValueByKey(...)`) and renders exactly one
of three `h:panelGroup` blocks:

- **A4** — full formal layout: letterhead-style header (institution name/address/
  contact), full-width details table, item table, bill-level item count, signature
  lines (`Requested By _____________` / `Issued By _____________`), footer.
- **FiveFive** — half-page compact layout, same data, tighter spacing.
- **POS** — narrow thermal layout, minimal chrome, compact item list.

Each block includes the status badge (`Pending` / `Completed` / `Cancelled`, derived
from `dto.completed`/`dto.cancelled`) and the DUPLICATE watermark when
`cc.attrs.duplicate eq true`. Existing header/footer CSS/HTML config keys
(`'Pharmacy BHT Issue Request Receipt CSS'`, `'...Header'`, `'...Footer'`) are reused
as-is inside each block, consistent with today.

### Composite 2 — print-config gear button

`src/main/webapp/resources/pharmacy/print/bht_issue_request_print_config_button.xhtml`
(same namespace, tag `phprint:bht_issue_request_print_config_button`).

No required attributes — fully self-contained: a gear `p:commandButton` gated by the
existing `ChangeReceiptPrintingPaperTypes` privilege, opening a `p:dialog` with three
`h:selectBooleanCheckbox` (A4 / FiveFive / POS), an "open" `p:ajax` calling
`pharmacyConfigController.loadCurrentConfig()`-equivalent, and an `ajax="false"` Save
button. Reuses the existing `pharmacyConfigController` (`@Named @ViewScoped`) bean
rather than introducing a new controller, matching the precedent already established
for the Direct Issue Bill's format flags in that same class.

**New fields/methods on `PharmacyConfigController`:**
```java
private boolean bhtIssueRequestReceiptA4;
private boolean bhtIssueRequestReceiptFiveFive;
private boolean bhtIssueRequestReceiptPos;
// loadBhtIssueRequestReceiptConfig() — reads via configOptionApplicationController.getBooleanValueByKey
public void saveBhtIssueRequestReceiptConfig() {
    configOptionApplicationController.setBooleanValueByKey("Pharmacy BHT Issue Request Receipt is A4", bhtIssueRequestReceiptA4);
    configOptionApplicationController.setBooleanValueByKey("Pharmacy BHT Issue Request Receipt is FiveFive", bhtIssueRequestReceiptFiveFive);
    configOptionApplicationController.setBooleanValueByKey("Pharmacy BHT Issue Request Receipt is POS", bhtIssueRequestReceiptPos);
    // reload + JsfUtil success message
}
```

**New config keys** (application-wide, matching the naming already used for this same
document's CSS/Header/Footer keys):
- `Pharmacy BHT Issue Request Receipt is A4` (default false)
- `Pharmacy BHT Issue Request Receipt is FiveFive` (default true — current de-facto format)
- `Pharmacy BHT Issue Request Receipt is POS` (default false)

## Wiring per page

- **`ward_pharmacy_bht_issue_request_bill.xhtml`**: in the post-settle "View Bill"
  panel, replace `<ph:pharmacy_bht_issue_request_receipt .../>` with
  `<phprint:bht_issue_request_receipt dto="#{pharmacyRequestForBhtController.bhtIssueRequestPrintDto}" comment="#{pharmacyRequestForBhtController.comment}"/>`,
  and add `<phprint:bht_issue_request_print_config_button/>` next to the existing Print
  button. Keep the existing `gpBillPreview` panel/`p:printer` wiring on the page as-is.
- **`ward_pharmacy_reprint_bht_issue_request.xhtml`**: replace the composite with
  `<phprint:bht_issue_request_receipt dto="#{pharmacyBillSearch.bhtIssueRequestPrintDto}" duplicate="true"/>`
  and add the config-button composite next to the Reprint button.
- **`ward_pharmacy_bht_issue.xhtml`**: replace the "Original Request" dialog's
  composite with `<phprint:bht_issue_request_receipt dto="#{pharmacySaleBhtController.bhtIssueRequestPrintDto}"/>`.
  No config-button composite here (no print action on this page).

**Controller changes** (`PharmacyBillSearch`, `PharmacyRequestForBhtController`,
`PharmacySaleBhtController`, all already `@SessionScoped`): each gets an injected
`@EJB BhtIssueRequestNativeSqlService` and a cached `BhtIssueRequestPrintDto` field,
populated once at the point each controller already loads/sets its respective bill for
this page (exact existing method to hook into — e.g. the reprint page's bill-loading
navigation method, the settle method that sets `printBill`, and the issue page's
`bhtRequestBill` setter — to be confirmed by reading each method during
implementation), not lazily inside the getter, to avoid repeated queries across
multiple EL evaluations per render.

## Error handling / edge cases

- Service returns `null` on exception or missing bill → page shows a `JsfUtil` error
  message; composite is not rendered (`dto` attribute absent/null guards the panel).
- Null FKs (no room assigned, no fax, empty comments) → LEFT JOINs + `str()` helper →
  empty string, never a blank/broken page.
- No format key enabled → nothing prints (matches the existing multi-key pattern
  app-wide); default `FiveFive=true` guarantees this only happens if an admin unchecks
  all three deliberately.
- Cancelled requests: status badge shows `Cancelled` regardless of format.

## Testing

Manual E2E via Playwright against local Payara, per
`developer_docs/testing/playwright-e2e-workflow.md`:

1. Create a BHT pharmacy issue request, settle it, verify the print preview renders
   correctly for each of A4/FiveFive/POS (toggle via the new gear dialog), including
   status badge and signature lines.
2. Open the reprint page for the same request and verify the DUPLICATE watermark
   appears and the same 3 formats render identically to the settle-page preview.
3. As a pharmacist, open the issue page for this request and confirm the "Original
   Request" dialog still displays correctly (no config button, no watermark).
4. Cancel a request and reprint; confirm the status badge shows `Cancelled`.
5. Mark a request as completed (via the existing Mark as Complete flow) and reprint;
   confirm the status badge shows `Completed`.
6. Confirm the gear dialog's format toggles persist across a page reload
   (application-wide config).

## Files touched (summary)

- **New:** 2 DTO classes, 1 native-SQL service, 2 composite `.xhtml` files (+ CSS as
  needed, reusing existing config-driven CSS keys where possible).
- **Edit:** 3 page `.xhtml` files (swap composite, add gear button where applicable),
  3 controllers (`PharmacyBillSearch`, `PharmacyRequestForBhtController`,
  `PharmacySaleBhtController` — inject service, cache DTO), 1 config controller
  (`PharmacyConfigController` — 3 boolean props + load/save).
- **Delete:** `resources/pharmacy/pharmacy_bht_issue_request_receipt.xhtml` (old
  entity-based composite), once all 3 call sites are migrated.

## Out of scope (deliberate, YAGNI)

- A dedicated native-SQL controller class / BillSearch atomic-type routing (guide
  Steps 4 and 7) — not applicable since no new parallel page is being created.
- A "Legacy View" toggle button.
- Department-specific (vs. application-wide) format config.
- Any change to the underlying request/issue/return business logic.
