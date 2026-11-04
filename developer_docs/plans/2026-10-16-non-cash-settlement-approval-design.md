# Non-Cash Settlement Approval — Design

**Date:** 2026-10-16
**Status:** Spec approved for planning
**Supersedes:** `developer_docs/cashier-drawer-cashbook-master-design.md` §4.7 bullet "Approval workflow for non-cash settlement (deferred)"

## 1. Goal

Settling non-cash payments (Card, Cheque, Slip, eWallet, OnlineSettlement) must go through a maker–checker approval before the payments actually settle. While a request awaits approval, the requester can neither end their shift nor create a handover. On approval, the settled amount leaves the requester's drawer.

## 2. Decisions

| # | Decision |
|---|---|
| D1 | Approval is **mandatory** for every new settlement, at every hospital. No config key. Hospitals that never use Settle Non-Cash are unaffected: nothing is ever pending, so no gate fires. |
| D2 | A pending request blocks the **requester** (shift end + handover create), and the payments in it are **locked** so they cannot move to anyone else. |
| D3 | **Cash book is not touched** by request, approval, rejection or cancellation. Cash-book treatment is handled separately. *Superseded by #24627: approval debits and cancellation credits the department's cash book.* |
| D4 | On approval, each payment's value is **debited from the requester's drawer** for its payment method (reverse of the credit made at handover acceptance). |
| D5 | Approver must hold a new privilege and must **not** be the requester. |
| D6 | Legacy settlement bills (created before this change, no approval stamp) remain settled as they are. Their drawer was never debited, so cancelling one must not credit the drawer. |
| D7 | Existing drawer balances inflated by past settlements are out of scope; correct via Drawer Adjustment if needed. |

## 3. Background (current behaviour)

- `PaymentSettlementService.settlePayments()` creates a `PAYMENT_SETTLEMENT_BILL` and immediately sets `paymentSettled=true`, `paymentSettlementBill=<bill>`, `currentHolder=null`.
- Handover create debits the sender's drawer for non-cash payments; handover accept credits the receiver's. Settlement never debits, so the final holder's non-cash drawer balance only grows. D4 fixes this for new settlements.
- `PaymentSettlementService.cancelSettlement()` exists but no page calls it.
- `Request` / `RequestType` / `RequestStatus` / `RequestController` already implement approval for Drawer Adjustment, Petty Cash, Bill Cancel, etc. This design reuses that pattern.
- `DrawerService.applyDrawerAdjustment(drawer, paymentMethod, delta, bill, actor)` already applies a signed per-method drawer change with a `DrawerEntry` tagged to the drawer owner.

## 4. Data Model

No new tables or columns.

| Element | Change |
|---|---|
| `RequestType` | Add `NON_CASH_SETTLEMENT_APPROVAL("Non-Cash Settlement Approval", RequestCategory.APROVEL, "NC-SET-APV")` |
| `Privileges` | Add `ApproveNonCashSettlement("Approve Non-Cash Settlement")`; register in `UserPrivilageController` under the Cash Transaction node (next to `SettleNonCashPayments`) and in the privilege category switch in `Privileges` |
| `Request` (per settlement) | `requestType=NON_CASH_SETTLEMENT_APPROVAL`, `bill=<settlement bill>`, `requester`=`targetWebUser`=holder, `department`=settlement department, `status=PENDING` |
| `Bill` (settlement bill) | Uses existing `approveUser` / `approveAt` as the approval stamp |

### State definitions

| State | Payment | Settlement bill | Request |
|---|---|---|---|
| Unsettled | `paymentSettlementBill=null`, `paymentSettled=false` | — | — |
| **Pending** | `paymentSettlementBill=<bill>`, `paymentSettled=false`, `currentHolder`=requester | not retired, `approveAt=null` | `PENDING` |
| **Approved** | `paymentSettled=true`, `currentHolder=null` | `approveUser`/`approveAt` set | `APPROVED` (approved=true) |
| **Rejected / Withdrawn** | back to Unsettled | `retired=true`, `retireComments`=reason | `REJECTED` or `CANCELLED` |
| Legacy settled | `paymentSettled=true`, `currentHolder=null` | `approveAt=null`, no Request | — |

