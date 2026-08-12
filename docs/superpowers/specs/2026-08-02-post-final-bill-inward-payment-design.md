# Post Final Bill Inward Payment — Design

**Issue:** [#22617](https://github.com/hmislk/hmis/issues/22617)
**Related:** [#22282](https://github.com/hmislk/hmis/issues/22282) — multiple final bill versions per admission (`patientEncounter.finalBill` stays the authoritative pointer regardless of version count; only relevant here for how "the final bill" is resolved)
**Date:** 2026-08-02

---

## 1. Goal

Today the only mechanism for recording an inward patient's cash/card payment is the
pre-final "Inward Payment" (deposit) — `BillType.InwardPaymentBill` /
`BillTypeAtomic.INWARD_DEPOSIT`, entered via `InwardPaymentController` /
`inward_bill_payment.xhtml`. Those payments are summed as "Paid By Patient" and factored
into the Due shown on the Interim Bill, and ultimately into `finalBill.paidAmount` when
the final bill is created (`BhtSummeryController.settle()`).

Once the final bill exists, there is no correctly-tracked way to record further payments
toward the outstanding balance — cashiers have no dedicated flow, no receipt showing the
running balance, and no reporting bucket for this money.

This adds a **Post Final Bill Inward Payment**: a distinct payment type, recorded after
the final bill, that must:
- **not** be swept into the final-bill due calculation (that calculation is pre-final by
  definition and must stay exactly as-is), and
- **be** reported in exactly the same place as existing Inward Payments in every
  financial report (Cashier Summary, Cashier Details, All Cashier Summary, Daily Return)
  — same bucket, no new rows or sections.

## 2. Investigation findings (current behavior)

Established by reading the code directly (file:line references):

- **Deposit bill creation** — `InwardPaymentController.saveBill()`
  (`src/main/java/com/divudi/bean/inward/InwardPaymentController.java:870-890`) sets
  `BillType.InwardPaymentBill`, `BillTypeAtomic.INWARD_DEPOSIT`,
  `BillNumberSuffix.INWPAY`, `BillClassType.BilledBill`.
- **Bill numbering** — `BillNumberGenerator.institutionBillNumberGenerator(Department,
  BillType, BillClassType, BillNumberSuffix)`
  (`src/main/java/com/divudi/ejb/BillNumberGenerator.java:1226-1244`) →
  `fetchLastBillNumber(...)` (`:1746-1826`). This overload does **not** reset yearly
  despite the `BillNumber.billYear` field existing on the entity — it's a flat
  per-department+billType+billClassType counter. Yearly-reset logic exists only in a
  separate, unused-here `fetchLastBillNumberForYear*` overload keyed by `BillTypeAtomic`
  (`:177-270`). The new payment type's numbering should mirror the *actual* existing
  behavior (a new `BillNumberSuffix`, same non-yearly-reset generator), not invent new
  yearly-reset behavior that doesn't exist elsewhere in this flow.
- **Interim bill "Payments" tab** — backed by `BhtSummeryController`
  (`src/main/java/com/divudi/bean/inward/BhtSummeryController.java`), tab markup in
  `inward_bill_intrim.xhtml:1321-1382`, `paymentBill` getter
  (`BhtSummeryController.java:4381-4386`) → `InwardBeanController.fetchPaymentBill()`
  (`InwardBeanController.java:1873-1889`), filtered by `b.billType = BillType.InwardPaymentBill`.
- **Final bill due calculation** —
  `InwardBeanController.getPaidByPatientValue(PatientEncounter)`
  (`InwardBeanController.java:2427-2443`) and `getPaidValue()` (`:2411-2425`) both filter
  strictly on `BillType.InwardPaymentBill`. Called from `BhtSummeryController` at
  multiple due/balance computation sites (`:2456,2465,2535,2565,2661,2721,4804`). **Any
  new bill type reusing `BillType.InwardPaymentBill` would automatically be swept into
  this sum** — the new feature must use a *different* `BillType` to stay excluded without
  touching these queries.
- **Final bill netTotal/paidAmount** — `BhtSummeryController.saveBill()`,
  `:3193`: `getCurrent().setNetTotal(grantTotal - discount)` — the **full** charge total,
  never netted against prior deposits. `setPaidAmount(paid)` is a **one-time snapshot**
  (`paid = paidByPatient + paidByCompany`, `:4715-4716`) taken at settle time and never
  updated afterward. `due = (grantTotal - discount) - paid` (`:4718`), persisted to
  `PatientEncounter.amountDueAtFinalProcessing` (`:4732-4740`, field at
  `PatientEncounter.java:280`) — a snapshot, not a live value. `Bill.balance`
  (`Bill.java:1322-1327`) exists but is never set by `BhtSummeryController` for the final
  bill, so it cannot be trusted either.
- **Inpatient Dashboard navigation** — `admission_profile.xhtml:466-477`, "Payments"
  button (privilege `InwardDoctorPaymentAccess`) →
  `InwardSearch.navigateDoctorPayment()`
  (`src/main/java/com/divudi/bean/inward/InwardSearch.java:955-970`).
- **Main menu navigation** — live path is main menu → "Payment Management"
  (`FinancialTransactionController.navigateToPaymentManagement()`,
  `FinancialTransactionController.java:337`) → `/payments/pay_index.xhtml`, which has an
  "Inward Deposit" tab (`pay_index.xhtml:138-171`) calling
  `InwardPaymentController.navigateToInwardDepositPayment()` (`:156-165`). (There's also
  a dead, `rendered="...and false"` menu path at `menu.xhtml:1796-1799` — not used.)
- **Cashier reports** — `CashierReportController.findSummeryOwn()`
  (`src/main/java/com/divudi/bean/report/CashierReportController.java:2275-2326`) sets
  `c.setInwardPaymentCash/Cheque/Slip(...)` (`:2319-2321`) and
  `setInwardCancelCash/Cheque/Slip(...)` (`:2323-2325`) via `calTotOwn(w, billClass,
  paymentMethod, BillType.InwardPaymentBill)`. `calTotOwn()` (`:2328-2344`) branches
  explicitly `if (billType == BillType.InwardPaymentBill)`. Rendered as the "Inward
  Payment" / "Inward Payment Cancel" rows (`:2230-2246`). The same class backs Cashier
  Summary, Cashier Details, and All Cashier Summary (`navigateToAllCashierSummary`,
  `:288`) — one change here covers all three reports.
- **Daily Return** — no `BillType.InwardPaymentBill`-specific bucket exists today in
  `DailyReturnDtoController.java` / `DailyReturnDtoService.java`. Needs a new bucket
  added (not just an extension of an existing one).
- **Print template** — `posPaperPaymentBill.xhtml` / `FiveFivePaymentBill.xhtml` /
  `A4PaperPaymentBill.xhtml`
  (`src/main/webapp/resources/inward/bill/payment/`) currently show: institution header,
  "Deposit Receipt" heading, Name/Age-Sex/Admission Type/BHT No/dates, Bill No, Payment
  Method, comment, multi-method payment breakdown, single "Paying Amount" = `bill.netTotal`,
  cashier name. **No fields for final bill total, paid-previously, due-previously, or
  balance** — these must be added new.

## 3. Design decisions (confirmed with user)

| Decision | Choice | Why |
|---|---|---|
| Name | **Post Final Bill Inward Payment** | Drives all enum/label naming below |
| Controller | **New dedicated controller** (`PostFinalBillInwardPaymentController`), not a mode flag on `InwardPaymentController` | `InwardPaymentController` is `@SessionScoped` and depended on throughout the existing deposit flow; adding branching there risks breaking it. A new, independent controller keeps both flows simple and isolated. |
| BillType strategy | **New `BillType.PostFinalBillInwardPayment`**, not a shared `BillType` + new `BillTypeAtomic` | A new `BillType` means `getPaidByPatientValue()`/`getPaidValue()` need **zero** changes (they only ever summed `InwardPaymentBill`) — the safer path, since those queries feed ~7 due/balance computation sites. The tradeoff (Cashier/Daily-Return reports need an additive branch to fold the new type into the existing bucket) is lower-risk than editing core due-calculation queries. |
| Cancellation/refund | **Both supported**, mirroring `INWARD_DEPOSIT_CANCELLATION`/`INWARD_DEPOSIT_REFUND` at the same level of detail | Parity with the existing deposit flow; no additional requirements requested. |
| Payment amount cap | **Capped at live balance due** — cannot exceed `finalBill.netTotal - finalBill.paidAmount - SUM(non-cancelled post-final payments)` | Prevents accidental overpayment; matches how deposits are implicitly bounded by remaining charges. |
| Privilege | **New dedicated privilege** (`InwardPostFinalPaymentAccess`) | Lets a role be granted pre-final deposit access without post-final settlement access, or vice versa. |
| Reports | **Same bucket as "Inward Payment" everywhere** (Cashier Summary, Cashier Details, All Cashier Summary, **and** Daily Return — the latter gets a new "Inward Payment" bucket that didn't exist before, folding in both types from day one) | Explicit user requirement: no new report sections, this money reads as ordinary "Inward Payment" income to report consumers. |
| Scope of linkage | Encounter-scoped (like deposits), not final-bill-version-scoped | Keeps payments valid across final bill version changes under #22282 without new per-version FK plumbing; `patientEncounter.finalBill` stays the pointer used to resolve "the current final bill" at query time. |

## 4. Data model

New entries, added alongside their existing siblings (never modify/renumber existing
enum values — see project rule on backward compatibility):

- `BillType.PostFinalBillInwardPayment` (`com.divudi.core.data.BillType`)
- `BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT`,
  `POST_FINAL_BILL_INWARD_PAYMENT_CANCELLATION`,
  `POST_FINAL_BILL_INWARD_PAYMENT_REFUND` (`com.divudi.core.data.BillTypeAtomic`), each
  mapping to the new `BillType`
- `BillNumberSuffix` — new value for this payment type's own series, generated via the
  same `institutionBillNumberGenerator(...)` path the deposit uses today (flat
  per-department counter, consistent with actual current behavior, not an invented
  yearly reset)
- New privilege `InwardPostFinalPaymentAccess`

## 5. Balance due computation

Computed live, every time it's needed (display, cap, print) — never trusted from
`finalBill.getPaidAmount()` alone, since that field is a one-time snapshot:

```
balance = finalBill.getNetTotal()
        - finalBill.getPaidAmount()
        - SUM(netTotal of non-cancelled PostFinalBillInwardPayment bills
              for this patientEncounter)
```

`finalBill` is resolved via `patientEncounter.finalBill` (the confirmed version, per
#22282's design — irrelevant to this feature beyond "use the existing pointer, don't
invent a new one").

## 6. New page/controller

`PostFinalBillInwardPaymentController` (new, `@SessionScoped`) +
`inward_bill_post_final_payment.xhtml`, structurally modeled on `InwardPaymentController`
/ `inward_bill_payment.xhtml` (payment method selection, multi-method breakdown, comment
field, save flow) but an independent class — no shared session state with the deposit
controller. Only reachable when `patientEncounter.finalBill` exists, is confirmed, and is
not cancelled; otherwise the entry points (below) are hidden/disabled.

## 7. Interim bill page

New tab **"Post Final Payments"** in `inward_bill_intrim.xhtml`'s Fees-and-Details tab
group, alongside the existing "Payments" tab. Same grid columns (Bill No, Paid At, Added
User, Payment Method, Value, Checked User, Checked At, Action), populated by a new
`fetchPostFinalPaymentBill()`-style method filtered on the new `BillType`. This tab's
values are **not** added into that page's Charges/Summary totals — the Interim Bill page
represents pre-final-bill state.

## 8. Navigation

- **Inpatient Dashboard** (`admission_profile.xhtml`): new "Post Final Payment" button
  next to the existing "Payments" button (near `:466-477`), gated on
  `InwardPostFinalPaymentAccess` and on a confirmed final bill existing.
- **Main menu → Payment Management** (`pay_index.xhtml`): new "Post Final Bill Payment"
  tab alongside the existing "Inward Deposit" tab (near `:138-171`).

## 9. Print receipt

New composite print components, cloned from the existing
`posPaperPaymentBill.xhtml`/`FiveFivePaymentBill.xhtml`/`A4PaperPaymentBill.xhtml` set,
adding these fields not present today:

- Final bill net total
- Paid previously (deposits + CC payments + all prior non-cancelled post-final payments)
- Due previously (balance immediately before this payment)
- Amount paid this time
- Resulting balance

All other fields (institution header, patient/admission details, payment method
breakdown, cashier name, duplicate/cancelled flags) carry over unchanged from the
existing template.

## 10. Reports

- **Cashier Summary / Cashier Details / All Cashier Summary** (all backed by
  `CashierReportController`): `calTotOwn()` gets an additive branch — when summing the
  "Inward Payment" bucket, include `BillType.PostFinalBillInwardPayment` alongside
  `BillType.InwardPaymentBill` in the same query/sum feeding `findSummeryOwn()`'s
  `setInwardPaymentCash/Cheque/Slip` and `setInwardCancelCash/Cheque/Slip`. No new row,
  no new column — same "Inward Payment" / "Inward Payment Cancel" labels.
- **Daily Return**: no equivalent bucket exists today. Add one ("Inward Payment"),
  summing both `BillType.InwardPaymentBill` and `BillType.PostFinalBillInwardPayment`
  from the start — so it never has a version that only shows one of the two.

## 11. Out of scope

- Any change to `InwardPaymentController`'s existing deposit flow or its queries.
- Any change to the pre-final "Payments" tab or the final-bill due-calculation queries
  (`getPaidByPatientValue`/`getPaidValue`) themselves.
- Per-final-bill-version payment tracking — payments stay encounter-scoped, matching how
  deposits already behave; no new FK to a specific final bill row.
