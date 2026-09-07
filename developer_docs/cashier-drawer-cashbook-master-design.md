# Cash Drawer, Handover & Cashbook System — Master Design Plan

**Master Issue:** [#17532 — Cash Handover & Drawer System](https://github.com/hmislk/hmis/issues/17532)
**Priority:** Critical
**Status:** 11 completed, 52 open of 63 sub-issues
**Last Updated:** 2026-05-23 (Phase 4 — Non-Cash Settlement model simplified)

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Current Architecture](#2-current-architecture)
3. [Core Financial Flow](#3-core-financial-flow)
4. [Phase 0 — Critical Data Integrity Fixes](#phase-0--critical-data-integrity-fixes)
5. [Phase 1 — Float Management](#phase-1--float-management)
6. [Phase 2 — Handover Process Improvements](#phase-2--handover-process-improvements)
7. [Phase 3 — Shortage/Excess Settlement](#phase-3--shortageexcess-settlement)
8. [Phase 4 — Cashbook, Deposit & Non-Cash Settlement](#phase-4--cashbook-deposit--non-cash-settlement)
9. [Phase 5 — Staff Welfare Billing](#phase-5--staff-welfare-billing)
10. [Phase 6 — Shift Duration & Notifications](#phase-6--shift-duration--notifications)
11. [Entity Change Summary](#entity-change-summary)
12. [New Privileges Required](#new-privileges-required)
13. [Implementation Notes](#implementation-notes)

---

## 1. System Overview

The cashier system manages the full lifecycle of financial transactions:

```
Payment Collection → Drawer → Shift End → Handover → Cashbook → Deposit/Banking
```

**Key Concepts:**
- **Payment**: Individual monetary transaction linked to a bill, with one payment method
- **Drawer**: Per-user container tracking in-hand balances by payment method (18 types)
- **Handover**: Transfer of payments from one cashier to another (3-phase lifecycle)
- **CashBook**: Daily record of income/expense per department/site/institution
- **Float**: Working capital transferred between cashiers (not linked to dept/site/institution)
- **Deposit**: Removal of funds from the system into bank or safe

**Payment Methods (18):** Cash, Card, Cheque, Slip, eWallet, Staff, Credit, Staff_Welfare, Voucher, IOU, Agent, PatientDeposit, PatientPoints, OnlineSettlement, MultiplePaymentMethods, OnCall, None, YouOweMe (deprecated)

---

## 2. Current Architecture

### 2.1 Entities

| Entity | Location | Purpose |
|--------|----------|---------|
| `Drawer` | `core/entity/cashTransaction/Drawer.java` | Per-user balances: InHand, ShortageOrExcess, Balance × 18 methods |
| `DrawerEntry` | `core/entity/cashTransaction/DrawerEntry.java` | Audit trail with before/after snapshots per transaction |
| `CashBook` | `core/entity/cashTransaction/CashBook.java` | Per-dept/site/institution daily balance × 18 methods |
| `CashBookEntry` | `core/entity/cashTransaction/CashBookEntry.java` | Transaction log with 288+ granular balance fields |
| `Payment` | `core/entity/Payment.java` | Core payment record linked to bill, dept, institution |
| `PaymentHandoverItem` | `core/entity/PaymentHandoverItem.java` | Tracks payment through 3-phase handover (shift → create → accept) |

### 2.2 Services

| Service | Location | Responsibility |
|---------|----------|----------------|
| `PaymentService` | `service/PaymentService.java` | Creates payments, triggers drawer + cashbook updates |
| `DrawerService` | `service/DrawerService.java` | Thread-safe drawer balance updates, entry recording |
| `CashbookService` | `service/CashbookService.java` | Cashbook entry creation, balance updates (**incomplete**) |

### 2.3 Controllers

| Controller | Location | Responsibility |
|------------|----------|----------------|
| `FinancialTransactionController` | `bean/cashTransaction/` | Primary orchestrator — shifts, handovers, floats, 100+ navigation methods |
| `DrawerController` | `bean/cashTransaction/` | Drawer operations and balance viewing |
| `DrawerEntryController` | `bean/cashTransaction/` | Drawer history search and display |
| `DrawerAdjustmentController` | `bean/cashTransaction/` | Drawer balance adjustments |
| `CashBookController` | `bean/cashTransaction/` | Cashbook operations (deprecated, uses CashbookService) |
| `CashBookEntryController` | `bean/cashTransaction/` | Individual cashbook entry management |
| `PaymentController` | `bean/cashTransaction/` | Payment operations |
| `PaymentHandoverItemController` | `bean/cashTransaction/` | Handover item management |

### 2.4 Pages

All cashier pages are under `src/main/webapp/cashier/` (89 files). Navigation hub: `cashier/index.xhtml`.

### 2.5 Bill Types (BillTypeAtomic)

| Bill Type | Category | Effect |
|-----------|----------|--------|
| `FUND_SHIFT_START_BILL` | Shift | FLOAT_STARTING_BALANCE |
| `FUND_SHIFT_END_BILL` | Shift | FLOAT_CLOSING_BALANCE |
| `FUND_SHIFT_END_CASH_RECORD` | Shift | Record cash count |
| `FUND_TRANSFER_BILL` | Float | FLOAT_DECREASE (sender) |
| `FUND_TRANSFER_RECEIVED_BILL` | Float | FLOAT_INCREASE (receiver) |
| `FUND_SHIFT_HANDOVER_CREATE` | Handover | FLOAT_DECREASE (sender) |
| `FUND_SHIFT_HANDOVER_ACCEPT` | Handover | FLOAT_INCREASE (receiver) |
| `FUND_SHIFT_SHORTAGE_BILL` | Adjustment | FLOAT_INCREASE |
| `FUND_SHIFT_EXCESS_BILL` | Adjustment | CASH_IN |
| `FUND_DEPOSIT_BILL` | Deposit | BANK_OUT |
| `FUND_WITHDRAWAL_BILL` | Withdrawal | BANK_IN |
| `DRAWER_ADJUSTMENT` | Adjustment | FLOAT_CHANGE |

---

## 3. Core Financial Flow

### 3.1 Payment Collection

```
Patient Bill Created
  → BillItem(s) created
  → BillFee(s) created (except pharmacy)
  → Payment(s) created (one per payment method)
    → PaymentService.createPayment()
      → Saves Payment (linked to bill, dept, site, institution)
      → DrawerService.updateDrawer() — adds to cashier's drawer
      → CashbookService.writeCashBookEntry() — records in cashbook
```

### 3.2 Shift Lifecycle

```
Start Shift (FUND_SHIFT_START_BILL)
  → Initial float assigned to drawer
  → Cashier collects payments during shift

End Shift (FUND_SHIFT_END_BILL)
  → System calculates expected totals
  → Cashier records actual cash in hand
  → Shortages/excesses identified
```

### 3.3 Handover (3-Phase)

```
Phase 1: Shift End
  → Cashier ends shift → FUND_SHIFT_END_BILL created
  → System tallies all payments by method

Phase 2: Handover Creation
  → Cashier initiates handover → FUND_SHIFT_HANDOVER_CREATE
  → Component bills per payment method
  → PaymentHandoverItem records link payments to handover
  → Cash: aggregated as bulk amount
  → Non-cash (card/cheque/slip): individual payments listed
  → Payments marked: handingOverStarted = true
  → Sender's drawer decremented

Phase 3: Handover Acceptance
  → Receiver reviews and accepts → FUND_SHIFT_HANDOVER_ACCEPT
  → PaymentHandoverItem updated with accept bill references
  → currentHolder updated to receiver
  → Receiver's drawer incremented
  → First handover: CashBook entry written
  → Subsequent handovers: CashBook NOT updated (already recorded)
```

### 3.4 Float Transfers

```
Float Out (FUND_TRANSFER_BILL)
  → Sender creates transfer → drawer decremented
  → Currently: deducted immediately (problematic — see Phase 1)

Float In (FUND_TRANSFER_RECEIVED_BILL)
  → Receiver accepts → drawer incremented

Note: Floats are NOT linked to dept/site/institution
```

### 3.5 Cashbook Recording

```
First Handover of payments for a dept/site/institution:
  → Positive CashBookEntry created
  → CashBook balances incremented

Deposit/Banking:
  → FUND_DEPOSIT_BILL created
  → Negative CashBookEntry created
  → CashBook balances decremented
```

---

## Phase 0 — Critical Data Integrity Fixes

**Priority: IMMEDIATE — Must fix before other phases**

These are foundational bugs that cause incorrect balances and data corruption. All subsequent phases depend on correct payment/drawer/cashbook operations.

### Issues

| # | Issue | Impact | Scope |
|---|-------|--------|-------|
| #16293 | Missing `break` statements in payment recording across 34+ controllers | Data corruption, incomplete audit trails | PaymentService, multiple controllers |
| #18167 | Missing drawer update in `PaymentService.createPayment` overload | Drawer balance drift | PaymentService |
| #18834 | Drawer existence validation gap in `submitAdjustmentRequest()` | NPE, failed adjustments | DrawerAdjustmentController |
| #8981 | Inward deposits not updating drawer | Drawer balance incorrect | Inward module |
| #8328 | Shortage bill amount doubled in drawer | Inflated drawer balance | DrawerService |
| #7588 | Same payment included in two cashbook entries | Double-counted revenue | CashbookService |

### Design

1. **Audit `PaymentService`**: Review all `createPayment()` overloads. Every path must:
   - Save the Payment
   - Call `DrawerService.updateDrawer()`
   - Call `CashbookService.writeCashBookEntry()`

2. **Fix switch-case breaks** (#16293): Systematic audit of all controllers calling `populatePaymentDetails()` or similar switch statements on `PaymentMethod`.

3. **Guard against duplicate cashbook entries** (#7588): Add `cashbookEntryCompleted` flag check before writing. Payment already has this field — ensure it's checked.

4. **Fix shortage/excess drawer math** (#8328): Review `DrawerService` handling of `FUND_SHIFT_SHORTAGE_BILL` and `FUND_SHIFT_EXCESS_BILL` to ensure value is applied once, not doubled.

### Validation

After fixes, verify with test scenarios:
- Create payment → check drawer updated exactly once
- Cancel payment → check drawer reversed exactly once
- Record shortage → check drawer adjusted correctly
- Every payment method covered in switch statements

---

## Phase 1 — Float Management

**Issues:** #18901–#18904, #18913–#18914, #17934, #17539, #17965–#17966, #17928–#17939 (21 issues)

### Current Problems

1. Float is deducted from sender immediately, even before receiver accepts (#17965)
2. No float request workflow — only direct transfers (#17928)
3. No validation on empty amount (#18901)
4. Handover allowed even with pending float transactions (#18914)
5. No cancellation before acceptance (#17934)
6. Print formats don't match hospital requirements (#18902, #18903)

### Design

#### 1.1 Float Transfer Request Workflow (New)

**New Entity: `FundTransferRequest`** (or reuse Bill with new BillTypeAtomic values)

Recommended approach: **Use existing Bill entity** with new `BillTypeAtomic` values to avoid adding entities. The bill pattern is already established for all financial transactions.

```
New BillTypeAtomic values:
  FUND_TRANSFER_REQUEST         — Request created by cashier
  FUND_TRANSFER_REQUEST_CANCEL  — Request cancelled by creator
  FUND_TRANSFER_REQUEST_REJECT  — Request rejected by issuer
```

**State Flow:**
```
REQUESTED → APPROVED → ISSUED → RECEIVED
    ↓           ↓
 CANCELLED   REJECTED
```

Track state via bill relationships:
- Request bill → `referenceBill` points to issue bill when approved
- Issue bill (existing `FUND_TRANSFER_BILL`) → `referenceBill` points to request
- Receive bill (existing `FUND_TRANSFER_RECEIVED_BILL`) → `backwardReferenceBill` points to issue

#### 1.2 Deferred Deduction (#17965)

**Current:** Sender's drawer is decremented when float-out bill is created.
**New:** Sender's drawer is decremented only when receiver accepts.

Implementation:
- On `FUND_TRANSFER_BILL` creation: Mark bill as pending, do NOT update sender's drawer
- On `FUND_TRANSFER_RECEIVED_BILL` creation: Update BOTH sender's drawer (decrement) and receiver's drawer (increment)
- Add `floatPending` flag to track unaccepted floats

#### 1.3 Handover Guard (#18914)

Before allowing handover creation, check:
```java
// In FinancialTransactionController.navigateToHandoverCreateBill*()
List<Bill> pendingFloats = findPendingFloatTransfers(currentUser);
if (!pendingFloats.isEmpty()) {
    JsfUtil.addErrorMessage("Cannot handover: pending float transfers exist");
    return; // block navigation
}
```

#### 1.4 Float Cancellation (#17934)

Allow cancellation of `FUND_TRANSFER_BILL` only if:
- `FUND_TRANSFER_RECEIVED_BILL` does not exist for this transfer
- Cancel creates a cancellation bill (standard pattern)
- Since drawer wasn't deducted yet (per 1.2), no drawer reversal needed

#### 1.5 Validation & UI Fixes

| Issue | Fix |
|-------|-----|
| #18901 | Add `disabled="#{empty amount or amount <= 0}"` on submit button |
| #18902, #18903 | Update print XHTML to match hospital format |
| #18904 | Add required comment field for receiver |
| #18913 | Show `fromWebUser` name in pending float list datatable |
| #17966 | Remove single-transfer restriction for bulk cashier role |

#### 1.6 New Privileges (#17936–#17938)

```
Privilege: FUND_TRANSFER_REQUEST_CREATE
Privilege: FUND_TRANSFER_REQUEST_CANCEL
Privilege: FUND_TRANSFER_ISSUE
Privilege: FUND_TRANSFER_CANCEL_BEFORE_ACCEPT
```

---

## Phase 2 — Handover Process Improvements

**Issues:** #17953, #18907–#18910, #18912, #17956 (7 issues)

### Current Problems

1. Receiver can accept without verifying individual card/cheque/slip details (#18912)
2. Rejection doesn't reverse drawer updates (#18910)
3. Accepted handovers can be cancelled (#18908)
4. Patient deposits appear in handover listings (#17956)
5. Print formats missing (#18907, #18909)

### Design

#### 2.1 Payment Verification Before Accept (#18912, #17953)

Add verification step between preview and accept:

```
Handover Preview → Verification Step (NEW) → Accept
```

**UI Design (handover_accept.xhtml changes):**
- Display non-cash payments (card, cheque, slip) as individual line items
- Each line has a checkbox: "Verified"
- Accept button disabled until all non-cash items verified
- Cash amount shown as bulk total (already aggregated)

**Data tracking:**
- Add `verified` boolean to `PaymentHandoverItem` entity
- Or track via component bill items (lighter approach)

#### 2.2 Handover Rejection — Drawer Reversal (#18910)

When a handover is rejected:
```java
// Reverse sender's drawer changes
DrawerService.updateDrawerForIns(handoverPayments, senderUser);
// Reset payment flags
for (Payment p : handoverPayments) {
    p.setHandingOverStarted(false);
    p.setHandingOverCompleted(false);
}
// Retire the handover create bill
handoverCreateBill.setRetired(true);
```

#### 2.3 Cancellation Guard (#18908)

```java
// In handover cancel logic
if (handoverBill.getForwardReferenceBill() != null) {
    // Accept bill exists — cannot cancel
    JsfUtil.addErrorMessage("Cannot cancel: handover already accepted");
    return;
}
```

#### 2.4 Exclude Patient Deposits (#17956)

Filter `PaymentMethod.PatientDeposit` from:
- Shift end payment listings
- Handover start payment queries
- Add `AND p.paymentMethod != :patientDeposit` to JPQL queries

#### 2.5 Print Formats (#18907, #18909)

Create/update print templates:
- `handover_creation_bill_print.xhtml` — Handover summary print
- `handover_accept_bill_print.xhtml` — Acceptance receipt print
- Match hospital format requirements (obtain from Ruhunu team)

---

## Phase 3 — Shortage/Excess Settlement

**Issues:** #17940–#17943, #17946–#17948, #17955 (8 issues)

### Current Problems

1. Shortages/excesses can be recorded but never settled or cancelled
2. No approval workflow for settlements
3. No clear lifecycle management

### Design

#### 3.1 Settlement Lifecycle

**Use existing bill patterns** — no new entities needed. Track state via bill relationships:

```
Shortage/Excess RECORDED (existing FUND_SHIFT_SHORTAGE_BILL / FUND_SHIFT_EXCESS_BILL)
  → SETTLEMENT_REQUESTED (new BillTypeAtomic)
    → SETTLEMENT_APPROVED (approval bill references request)
      → SETTLED (settlement bill created, drawer adjusted)
    → SETTLEMENT_REJECTED
  → CANCELLED (standard bill cancellation)
```

**New BillTypeAtomic values:**
```
FUND_SHIFT_SHORTAGE_SETTLE_REQUEST
FUND_SHIFT_SHORTAGE_SETTLE_APPROVE
FUND_SHIFT_SHORTAGE_SETTLE
FUND_SHIFT_SHORTAGE_CANCEL

FUND_SHIFT_EXCESS_SETTLE_REQUEST
FUND_SHIFT_EXCESS_SETTLE_APPROVE
FUND_SHIFT_EXCESS_SETTLE
FUND_SHIFT_EXCESS_CANCEL
```

#### 3.2 Settlement Flow

**Settle Shortage (#17941):**
```
1. Cashier/admin requests settlement → SHORTAGE_SETTLE_REQUEST bill
2. Approver reviews → SHORTAGE_SETTLE_APPROVE bill (if approved)
3. Settlement executed:
   - Create SHORTAGE_SETTLE bill
   - Drawer: reduce shortageOrExcess value to 0
   - DrawerEntry: record the adjustment
   - CashBook: no entry (shortage is internal reconciliation)
```

**Cancel Shortage (#17940):**
```
1. Standard bill cancellation of FUND_SHIFT_SHORTAGE_BILL
2. Reverse drawer changes from original recording
3. Only allowed if NOT yet settled
```

**Cancel Settled Shortage (#17942):**
```
1. Cancel the SHORTAGE_SETTLE bill
2. Reverse the settlement drawer changes
3. Shortage returns to recorded state
```

Same patterns apply to Excess bills (#17946–#17948).

#### 3.3 Approval Process (#17943)

```
New Privileges:
  SHORTAGE_SETTLE_REQUEST   — Can request settlement
  SHORTAGE_SETTLE_APPROVE   — Can approve settlement
  EXCESS_SETTLE_REQUEST     — Can request settlement
  EXCESS_SETTLE_APPROVE     — Can approve settlement
```

**UI:** New pages or dialog in existing pages:
- List pending settlement requests (for approvers)
- Settlement request form (for cashiers/admins)
- Approval/rejection form with comments

#### 3.4 Finalize Process (#17955)

This is the umbrella design above. Implementation order:
1. Record shortage/excess (already works)
2. Cancel shortage/excess bills
3. Settle shortage/excess bills
4. Approval workflow
5. Cancel settled bills

---

## Phase 4 — Cashbook, Deposit & Non-Cash Settlement

**Issues:** #18911, #18919, #17958–#17962, #17964, #18915 (9 issues)

### Conceptual Model

Money flows through two distinct terminal events:

| Tender | Terminal event | Bill type | UI label |
|---|---|---|---|
| Cash | Physically deposited at bank | `FUND_DEPOSIT_BILL` (existing) | **Bank Deposit** |
| Card / Cheque / Slip / eWallet / OnlineSettlement | Confirmed by bank or processor | `PAYMENT_SETTLEMENT_BILL` (new) | **Settlement** |

Non-cash items accumulate at the holder (typically main cashier) across multiple shift handovers until they are formally **settled** with the bank or processor. They re-appear in every subsequent handover until settled, because they are individually-identified physical documents that move with the holder.

Cash is different — once handed over, its individual `Payment` records are NOT re-listed in subsequent handovers. Cash is reconciled by denomination count and flows through the Drawer balance.

### 4.1 Why Non-Cash Items Keep Appearing in Handovers

A non-cash `Payment` carries through handovers as long as `currentHolder` points to a real user. The holder is responsible for handing it on (or settling it) until it is externally confirmed.

### 4.2 The Settlement Event (#17964, supersedes #18915 conceptually)

The holder of one or more non-cash payments selects them, records the external reference (POS batch ID, deposit slip number, cheque number, eWallet receipt), and submits a `PAYMENT_SETTLEMENT_BILL`. This:

- Sets `payment.paymentSettled = true`
- Sets `payment.paymentSettlementBill = <this bill>`
- Sets `payment.currentHolder = null`

From that point onward the payment is excluded from every handover query (the existing `currentHolder = :user` filter handles this automatically — no handover query change needed).

**Cash Book is NOT touched at settlement.** The payment was already added to the cash book at first handover (existing behaviour). The settlement is a status transition, not a financial event.

**One settlement bill — one department.** A `PAYMENT_SETTLEMENT_BILL` may only contain payments that share the same `Payment.department`. The settlement bill's `department` / `fromDepartment` / `institution` / `fromInstitution` are derived from the payments, not from the actor. The settlement bill number (`deptId` / `insId`) is generated via `BillNumberGenerator.departmentBillNumberGeneratorYearly(...)` scoped to that department. This keeps department-level cashbooks, reports, and audit trails coherent. The UI presents a department dropdown (built from `findPendingSettlementDepartments(holder)`); the holder picks a department and settles within it.

### 4.3 Validation Rule

A `Payment` may only be settled if it has already been recorded in the cash book — i.e. has been through first handover. Concrete check:

```java
if (!payment.isCashbookEntryCompleted()) {
    throw new IllegalStateException("Cannot settle a payment that has no cash book entry");
}
```

This prevents the team from settling payments that were never properly handed over.

### 4.4 Settlement Cancellation

Standard bill cancellation pattern. Reverses the three field updates so the payment re-enters the handover flow next time:

- `paymentSettled = false`
- `paymentSettlementBill = null`
- `currentHolder` restored to the user who held it at the time of settlement (recorded on the original settlement bill)

Cash Book is not touched on cancellation either. Discrepancies (bounced cheque, processor chargeback, lost payment) are out of scope for this design and handled via a separate, planned reconciliation process — these cases are rare.

### 4.5 Entity Changes

**`Payment`** — add two fields:

| Field | Type | Default | Meaning |
|---|---|---|---|
| `paymentSettled` | boolean | false | True once non-cash settled. Always false for cash. |
| `paymentSettlementBill` | `@ManyToOne Bill` | null | FK to the settlement bill. Null for cash or unsettled non-cash. |

**`BillTypeAtomic`** — add two values:

```java
PAYMENT_SETTLEMENT_BILL("Payment Settlement Bill", BillCategory.BILL,
    ServiceType.OTHER, BillFinanceType.NO_FINANCE_TRANSACTIONS,
    CountedServiceType.OTHER, PaymentCategory.NO_PAYMENT,
    BillType.PaymentSettlementBill),

PAYMENT_SETTLEMENT_BILL_CANCELLED("Payment Settlement Bill - Cancelled", BillCategory.CANCELLATION,
    ServiceType.OTHER, BillFinanceType.NO_FINANCE_TRANSACTIONS,
    CountedServiceType.OTHER, PaymentCategory.NO_PAYMENT,
    BillType.PaymentSettlementBill),
```

`BillFinanceType.NO_FINANCE_TRANSACTIONS` because the settlement does NOT move drawer or cash book balances — the payment is already in the cash book from first handover.

**`BillType`** (legacy enum) — add `PaymentSettlementBill` value to keep the existing pattern intact.

### 4.6 Workflow Summary

```text
Pre-condition: Payment is non-cash, cashbookEntryCompleted=true, currentHolder=<holder>

Holder opens "Settle Non-Cash" page
  → Page lists payments where: paymentMethod != Cash
                                AND paymentSettled = false
                                AND currentHolder = :loggedInUser
                                AND cashbookEntryCompleted = true
  → Holder selects payments, enters external references
  → Submits → PAYMENT_SETTLEMENT_BILL created
  → For each payment:
      paymentSettled = true
      paymentSettlementBill = <bill>
      currentHolder = null

Cancellation:
  → PAYMENT_SETTLEMENT_BILL_CANCELLED created via standard cancellation
  → For each payment in the original bill:
      paymentSettled = false
      paymentSettlementBill = null
      currentHolder = <holder at time of settlement, restored from settlement bill metadata>
```

### 4.7 What This Replaces / Defers

This simplified design **supersedes** the earlier proposal in this document for:

- A separate `SettlementBatch` entity (not needed — `Bill` + new `BillTypeAtomic` suffices)
- A Payment lifecycle status enum (not needed — boolean + FK + existing `currentHolder` suffices)
- Cash-book adjustment entries on settlement (not done — separate reconciliation handles discrepancies)
- Approval workflow for non-cash settlement (deferred to a later issue if needed)

### 4.8 Deferred Items in This Phase

The following Phase-4 items remain open but are NOT part of the initial Settlement PR:

- `CashbookService.updateBalances()` completion (independent fix)
- Department cash book per-method tracking (#18911) — partly satisfied by Settlement giving a clean "what is unsettled" view
- Deposit funds enhancements (#17958–#17962, #18919) — for the existing cash-deposit workflow, separate from Settlement
- eWallet at handover (#18915) — verify that the first-handover cash-book write already populates the eWallet column; if not, that is a small follow-up fix

---

## Phase 5 — Staff Welfare Billing

**Issues:** #17546, #17547, #18905, #18906 (4 issues)

### Design

#### 5.1 Handover Display (#17546)

Staff welfare bills should appear in handover point just like other payment methods. Fix the JPQL query in handover listing to include `PaymentMethod.Staff_Welfare`.

#### 5.2 Staff Selection (#17547)

Fix staff autocomplete/selection in staff welfare billing — likely a missing converter or scope issue.

#### 5.3 Include in Summaries (#18905)

Add `Staff_Welfare` payment method totals to:
- Shift end summary
- Shift detail report
- All cashier summary

These reports likely filter by payment method — ensure `Staff_Welfare` is included in the filter list.

#### 5.4 Combined Multi-Payment (#18906)

When OPD/Pharmacy bill uses `MultiplePaymentMethods` and one component is `Staff_Welfare`, ensure `PaymentService.createPaymentFromComponentDetail()` handles it correctly.

---

## Phase 6 — Shift Duration & Notifications

**Issues:** #17967, #17969–#17971 (4 issues)

### Design

#### 6.1 Configurable Duration (#17967)

Add configuration key:
```
Config Key: "Maximum Shift Duration Hours"
Default: 12
Type: Long
```

Store in `ConfigOption` table (existing pattern).

#### 6.2 Duration Check

```java
// In FinancialTransactionController or a new ShiftService
public boolean isShiftExceeded(WebUser user) {
    Bill shiftStartBill = findLastShiftStartBill(user);
    if (shiftStartBill == null) return false;
    long hours = ChronoUnit.HOURS.between(
        shiftStartBill.getCreatedAt().toInstant(), Instant.now());
    return hours > configuredMaxHours;
}
```

#### 6.3 Transaction Restrictions (#17970)

When shift exceeds duration:
- Block new bill creation (show warning message)
- Force shift end / handover
- Exception: Allow cancellations and corrections

#### 6.4 Transaction Allowances (#17971)

Even during extended shifts, allow:
- Bill cancellations
- Payment corrections
- Shortage/excess recording
- Float operations (to facilitate handover)

#### 6.5 Notifications (#17969)

- Show warning banner on cashier pages when shift exceeds 80% of max duration
- Show critical alert when shift exceeds max duration
- Optional: Send notification to supervisor (future enhancement)

---

## Entity Change Summary

### Existing Entity Modifications

| Entity | Change | Phase |
|--------|--------|-------|
| `PaymentHandoverItem` | Add `verified` (boolean) | Phase 2 |
| `Payment` | Ensure `floatPending` / use existing flags correctly | Phase 1 |
| `Payment` | Add `paymentSettled` (boolean) and `paymentSettlementBill` (FK → Bill) | Phase 4 |

### New BillTypeAtomic Values

| Value | Phase | Purpose |
|-------|-------|---------|
| `FUND_TRANSFER_REQUEST` | Phase 1 | Float request created |
| `FUND_TRANSFER_REQUEST_CANCEL` | Phase 1 | Float request cancelled |
| `FUND_TRANSFER_REQUEST_REJECT` | Phase 1 | Float request rejected |
| `FUND_SHIFT_SHORTAGE_SETTLE_REQUEST` | Phase 3 | Shortage settlement requested |
| `FUND_SHIFT_SHORTAGE_SETTLE_APPROVE` | Phase 3 | Shortage settlement approved |
| `FUND_SHIFT_SHORTAGE_SETTLE` | Phase 3 | Shortage settled |
| `FUND_SHIFT_SHORTAGE_CANCEL` | Phase 3 | Shortage bill cancelled |
| `FUND_SHIFT_EXCESS_SETTLE_REQUEST` | Phase 3 | Excess settlement requested |
| `FUND_SHIFT_EXCESS_SETTLE_APPROVE` | Phase 3 | Excess settlement approved |
| `FUND_SHIFT_EXCESS_SETTLE` | Phase 3 | Excess settled |
| `FUND_SHIFT_EXCESS_CANCEL` | Phase 3 | Excess bill cancelled |
| `FUND_DEPOSIT_APPROVAL` | Phase 4 | Deposit approved |
| `PAYMENT_SETTLEMENT_BILL` | Phase 4 | Non-cash payment settled with bank/processor |
| `PAYMENT_SETTLEMENT_BILL_CANCELLED` | Phase 4 | Non-cash payment settlement cancelled |

### New Configuration Keys

| Key | Default | Phase |
|-----|---------|-------|
| `Maximum Shift Duration Hours` | 12 | Phase 6 |

---

## New Privileges Required

### Phase 1 — Float Management
```
FUND_TRANSFER_REQUEST_CREATE
FUND_TRANSFER_REQUEST_CANCEL
FUND_TRANSFER_ISSUE
FUND_TRANSFER_CANCEL_BEFORE_ACCEPT
```

### Phase 3 — Shortage/Excess
```
SHORTAGE_SETTLE_REQUEST
SHORTAGE_SETTLE_APPROVE
EXCESS_SETTLE_REQUEST
EXCESS_SETTLE_APPROVE
SHORTAGE_CANCEL
EXCESS_CANCEL
```

### Phase 4 — Deposits
```
DEPOSIT_APPROVE
DEPOSIT_MARK_COMPLETE
NON_CASH_DEPOSIT_COMPLETE
```

---

## Implementation Notes

### Branching Strategy

Each issue should get its own branch from `origin/development`:
```bash
git checkout -b <issue-number>-<short-description> origin/development
```

### Phase Dependencies

```
Phase 0 (Data Integrity) ← MUST complete first
  ↓
Phase 1 (Float) ← No dependency on Phase 2-6
Phase 2 (Handover) ← No dependency on Phase 1, 3-6
Phase 3 (Shortage/Excess) ← No dependency on Phase 1-2
Phase 4 (Cashbook/Deposit) ← Benefits from Phase 0 fixes
Phase 5 (Staff Welfare) ← Independent
Phase 6 (Shift Duration) ← Independent
```

Phases 1–6 can be worked in parallel after Phase 0 is complete. Within each phase, individual issues can often be tackled independently.

### Key Principles

1. **Use existing Bill pattern** — avoid new entities where possible. Bills with `BillTypeAtomic` values, linked via `referenceBill`/`backwardReferenceBill`, handle most workflows.
2. **Every payment path must update Drawer + CashBook** — no exceptions.
3. **Thread safety** — continue using `synchronized (drawer)` blocks in DrawerService.
4. **Backward compatibility** — never modify existing constructors (#8 in CLAUDE.md rules). Never rename fields with database typos (e.g., `purcahseRate`).
5. **Test drawer math** — after any drawer-related change, verify: create payment, cancel payment, handover, float in/out, shortage, excess.

### Orchestration for Future Conversations

This document serves as the master design reference. When starting work on any sub-issue:

1. Reference this document and the specific phase/section
2. Check the master issue #17532 for current completion status
3. Branch from `origin/development`
4. After completing an issue, check off the item in #17532

---

## Related GitHub Issues (Not in #17532)

These older issues may overlap with the master plan. Check before working on them:

| # | Title | Likely Covered By |
|---|-------|-------------------|
| #11074 | Drawer Balance Issues with Handover | Phase 0, Phase 2 |
| #11075 | Shortage bill amount not reduced in drawer | Phase 0 |
| #9295–#9297 | CC Deposit Cancellation drawer issues | Phase 0 |
| #9489, #9487 | Shortage bill cashbook issues | Phase 0 |
| #6991 | Need Institution/Site/Dept Balance in Cash Book Entry | Phase 4 |
| #10673 | Need summary for cashier at shift end / deposit | Phase 2 |
| #10976 | Reprints of Shift End summary and Shift Handover | Phase 2 |
| #11702 | Handover to Deposit Boxes | Phase 4 |
| #15912 | Optimize Payment Service | Phase 0 |
| #16293 | Systematic Payment Data Recording Defects | Phase 0 |