"Pending for user U" is defined as: a non-retired `NON_CASH_SETTLEMENT_APPROVAL` request with `status = PENDING` and `requester = U`. Because the payments' `currentHolder` stays U while pending, this is equivalent to the payment-side state above. The gate check lives in one place, `PaymentSettlementService.findPendingRequestOf(WebUser)`, which returns the request so the message can name it.

## 5. Workflows

### 5.1 Request (maker) — `PaymentSettlementService.requestSettlement(...)`

Replaces the immediate behaviour of `settlePayments(...)`. Same input (payments, reference number, comments, actor) and same validations as today, plus:

- Payment must not already be linked to a settlement bill (`paymentSettlementBill == null`).
- Payment must not be inside an outgoing handover awaiting acceptance (`handingOverStarted == false`).

Effects:
1. Create the `PAYMENT_SETTLEMENT_BILL` exactly as today (department-scoped number, totals, reference, comments). `approveUser`/`approveAt` left null.
2. For each payment: set `paymentSettlementBill=<bill>`. **Do not** change `paymentSettled` or `currentHolder`.
3. Create the `Request` (PENDING, requestNo generated the same way other request types are).
4. No drawer change, no cash-book change.

`findPendingSettlementDepartments` / `findPendingSettlementPayments` add `AND p.paymentSettlementBill IS NULL AND p.handingOverStarted = false` so locked payments are not offered again.

UI (`cashier/settle_non_cash.xhtml`): button label "Submit for Approval"; success message states the request is awaiting approval; the print preview shows **PENDING APPROVAL** instead of a "Verified By" line until approved.

The old `settlePayments` method is removed (its only caller is the controller), so no path settles without approval.

### 5.2 Gates

Add `hasPendingSettlementRequest(loggedUser)` checks, **not** bypassable by `Allow Shift End Without Handover Acceptance` or any other config:

| Location | Message |
|---|---|
| `FinancialTransactionController.settleShiftEnd()` | "You have a non-cash settlement awaiting approval (request <no>). It must be approved or rejected before you can end your shift." |
| `FinancialTransactionController.settleHandoverStartBill()` | "You have a non-cash settlement awaiting approval (request <no>). It must be approved or rejected before you can create a handover." |
| Navigation into handover-create (`navigateToHandoverCreateBillForCurrentShift`, `navigateToHandoverCreateBillForSelectedPeriod`, `navigateToHandoverCreateBillForSelectedShift`) and shift-end summary navigation | Same message, shown early; navigation is refused |

The server-side checks in the two `settle*` methods are authoritative; the navigation checks are for early feedback.

### 5.3 Approve (checker) — `PaymentSettlementService.approveSettlement(Request, WebUser approver)`

Validations (inside the transaction, against freshly loaded entities):
- Request type is `NON_CASH_SETTLEMENT_APPROVAL` and status is `PENDING` (or `UNDER_REVIEW`).
- Approver has `ApproveNonCashSettlement` (checked in the controller) and `approver != requester`.
- Settlement bill not retired, not cancelled, `approveAt == null`.
- Every linked payment: still `paymentSettlementBill == bill`, `paymentSettled == false`, `currentHolder == requester`, not cancelled/retired. Any failure aborts the whole approval with a message naming the payment.

Effects:
1. For each payment: `paymentSettled=true`, `currentHolder=null`.
2. Drawer: group payment values by method; for each method call `drawerService.applyDrawerAdjustment(requesterDrawer, method, -sum, settlementBill, approver)`.
3. Bill: `approveUser=approver`, `approveAt=now`.
4. Request: `approved=true`, `approvedBy`, `approvedAt`, `status=APPROVED`.

### 5.4 Reject — `rejectSettlement(Request, WebUser, String reason)` / Withdraw — `withdrawSettlement(Request, WebUser)`

