# Native Purchase Order Request — Design

**Issue:** [#22727](https://github.com/hmislk/hmis/issues/22727)
**Master issue:** [#22726](https://github.com/hmislk/hmis/issues/22726) — Phase 1 of the
Purchase Order native migration.
**Date:** 2026-08-07
**Branch:** `22727-native-po-request`

---

## 1. Goal

Replace the write path of the Purchase Order Request page
(`pharmacy_purhcase_order_request.xhtml` / `PurchaseOrderRequestController`) with a
native-SQL implementation, mirroring what `RetailSaleNativeSqlController` /
`RetailSaleNativeSqlService` did for pharmacy retail sale (#20260).

**Requirement: 100% functional replication.** Every button, validation message, and
bill-number strategy on the legacy page carries over. Nothing is deferred to a
follow-up. Any legacy behavior this spec does not explicitly mention is still in
scope — the Playwright + second-agent review in §7 exists specifically to catch
omissions.

This phase does **not** touch:
- `PurchaseOrderController` / the Approving page (Phase 2, #22726)
- The List-to-Finalize or List-to-Approve list/search pages — both already query via
  JPQL DTO constructors (`PharmacyPurchaseOrderDTO`), not raw entity graphs

---

## 2. What the Request page actually does

Established by reading `PurchaseOrderRequestController.java` in full.

**Bill lifecycle** — single bill, two atomic states, same row:
- Created with `BillTypeAtomic.PHARMACY_ORDER_PRE` on first save (`saveBill()`)
- Draft may be saved repeatedly (`saveRequest()` → `saveRequestWithoutMessage()` →
  `saveBill()` + `saveBillComponent()`) — each call is create-if-new /
  update-if-existing on the *same* bill row, not a new bill
- Promoted in place to `BillTypeAtomic.PHARMACY_ORDER` on `finalizeRequest()` →
  `finalizeBill()` + `finalizeBillComponent()`
- No second bill is created at this phase — the requestedBill↔approvedBill two-bill
  cross-link happens entirely in Phase 2 (`PurchaseOrderController.approve()`)

**Guards enforced before any write** (must all be replicated):
- `saveRequestWithoutMessage()`: cannot save once `currentBill.isChecked()`
  (finalized); supplier (`toInstitution`) must be set
- `finalizeRequest()`: supplier set; not already finalized; payment method set;
  at least one bill item; every item has qty+price (`allBillItemsValid`); recalculated
  total unit count > 0
- `addItem()`: item selected; bill's `departmentType` auto-set from first item or
  defaulted to `Pharmacy`; subsequent items must match bill's `departmentType`;
  department type must be in `sessionController.getAvailableDepartmentTypesForPharmacyTransactions()`;
  duplicate-item check gated on config `Prevent Duplicate Items in Purchase Orders`
- `onEdit()`: integer-only qty/free-qty gated on config
  `Pharmacy Purchase - Quantity Must Be Integer` (default true)
- Privilege checks: `PurchaseOrderSave` (save/remove), `PurchaseOrderFinalize` (finalize)

**Line items:**
- `addItem()` — single add, auto-fills last purchase/retail rate via
  `applyLastRatesToBillItem()`, sets serial number
- `removeItem()` — soft-retires the `BillItem` + its `PharmaceuticalBillItem`
  (`retired=true`, retirer, retiredAt); saves the request first
  (`saveRequestWithoutMessage()`) so the retiring edit lands on a persisted row;
  reloads `currentBill` via `billService.reloadBill()` afterward
- `onEdit()` / `calculateLineValues()` — recomputes line gross/net from user-entered
  qty/free-qty/rate
- `generateBillComponentsForAllSupplierItems(items)` — bulk add: dedupes against
  existing non-retired lines (same config gate), fetches last purchase/retail rates in
  batch (`fetchLastPurchaseRatesForItems` / `fetchLastRetailRatesForItems`), assigns
  serial numbers continuing from current list size
- `addAllSupplierItems()` / `addAllSupplierItemsBelowRol()` — two entry points into the
  bulk-add above, one unconditional, one filtered to items below reorder level
- `finalizeBillComponent()` — on finalize, retires any line whose combined
  qty+free-qty units is `<= 0` (comment: "Retired at Finalising PO"); sets
  `remainingQty`/`remainingFreeQty` on the surviving `PharmaceuticalBillItem` to the
  finalized qty/free-qty; accumulates `totalBillItemsCount`
- `resyncPharmaceuticalBillItemIfEmpty()` — issue #21417 guard: if a duplicate-removal
  left a `BillItem` with an all-zero PBI but its `BillItemFinanceDetails` still holds
  real quantities, rebuild the PBI from BIFD before persisting. **Must be replicated
  exactly** — this fixed a real production data-integrity bug

**Bill-number generation** (`createAndAssignBillNumber()`) — 4 independently
config-gated strategies for `deptId`, 1 for `insId`, with a legacy default fallback
for each. All must be replicated verbatim; this is exactly the kind of intentional,
configuration-driven branching CLAUDE.md's "never fix intentional complexity" rule
protects.

**Email** (`prepareEmailDialog()` / `sendPurchaseOrderEmail()`) — sends the PO to
the supplier's configured recipient.

**Totals** (`calculateBillTotals()`) — bill-level aggregate from line items.

**Concurrency guards already in the legacy code** (must carry over, not redesigned):
- `saveRequestWithoutMessage()` and `approve()`-adjacent methods are `synchronized` —
  fixed issue #21417 (duplicate-submit double-persisted the same in-memory BillItem).
  The native service's save/finalize entry points need the equivalent protection.

---

## 3. Approach

**Chosen: new native service + controller, following the `PurchaseOrderNativeSqlService`
package/DTO conventions already established by #20923, using
`RetailSaleNativeSqlService` as the native-SQL-mechanics reference.**

- New `PurchaseOrderRequestNativeSqlService` (`@Stateless`,
  `com.divudi.service.pharmacy`) — owns native INSERT/UPDATE for:
  - Bill row create (draft) / update (repeated draft save) / promote-in-place
    (finalize)
  - BillItem + PharmaceuticalBillItem INSERT/UPDATE per line, including the
    resync-if-empty guard
  - Bill-number assignment (reuses `BillNumberGenerator` — JPA-backed sequence
    logic, not something to reimplement in raw SQL)
- New `PurchaseOrderRequestNativeSqlController` (`@Named`, session-scoped to match
  `PurchaseOrderNativeSqlController`'s existing scope choice for this bill family) —
  owns validation/guard messages, item list state, autocomplete delegation, email
  dialog state
- New `pharmacy_purhcase_order_request_native.xhtml` — copies the legacy page's
  layout and buttons 1:1, swapping entity EL bindings for the new controller

Rationale:

- There is no existing native *write* path for POs to extend — #20923/PR #20925 only
  covers the read-only print page. Building fresh, closely modeled on
  `RetailSaleNativeSqlService`'s INSERT/UPDATE mechanics, is the only option; there is
  nothing to "convert in place."
- Reusing `BillNumberGenerator` (not reimplementing bill-number sequences in native
  SQL) follows the retail-sale precedent, where IDENTITY-PK-dependent or
  sequence-dependent logic stayed on JPA (see migration guide §"Entities Written Per
  Transaction").
- The single-bill-row lifecycle here (no PreBill/BilledBill pair at this phase) is
  simpler than retail sale's two-bill settle, so the migration guide's harder rules
  (L2 cache eviction, cross-link via merge) are Phase 2's concern, not this one —
  though this phase must still evict `Bill`/`BillItem`/`PharmaceuticalBillItem` from
  L2 cache after any native UPDATE, per the guide's common-mistake #6.

Rejected alternatives:

- **Convert `PurchaseOrderController`'s existing native read-path service to also
  handle writes.** Read and write concerns for different bill states (`PHARMACY_ORDER`
  read-only print vs `PHARMACY_ORDER_PRE`/`PHARMACY_ORDER` in-progress draft) don't
  share a natural home; conflating them risks the read service picking up
  transactional side effects it wasn't designed for.
- **Rewrite `PurchaseOrderRequestController` in place, swapping JPA calls for native
  SQL line-by-line.** Loses the ability to keep the legacy page live as a fallback
  during review (the retail-sale and PO-print precedents both keep a "Legacy View"
  escape hatch until the native path is proven).

---

## 4. Function inventory (legacy → native, for the second-agent review)

| Legacy method | Native equivalent | Notes |
|---|---|---|
| `saveBill()` | `PurchaseOrderRequestNativeSqlService.saveDraft()` | create-or-update same bill row |
| `saveBillComponent()` | same service, item INSERT/UPDATE loop | |
| `saveRequest()` / `saveRequestWithoutMessage()` | controller method, same guards | supplier-set, not-finalized checks |
| `createAndAssignBillNumber()` | unchanged — delegates to `BillNumberGenerator` | not reimplemented in SQL |
| `addItem()` | controller method, same guards | department-type matching, duplicate check |
| `removeItem()` | controller method + native UPDATE (soft retire) | must save draft first, same as legacy |
| `onEdit()` / `calculateLineValues()` | controller method, same math | |
| `generateBillComponentsForAllSupplierItems()` | controller method | batch rate fetch |
| `addAllSupplierItems()` / `addAllSupplierItemsBelowRol()` | controller methods | |
| `finalizeBill()` / `finalizeRequest()` | `PurchaseOrderRequestNativeSqlService.finalize()` | in-place promote, same guards |
| `finalizeBillComponent()` | same service | zero-qty retire, remainingQty set |
| `resyncPharmaceuticalBillItemIfEmpty()` | same service, ported verbatim | issue #21417 fix — do not drop |
| `prepareEmailDialog()` / `sendPurchaseOrderEmail()` | controller methods, unchanged (JPA/email infra, not settle-hot-path) | |
| `calculateBillTotals()` | controller method, same math | |
| `displayItemDetails()` / `closeItemHistory()` | controller methods, unchanged | |
| Privilege checks (`PurchaseOrderSave`, `PurchaseOrderFinalize`) | same checks in new controller | |
| `synchronized` guards on save/finalize | same on native controller/service entry points | issue #21417 pattern |

---

## 5. Data written (native SQL)

Only the `bill`, `billitem`, `pharmaceuticalbillitem` tables are touched at this
phase — there is no `billitemfinancedetails`/`billfinancedetails`/payment/stock
involvement yet (POs don't move stock or money until GRN/approval). This is a
narrower write surface than the retail-sale settle path.

After any native INSERT/UPDATE, evict from L2 cache: `Bill`, `BillItem`,
`PharmaceuticalBillItem` (guide's common-mistake #6).

---

## 6. Error handling

All validation stays as user-facing `JsfUtil` error messages, matching legacy
wording exactly where a message is user-visible (some are typo'd in the legacy code,
e.g. "Please selectr a supplier" in `finalizeRequest()` — CLAUDE.md's backward-
compatibility rule about not "fixing" intentional artifacts extends here: fix obvious
typos in *new* code, but do not treat matching legacy message text as optional).

---

## 7. Testing

- Playwright: create new PO request → add item (single) → add item (bulk, all
  supplier items) → add item (bulk, below ROL) → edit line qty/rate → remove a line
  → save draft (twice, confirm same bill row updates, not duplicated) → set payment
  method → finalize → confirm zero-qty lines retired → email → confirm List-to-
  Finalize picker still opens this bill correctly for further edits pre-finalize
- DB verification: `bill` row (single row, `BILLTYPEATOMIC` transitions
  `PHARMACY_ORDER_PRE` → `PHARMACY_ORDER`), `billitem`/`pharmaceuticalbillitem` rows
  (no duplicates from the #21417 double-submit pattern), bill-number columns for at
  least the default strategy
- Second-agent review against §4's function inventory before this phase is
  considered done and Phase 2 begins

---

## 8. Out of scope / explicitly deferred

- Approving page (`PurchaseOrderController`) — Phase 2, separate spec
- List-to-Finalize / List-to-Approve pages — already DTO-based, not touched
- Multi-window concurrency (numbered controller copies) — not needed; PO request has
  no multi-cashier-window precedent to match, unlike retail sale
