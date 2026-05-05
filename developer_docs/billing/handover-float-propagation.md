# Handover Float Propagation

How net cash floats carry from one user to the next across shift handovers.
Reference: issue #20111.

## Core concept

A shift handover moves **two things** to the receiving user:

1. **Collected payments** — each has a `department`, each was created from a bill.
2. **Net float** — cash-only, `department = null`, user-to-user transfer with
   no bill context.

The handover bill total = `sum(collected payments) + netFloat`.

`netFloat = cashFloatInTotal − cashFloatOutTotal` on the shift bundle
(`ReportTemplateRowBundle.getCashFloatNetTotal()`).

## The `department = null` marker

`Payment.department == null` is the canonical marker of a float payment.
All real collections are attached to a department (via the bill). Queries
that aggregate "my floats" rely on this and on `creater` / `floatRecipient`
(see `FinancialTransactionController.fetchShiftFloatsFromShiftStartToEnd`).

When constructing a float payment always set:

```java
p.setDepartment(null);
p.setInstitution(null);
```

Setting only `department=null` but leaving `institution` populated produces
a "half-float" that slips past some aggregates. Always null both.

## `currentHolder` vs `creater` vs `floatRecipient`

| Field | What it means | When accept rewrites it |
|---|---|---|
| `creater` | Original author of the payment | Never changes |
| `floatRecipient` | Original recipient of a float transfer | Never changes |
| `currentHolder` | The user whose drawer / shift this payment belongs to **right now** | Rewritten on handover accept for collected payments |

Collected (non-float) payments move between shifts **purely by rewriting
`currentHolder`**. No new payment rows are created on accept for those.

Float payments are **not** transferred by rewriting `currentHolder`. The
sender's float records stay attributed to the sender. Instead, on accept
a **new** float payment is created on the receiver side.

## What accept does (FUND_SHIFT_HANDOVER_ACCEPT)

See `FinancialTransactionController.acceptHandoverBillAndWriteToCashbook()`.

For each incoming payment in the handover bundle:

1. If `isFundTransferPayment(p)` (i.e. a float payment) → **skip**, keep the
   sender's record as-is.
2. Otherwise → rewrite `currentHolder = receiver`, mark handover-completed,
   update receiver's drawer.

After the loop, if `bundle.getCashFloatNetTotal() != 0`:

1. Create a new `Bill` with `billTypeAtomic = FUND_TRANSFER_RECEIVED_BILL`,
   `referenceBill = <the accept bill>`.
2. Create a new `Payment` on that bill with:
   - `paymentMethod = Cash`
   - `paidValue = netFloat`
   - `creater = sender` (the original handover creator)
   - `floatRecipient = receiver` (the accepting user)
   - `currentHolder = receiver`
   - `department = null`, `institution = null`
3. Update the receiver's cash drawer via `drawerController.updateDrawerForIns`.

This receiver-side float is what makes the net float appear on the receiver's
**own** shift handover screen alongside their collections.

## Why the create bill stores a float-inclusive total

`settleHandoverStartBill()` writes
`currentBill.setTotal(bundle.getTotal() + bundle.getCashFloatNetTotal())`.

The payment-based subtotal is recomputed from persisted payments on accept.
Net float is not carried on any Payment of the handover bill itself — it
lives only on the shift's separate float payments. Storing the
float-inclusive total lets `navigateToReceiveNewHandoverBill()` restore
`cashFloatNetTotal` by subtraction:

```java
floatNet = selectedBill.getTotal() − rebuiltPaymentTotal
```

This is why the create bill total and the sum-of-payments diverge by exactly
the net float — don't "fix" them to match.

## Cancellation

Handovers cannot be cancelled once accepted (only rejected before accept).
The receiver-side float payment therefore has no cancel path today.
If a future requirement arises, cancel should:

- Retire the `FUND_TRANSFER_RECEIVED_BILL` and its float payment.
- Reverse the receiver's cash drawer update.
- Clear `currentHolder` rewrites for all collected payments (restore to sender).

## Cash-only

By concept, floats are **cash only**. The code doesn't yet enforce this at
the creation boundary (tracked separately). The accept-side float propagation
creates only a `PaymentMethod.Cash` float, matching the convention used
across the rest of the float flow.

## Key methods

- `FinancialTransactionController.settleHandoverStartBill()` — persists
  float-inclusive total on CREATE.
- `FinancialTransactionController.navigateToReceiveNewHandoverBill()` —
  restores `cashFloatNetTotal` on the bundle by subtraction.
- `FinancialTransactionController.acceptHandoverBillAndWriteToCashbook()` —
  rewrites `currentHolder` for collections, creates receiver float on accept.
- `FinancialTransactionController.fetchShiftFloatsFromShiftStartToEnd()` —
  aggregates floats for a user's shift (by `creater OR floatRecipient`,
  filtered by `currentHolder`).
- `ReportTemplateRowBundle.getCashFloatNetTotal()` —
  `cashFloatInTotal − cashFloatOutTotal`.
