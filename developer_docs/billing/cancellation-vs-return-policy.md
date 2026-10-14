# Cancellation vs. Return — Policy (do not build item-level cancellation)

**Applies to:** every bill type in the system (OPD, Pharmacy, Inward/Inpatient,
Channel, Collecting Centre, …). Raised concretely by issue #23555 (a request
to add per-row checkboxes to the Inward Service Bill **cancellation** screen),
which turned out to duplicate a Return flow already shipped in #21247.

## The rule

A **cancellation always reverses the whole bill**. Cancelling creates a
contra-bill with exactly the opposite values of the original
(`bill.setCancelled(true)` + a `CancelledBill`/`RefundBill`-style contra
record — see `payment-cancellation-guidelines.md` for the implementation
pattern) and the original bill stops being active. There is no partial
cancellation anywhere in this codebase, and there must never be one:

- 🚫 Do **not** add row checkboxes / a selection column to a *cancellation*
  page or its backing `cancelBill()`-style method.
- 🚫 Do **not** add a `List<BillItem> itemsToCancel` parameter to a
  cancellation method, or a per-item "cancelled" flag driven from a
  cancellation screen.
- 🚫 Do **not** interpret "cancel just the wrong line" as a smaller version
  of cancellation. It is a different feature.

**Selective, item-level reversal is a Return (a.k.a. Refund).** A Return
creates its own bill referencing only the selected items/fees, with their
values inverted — implementations may use `BillItem.invertValue()` (e.g.
`InwardServiceRefundController`) or compute explicit negative totals (e.g.
`WardPharmacyReturnToPharmacyController.doSettle()`) — while the rest of the
original bill stays active and billable. This is the *only* supported way to
reverse "some but not all" of a bill. If a screen needs to let staff pick one
or more items and reverse just those, it is a Return screen, not a Cancel
screen — model it on one of the existing ones below rather than extending
Cancel.

## Why this split, not "smart cancellation"

- **Audit trail.** A cancellation says "this whole transaction should not
  have happened." A return says "this transaction happened correctly, and
  now some of it is being reversed." Collapsing the two loses that
  distinction in every report that keys off `bill.cancelled` vs.
  `bill.refunded`/`RefundBill`.
- **Downstream state.** Cancellation guards (professional payment already
  paid out, lab sample already received, bill already checked/finalised,
  request-approval workflow, etc.) are written and tested against
  "all-or-nothing." A partial cancellation would need to re-derive every one
  of those guards per item, and — per issue #23555 — an early attempt at this
  duplicated a feature that already existed with those guards correctly
  implemented as a Return.
- **Existing reports and reconciliation assume whole-bill cancellation.**
  Cashier summaries, cancellation-approval workflows
  (`Admin-OPD-Inward-Bill-Cancellation-Requests`), and the billing
  cancellation query pattern (always sum RECEIVED + CANCELLATION bill types
  with signed `netValue`) all assume a cancelled bill's items are either all
  live or all reversed together.

## Existing Return implementations — follow one of these patterns

| Module | Return screen | Controller | Notes |
|---|---|---|---|
| Inward Service Bill | `inward/inward_bill_service_refund.xhtml` | `InwardServiceRefundController` (`bean/inward`) | Checkbox multi-select `p:dataTable`; totals computed from selected items only, then inverted; multiple passes supported until fully returned. See `developer_docs/billing/inward-service-bill-return.md`. |
| OPD | `BillSearch.refundOpdBill()` | `bean/opd` (`BillSearch`) | Item-level OPD refund; see the sibling Collecting-Centre variant `refundCollectingCenterBill()` in the same class. |
| Pharmacy | `pharmacy_search_return_bill_pre.xhtml`, `pharmacy_search_return_bill_bht.xhtml`, and the wholesale/BHT variants | pharmacy return controllers | Item-and-payment or item-only returns; see the `Pharmacy - Return Items and Payments` / `Pharmacy Return - Items Only` wiki pages. |
| Channel booking | see `Cancelling-and-Refunding-a-Channel-Booking` wiki page | — | Cancel and refund are documented as distinct actions there too. |

If you are building a *new* module's reversal feature and none of these fit,
bring the design to the team before writing it — per CLAUDE.md's "discuss
uncertainties" rule — rather than inventing a fifth pattern.

## What to do when a user asks for "cancel just this one item"

1. Check whether a Return/Refund screen already exists for that bill type
   (search the sibling `..._refund.xhtml` / `..._return...xhtml` pages and
   the wiki's Search index — most bill types already have one).
2. If it exists but is hard to find from the Cancel screen, the fix is
   **discoverability** (a hint/link on the Cancel page, better search
   filters), not a new cancellation code path.
3. If it genuinely does not exist for that bill type, that is a legitimate
   feature request — build a **Return**, following the table above, not an
   extension of Cancel.

## Related docs

- `developer_docs/billing/payment-cancellation-guidelines.md` — how to
  implement a (whole-bill) cancellation correctly.
- `developer_docs/billing/inward-service-bill-return.md` — the reference
  Return implementation and its "do not regress" design rules.
- Wiki: `Inpatient-Bill-Cancellations-and-Refunds-Overview`,
  `Finance-Inpatient-Service-Bills` ("Returning (partial refund)" section).