- Reject: approver privilege, reason mandatory. Withdraw: only the requester, request still PENDING.
- For each payment: `paymentSettlementBill=null` (back to the holder's pending list).
- Bill: `retired=true`, `retirer`, `retiredAt`, `retireComments=reason`.
- Request: `rejected`/`rejectedBy`/`rejectedAt`/`rejectionReason`, `status=REJECTED` (withdraw: `cancelled…`, `status=CANCELLED`).
- No drawer change. Gate lifts automatically (no pending payments remain).

### 5.5 Cancellation of an approved settlement — `cancelSettlement(...)` (service only, no UI in scope)

Existing behaviour plus: if the bill has `approveAt != null`, credit the drawer back per method (`applyDrawerAdjustment(..., +sum, cancellationBill, actor)`) to the restored holder. Legacy bills (`approveAt == null`, `paymentSettled` true) get no drawer change. Refuse to cancel a bill that is still pending (use reject/withdraw instead).

## 6. Approval UI

- New page `cashier/non_cash_settlement_approvals.xhtml` (follows `ui-guidelines`: filters in left sidebar, no `h:panelGrid`, no raw h-tags).
  - Left: department filter, date range, status (default PENDING).
  - Main: table of requests — request no, bill no, department, requester, requested at, total, reference, payment count.
  - Row detail: payment list (method, originating bill, reference, bank, amount) and actions **Approve** / **Reject** (reject opens a reason input). Buttons are `disabled` (not hidden) without the privilege or when the viewer is the requester, with a tooltip saying why.
- Menu: link in `cashier/index.xhtml` next to "Settle Non-Cash Payments", with pending count, enabled by `ApproveNonCashSettlement`.
- Settle Non-Cash page: a "My pending requests" panel listing the user's PENDING requests with a **Withdraw** button.
- After approval, the approvals page (status filter = Approved) shows who approved and when. There is no separate settlement-bill reprint page today; adding one is out of scope.

## 7. Error Handling

- All state changes happen in `@Stateless` service methods (one transaction each); controllers translate `IllegalArgumentException`/`IllegalStateException` into `JsfUtil` error messages, as `PaymentSettlementController` does today.
- Double-submit: controller-level `submitting` flag (existing pattern) plus the service re-check of request status makes approve/reject idempotent-safe.
- Concurrency: a second approver acting on an already-approved request gets "Request is no longer pending".

## 8. Documentation

- Wiki `Non-Cash-Settlement.md`: rewrite Steps 4–5 (submit for approval, approval, rejection/withdrawal), add "Approving a Settlement", the shift/handover block, the drawer effect, and the new privilege. Keep "Cash Book is not updated".
- Wiki `Finance-Starting-and-Ending-a-Shift.md`: add the pending-settlement block.
- `developer_docs/cashier-drawer-cashbook-master-design.md` §4: replace "Approval workflow … deferred" with a pointer to this spec; add the drawer debit on approval.

## 9. Testing (local, Playwright via menus + DB)

1. User A (has `SettleNonCashPayments`) holds card + cheque payments after an accepted handover. Settle Non-Cash → select → Submit for Approval. Verify: bill created, Request PENDING, payments `paymentSettlementBill` set, `paymentSettled=false`, `currentHolder=A`, drawer unchanged.
2. As A: Shift End → blocked; Handover Create → blocked. Settle Non-Cash no longer lists the locked payments.
3. As A with approval privilege: approve own request → refused.
4. User B approves → payments settled, `currentHolder=null`, A's drawer card and cheque in-hand reduced by the exact sums, `DrawerEntry` rows tagged to A; A can now end shift and create handover; locked payments absent from handover.
5. New request → B rejects with reason → payments back in A's pending list, bill retired, gate lifted, drawer unchanged.
6. New request → A withdraws → same as 5 with status CANCELLED.
7. Hospital/user with no settlement activity: shift end and handover behave exactly as before.
8. Regression: handover create/accept and shift end for a user with no pending requests.

## 10. Out of Scope

- Cash-book postings for settlement (D3).
- Correcting historic drawer balances inflated by legacy settlements (D7).
- A UI for cancelling approved settlements.
- Amount-based approval thresholds or multi-level approval.
