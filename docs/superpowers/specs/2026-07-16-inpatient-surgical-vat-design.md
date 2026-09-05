# VAT on Services/Investigations — Inpatient & Surgical Dashboards

## Context

The inpatient dashboard and surgical dashboard both let staff add services/investigations
to a bill through the shared `BillBhtController` (`addToBill()` for inpatient,
`addToBillSurgery()` for surgical, both funnelling into `calTotals()`). Neither path
calculates VAT today, even though the data model already has dormant VAT fields:

- `Item.vatable` / `Item.vatPercentage` — exists, but `getVatPercentage()` is hard-coded
  to return `0` (dead field).
- `Bill.vat` / `Bill.vatPlusNetTotal` — exist, populated only when copying/inverting bills,
  never computed from item data.
- `BillItem.vat` / `BillItem.vatPlusNetValue` — same story.
- Admin "Manage Items" page (`admin/items/items.xhtml`) has a VAT Percentage / VATable tab
  already built but hidden behind `rendered="false"` with the tab titled "Depricated".
- `A4_paper_with_headings.xhtml` print template already has a VAT row wired to `bill.vat`,
  gated by a copy-pasted condition (`bill.discount ne 0.0`) that doesn't make sense for VAT.

This feature revives and wires together that existing skeleton rather than inventing a new
one, and scopes the calculation to the inpatient + surgical "add services" flows only (not
OPD, channel booking, or pharmacy, which are separate billing paths on the same `Item`
entity and are out of scope for this change).

## Decisions

1. **Scope**: `BillBhtController.addToBill()`, `addToBillSurgery()`, `calTotals()` only.
   Covers both `inward_bill_service.xhtml` (inpatient) and `inward_bill_surgery_service.xhtml`
   (surgical) since they share the controller.
2. **Data field**: Fix and reuse `Item.vatable` + `Item.vatPercentage` (fix the broken
   getter). Un-deprecate and redesign the admin UI tab rather than introduce a competing
   field.
3. **VAT basis**: Computed on **net value after discount**: `vat = netValue * item.vatPercentage / 100`.
4. **Gate**: VAT applies only when **both** `item.vatable == true` **and**
   `item.vatPercentage > 0`.
5. **Rate snapshot**: Add a new `BillItem.vatPercentage` field that stores the rate actually
   applied at billing time (independent of `BillItem.vat`, the computed amount). This keeps
   historical bills/prints/reports accurate if an item's VAT % changes later. Reuses the
   existing bare-field pattern already used for `BillItem.vat` rather than the optional/lazy
   `BillItemFinanceDetails` generic tax bucket.
6. **Bill-level rollup**: `Bill.vat` = sum of `BillItem.vat` across all bill items.
   `Bill.vatPlusNetTotal` = `Bill.netTotal + Bill.vat`. `Bill.netTotal` itself is **not**
   changed to include VAT — it stays a pure net figure, per the original ask.
7. **Rounding**: Round each `BillItem.vat` to 2 decimal places at calculation time, matching
   existing currency-rounding conventions used elsewhere in `BillBhtController` totals.
8. **UI display**: Both the item-add tables and their prints show:
   - a per-line **VAT** column (each `BillItem.vat`)
   - bill-summary rows for **Net Total**, **VAT**, and **Net Total + VAT**
9. **Admin item management**: Re-enable the existing hidden VAT tab on
   `admin/items/items.xhtml`, redesigned per the `ui-guidelines` skill (proper panel/tab
   layout, not the old markup verbatim).
10. **Item management REST API**: Add `vatable` + `vatPercentage` to the Service and
    Investigation request/response DTOs and to `ServiceApiService`/`InvestigationApiService`
    create/update mapping, per the `api-development` skill checklist.
11. **AI chat**: Extend the existing `manage_investigations` tool with `vatable`/
    `vatPercentage` parameters. No `manage_services` AI tool exists today (services were
    never wired into `AnthropicApiService` at all) — add a new `manage_services` tool
    mirroring `manage_investigations`'s shape (including the new VAT params) so VAT parity
    exists between investigations and services in AI chat, and register it in
    `CapabilityStatementResource`.
12. **Prints**: Fix the `A4_paper_with_headings.xhtml` VAT row's render condition (currently
    incorrectly gated on `bill.discount ne 0.0`) and add a per-line VAT column there and in
    the five-five and POS inward-service print variants.

## Out of scope

- OPD billing, channel booking, pharmacy billing — even though they use the same `Item`
  entity, wiring VAT into those flows is a separate change.
- The older bulk "VAT management" page (`dataAdmin/item_list_for_vat.xhtml`) — left as-is;
  it already edits the same `Item.vatable`/`vatPercentage` fields this design fixes, so it
  benefits for free once the getter bug is fixed.
- `InwardChargeType.VAT` (VAT-as-a-manual-charge-line pattern used elsewhere in inward
  billing) is a different, pre-existing mechanism and is not touched by this change.
- `BillFinanceDetails`/`BillItemFinanceDetails` generic tax framework — not used by this
  feature (see decision 5).

## Database migration

New column `bill_item.vat_percentage` (double, default 0) needs a migration script following
the project's cross-deployment case-sensitivity rules (`INFORMATION_SCHEMA` detection,
prepared statements) per `developer_docs/database/migration-development-guide.md`.
`item.vatable` and `item.vat_percentage` columns already exist (dormant field), no migration
needed there.
