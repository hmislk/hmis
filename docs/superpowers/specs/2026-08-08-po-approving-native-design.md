# Native Purchase Order Approving — Design

**Issue:** [#22738](https://github.com/hmislk/hmis/issues/22738)
**Master issue:** [#22726](https://github.com/hmislk/hmis/issues/22726) — Phase 2 of the
Purchase Order native migration.
**Date:** 2026-08-08
**Branch:** `22738-native-po-approving`

---

## 1. Goal

Replace the write path of the Purchase Order Approving page
(`pharmacy_purhcase_order_approving.xhtml` / `PurchaseOrderController`) with a
native-SQL implementation, following the pattern established by Phase 1
(`PurchaseOrderRequestNativeSqlController` / `PurchaseOrderRequestNativeSqlService`,
issue #22727, PR #22733).

**Requirement: 100% functional replication.** Every button, validation message,
and guard on the legacy page carries over. Nothing is deferred to a follow-up.
Any legacy behavior this spec does not explicitly mention is still in scope —
the Playwright + second-agent review in §7 exists specifically to catch
omissions.

This phase does **not** touch:
- `PurchaseOrderRequestController` / the Request page (Phase 1, done)
- The List-to-Finalize or List-to-Approve list/search pages' own query logic —
  both already query via JPQL DTO constructors (`PharmacyPurchaseOrderDTO`).
  Only the List-to-Approve page's "Approve" button wiring changes (§6).

---

## 2. What the Approving page actually does

Established by reading `PurchaseOrderController.java` and
`pharmacy_purhcase_order_approving.xhtml` in full.

**Bill lifecycle** — two bills, cross-linked:
- `requestedBill` — the finalized PO Request bill (`BillTypeAtomic.PHARMACY_ORDER`),
  loaded read-only; never mutated except for the cross-link write at approve time
- `approvedBill` — a new `BilledBill` created on `navigateToPurchaseOrderApproval()`,
  seeded from the request bill's lines, editable in memory. Unlike the Request
  page, there is no repeated "Save Draft" — the page goes straight from
  item-editing to a single `approve()` call that both saves and finalizes in
  one step. (`saveBill()` + `saveBillComponent()` are internal helpers called
  only from `approve()`, not separate page actions.)
- Cross-link: on successful approve, `requestedBill.referenceBill = approvedBill`,
  persisted via `billFacade.edit(requestedBill)` — **JPA merge, never native
  SQL**, per the master issue's L2-cache-coherence rule (#22726 decisions).

**Guards enforced before any write** (must all be replicated):
- `navigateToPurchaseOrderApproval()`: reject if `requestedBill.getReferenceBill() != null`
  (already approved) — checked again in `approve()` since the two calls can
  race (see concurrency note below)
- `approve()`: requires `APPROVE`/`PurchaseOrdersApprovel` privilege; payment
  method set; at least one bill item; **every line** must have a non-null PBI,
  `qty + freeQty > 0`, and `purchaseRate > 0` (all-or-nothing — one bad line
  blocks the whole approve, unlike Request's finalize which auto-retires
  zero-qty lines)
- `onEdit()`: integer-only qty/free-qty gated on config
  `Pharmacy Purchase - Quantity Must Be Integer` (default true) — identical
  gate to Phase 1's Request page

**Concurrency guards already in the legacy code** (must carry over verbatim —
these fixed a real production incident, not defensive boilerplate):
- Both `navigateToPurchaseOrderApproval()` and `approve()` are `synchronized`.
  Per the legacy code comment: the Approve button posts directly with no
  confirm-then-review gap large enough to matter, but a double-click on
  **either** button race two calls through the same session-scoped
  `billItems`/`generateBillComponent()` state, duplicating every line. This
  reproduced the GRN item duplication reported by Ruhunu
  (PO/RH/GSK/26/01093), the same bug class as the Request page's #21417 guard.
  The native controller's equivalent entry points need the same
  `synchronized` + already-approved re-check.

**Line items:**
- `removeItem()` / `removeSelected()` — in-memory only; no `saveRequestWithoutMessage()`-
  style intermediate persistence step exists here (there is no draft-save
  concept on this page) — removal just mutates the session's `billItems` list;
  the removed lines are simply never included when `approve()` eventually
  calls `saveBillComponent()`
- `onEdit()` / `calculateLineValues()` / `updateCalculatedValues()` — recomputes
  line gross/net from user-entered qty/free-qty/rate; `updateCalculatedValues()`
  is a lighter path used when BIFD is already populated (skips resetting audit
  fields) — this optimization is legacy-only bookkeeping and does not need a
  native equivalent (see §3 rationale)
- `generateBillComponent()` — seeds `billItems` from the request bill's
  non-retired `PharmaceuticalBillItem`s on page entry; copies ~25 individual
  `BillItemFinanceDetails` fields verbatim per line (see §3 for the native
  approach, which recomputes instead of copying)
- On `approve()`, `saveBillComponent()` retires any line with
  `qty + freeQty <= 0` (comment: "Retired at Approving PO") and sets
  `remainingQty`/`remainingFreeQty` to the approved qty/freeQty on survivors —
  this is a **narrower guard than it looks**: the earlier all-or-nothing
  validation loop in `approve()` already rejects zero-qty lines outright, so
  this retire branch is effectively dead in the current legacy flow. Must
  still be replicated (defense-in-depth, matches Request page's equivalent
  sweep) but is not reachable via the UI's own validation.

**Bill-number generation** (`approve()`, inline) — 3 independently config-gated
strategies for `deptId`, 1 for `insId`, with a legacy default fallback for
each — same shape as Phase 1's `createAndAssignBillNumber()`, different
`BillTypeAtomic`/config-key strings (`PHARMACY_ORDER_APPROVAL`, "POA" default
suffix). Must be replicated verbatim.

**Email** (`prepareEmailDialog()` / `sendPurchaseOrderEmail()` /
`generatePurchaseOrderHtml()`) — sends the **approved** bill to the supplier.
Same structure as Phase 1's Request-page email, including the same HTML-escaping
requirement (Phase 1's PR review caught unescaped free-text fields in the
email body — apply `esc()` here from the start, not as a follow-up fix).

**Totals** (`calculateBillTotals()`) — bill-level aggregate from line items,
also renumbers `searialNo` for surviving lines (same in-memory-only gap Phase 1
had before its review fix — see §3, this phase persists it from the start).

**Print** (`printPreview` flag, 4 config-gated paper formats via
`ph:po_custom_*` composite components) — read-only rendering, reuses the
existing composites unchanged; not part of the write-path migration.

---

## 3. Approach

**New `PurchaseOrderApprovingNativeSqlService` (`@Stateless`,
`com.divudi.service.pharmacy`) + `PurchaseOrderApprovingNativeSqlController`
(`@Named @SessionScoped`, `com.divudi.bean.pharmacy`)**, following Phase 1's
package/naming/DTO conventions.

### Naming

Per your convention: `Service` suffix for `@Stateless` EJBs, `Controller`
suffix for `@Named` CDI beans. `PurchaseOrderNativeSqlController`/`Service`
(#20923) are already taken by the read-only print path — this phase's classes
are named `PurchaseOrderApprovingNativeSqlController`/`Service` to avoid
collision and match the page's own name.

### Service responsibilities

- `createApprovedBill(...)` — native INSERT for the approved bill row
  (mirrors Phase 1's `createDraftBill`, different `BillTypeAtomic`/fields:
  `referenceBill_ID`/`backwardReferenceBill_ID` point at the request bill,
  set at insert time since — unlike Phase 1 — there is no repeated-save
  lifecycle to worry about re-reading stale FKs across)
- `saveApprovedLine(...)` — native INSERT/UPDATE for `billitem` +
  `pharmaceuticalbillitem`, mirroring Phase 1's `saveLine()` exactly,
  including the upsert-by-existence fix from Phase 1's review (never branch
  on a DTO id the caller can't reliably supply back)
- `retireZeroQtyApprovedLines(...)` — mirrors Phase 1's
  `retireZeroQtyLines()`, reusing its package-private `isZeroQtyLine()`
  predicate the same way `computeLineValues()` is reused; kept even though
  current UI validation makes it unreachable, per §2
- `loadRequestedLines(requestedBillId)` — **new for this phase**: a JPQL
  `SELECT NEW` projection reading the request bill's non-retired
  `PharmaceuticalBillItem`s, returning only: `itemId`, `ampp` (boolean),
  `quantity`, `freeQuantity`, `purchaseRate` (line gross rate), `retailRate`,
  `unitsPerPack`, `serialNo`. This is Phase 1's `PurchaseOrderRequestLineData`
  shape, reused as the read-side projection too (see decision below).
- `computeLineValues(...)` — **reused from Phase 1**. It is currently
  package-private on `PurchaseOrderRequestNativeSqlService` (visible to
  `PurchaseOrderApprovingNativeSqlService` since both live in
  `com.divudi.service.pharmacy`) — default to injecting
  `PurchaseOrderRequestNativeSqlService` and calling its existing method
  directly rather than duplicating the logic. Only extract a shared helper
  class if that cross-service dependency turns out to be awkward in practice.
  Derives gross/net/purchase/retail values and PBI qty/rate conversions from
  the 5 raw inputs, identically for both phases.
- `BillItemFinanceDetails` stays JPA persist/merge, same split as Phase 1.
- Evict `Bill`, `BillItem`, `PharmaceuticalBillItem`, `BillItemFinanceDetails`
  from L2 cache after any native write (Phase 1 review finding, applied here
  from the start).

**Decision: recompute line values instead of copying ~25 BIFD fields.**
Legacy's `generateBillComponent()` copies every `BillItemFinanceDetails`
field verbatim from the request line (including discount/tax/expense/
wholesale/cost fields that are always zero for POs — Purchase Orders don't
use any of them). The native version instead carries only the 5 raw inputs
(qty, freeQty, purchaseRate, retailRate, unitsPerPack) through
`PurchaseOrderRequestLineData` and recomputes everything else via
`computeLineValues()` — the same function Phase 1 already uses and already
unit-tests. This is safe because:
- The approving page's own `onEdit()`/`calculateLineValues()` already
  recompute every derived field from user edits on the very first interaction
  — the copied values are working data, not an audit trail of the original
  request's numbers.
- It eliminates a 25-field copy that's pure duplication risk (a typo'd
  field name silently drops data) for zero behavioral gain.
- It keeps the two phases' line math provably identical — Phase 1's existing
  unit tests already cover this function's edge cases (zero qty, AMPP
  pack conversion, null fields).

**Decision: cross-link write stays JPA, `requestedBill` entity stays on JPA
throughout.** Confirmed with you — only the new `approvedBill`'s own rows go
through native SQL. `requestedBill` is loaded via `billFacade.find()` (as
today), read for guard checks and header seeding (payment method, supplier,
credit duration, department type, comments — all copied to `approvedBill` on
`navigateToPurchaseOrderApproval()`, matching legacy), and the final
`requestedBill.setReferenceBill(approvedBill); billFacade.edit(requestedBill)`
happens exactly as legacy does it, in JPA. Never touched by native SQL.

**Decision: `updateCalculatedValues()`'s "lighter recompute" optimization is
dropped.** Legacy has two code paths in `onEdit()` — a full
`calculateLineValues()` when BIFD looks incomplete, and a cheaper
`updateCalculatedValues()` when it doesn't, to skip resetting audit fields
(`createdAt`/`creater`) on an already-persisted line. Since audit fields are
now set once at native-insert time (not re-derived from BIFD state), this
distinction has no equivalent in the native flow — `onEdit()` here always
calls the single recompute helper (mirrors Phase 1's `recalculateLineValues`
+ `calculateBillTotals` pairing exactly).

### Controller responsibilities

- `requestedBill` / `approvedBill` state, `navigateToPurchaseOrderApproval(Long requestedBillId)`
  — takes the bill id as a parameter (see §6, matches Phase 1's
  `navigateToUpdatePurchaseOrder(Long billId)` fix), loads `requestedBill` via
  `billFacade.find()`, applies the same scope guard Phase 1's review added:
  reject if not `BillTypeAtomic.PHARMACY_ORDER`, retired, cancelled, or not
  owned by the logged-in user's department
- Already-approved check (`requestedBill.getReferenceBill() != null`) in both
  `navigateToPurchaseOrderApproval()` and `approve()`, `synchronized` on both
- `removeItem()` / `removeSelected()` — in-memory list mutation + serial
  renumbering (persisted only once `approve()` actually writes the lines —
  there's no intermediate persisted state to keep in sync here, unlike
  Phase 1's Request page)
- `onEdit()` — integer-qty gate + recompute, calls into the service's
  `computeLineValues()`-backed helper
- `approve()` — full validation loop (payment method, non-empty items,
  per-line PBI/qty/rate checks), then: native-create approved bill, native
  per-line save, bill-number assignment, native zero-qty retirement sweep,
  then the JPA cross-link write
- `prepareEmailDialog()` / `sendPurchaseOrderEmail()` /
  `generatePurchaseOrderHtml()` — same structure as Phase 1's, with `esc()`
  HTML-escaping applied to every free-text field from the start
- New page `pharmacy_purhcase_order_approving_native.xhtml` — copies the
  legacy layout 1:1 (item table, header panel, print-preview panel, email
  dialog, print-config dialog), with a "Legacy View" fallback button matching
  Phase 1's pattern

### Rejected alternatives

- **Copy all ~25 BIFD fields verbatim, matching legacy exactly.** Rejected —
  see decision above; recompute-from-raw-inputs is safer and reuses tested
  code.
- **Reuse `pharmaceuticalBillItemFacade.getPharmaceuticalBillItems()` (full
  entity graph) for the request-line read.** Rejected — this is exactly the
  entity-inflation cost the master issue exists to remove; a JPQL projection
  is a direct, low-risk substitution here since the read only needs the 8
  scalar fields listed in §3's `loadRequestedLines` description.
- **Make the approved bill's cross-link write native too, with explicit cache
  eviction to manage the risk.** Rejected — confirmed with you; goes against
  the master issue's explicit decision and the retail-sale precedent, no
  clear benefit.

---

## 4. Data written (native SQL + JPA)

Same split as Phase 1's spec:
- `bill` (the new approved bill), `billitem`, `pharmaceuticalbillitem` — native
  SQL, this service only
- `BillItemFinanceDetails` — JPA persist/merge, linked via
  `BILLITEMFINANCEDETAILS_ID`
- `requestedBill.referenceBill` (the cross-link FK) — JPA merge on the
  **existing** `PurchaseOrderRequestNativeSqlController`'s/legacy's bill
  entity, via `billFacade.edit()`, exactly as legacy does it. This is the one
  write in this phase that is explicitly *not* native, by design.

After any native INSERT/UPDATE, evict from L2 cache: `Bill`, `BillItem`,
`PharmaceuticalBillItem`, `BillItemFinanceDetails`.

---

## 5. Error handling

All validation stays as user-facing `JsfUtil` error messages, matching
legacy's wording where the message is user-visible copy (not silently
changed). Guards are checked in memory, before any native mutation reaches
the DB — same ordering-safety lesson Phase 1's review caught (validate
fully, then persist, never interleave).

---

## 6. List-to-Approve page rewiring

`pharmacy_purhcase_order_list_to_approve_dto.xhtml`'s "Approve" button
currently does:

```xml
action="#{purchaseOrderController.navigateToPurchaseOrderApproval}">
    <f:setPropertyActionListener target="#{purchaseOrderController.requestedBillId}" value="#{dto.billId}"/>
```

Changes to:

```xml
action="#{purchaseOrderApprovingNativeSqlController.navigateToPurchaseOrderApproval(dto.billId)}">
```

Identical fix to Phase 1's list-to-finalize page rewiring (PR #22733).

---

## 7. Review requirements (per master issue #22726)

Before this phase's PR merges:
1. Automated code review (CodeRabbit) — fix or explicitly resolve every
   finding, same process as Phase 1.
2. Playwright-driven manual walkthrough of the native Approving page: finalize
   a PO Request, navigate to Approve, edit/remove lines, approve, verify the
   approved bill + cross-link + print + email all work, confirm the legacy
   page's every button/function has a native equivalent.
3. Ideally a second-agent review of the diff, focused specifically on
   omissions relative to legacy (per master issue's stated purpose for this
   gate).

---

## 8. Reference Files

- `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderController.java` — legacy reference
- `src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml` — legacy page
- `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderRequestNativeSqlController.java` +
  `src/main/java/com/divudi/service/pharmacy/PurchaseOrderRequestNativeSqlService.java` —
  Phase 1, direct structural precedent (including its post-review fixes —
  upsert-by-existence, scope guards, evict Bill.class, HTML-escaping)
- `developer_docs/pharmacy/native-sql-bill-migration-guide.md` — native
  rewrite architecture guide
- Master issue #22726 — phase sequencing and cross-phase decisions
