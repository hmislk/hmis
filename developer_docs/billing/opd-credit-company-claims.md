# OPD Credit Company Claims — Business Rules

**Applies to:** claims raised to a credit company for credit bills billed outside the inpatient flow (issue #25847).
Screens: Financial Transaction Manager → Payment Management → Credit Company Payments → **Credit Company Claims**, and **Create Claim** on the OPD credit company due page.

## Which bills can be claimed

- OPD bills, package bills, and pharmacy retail and wholesale sales billed to the credit company.
- Pharmacy GRN wholesale bills cannot be claimed.
- A claim holds bills of one credit company only, with a due amount above 0.10 (the same cut-off as OPD Due Search; due = net total less refunds less settlements, to 2 decimal places).
- A bill can be in only one open claim at a time. A claim is open unless it is Cancelled, Settled or Closed.

## Claim number

- Every claim gets its own number from a counter per department and year (bill type `OPD_CREDIT_COMPANY_CLAIM`).
  This holds whatever the hospital's bill numbering setting is.
- The credit company's own reference is stored separately on the claim (entered at Submit or Resubmit).

## Status flow

| From | Can move to |
|---|---|
| Draft | Submitted, Cancelled |
| Submitted / Resubmitted | Rejected, Partially Settled, Settled, Closed |
| Rejected | Resubmitted, Closed, Cancelled |
| Partially Settled | Settled, Closed |
| Settled, Closed, Cancelled | nothing (final) |

- Rejecting records the reason, user and time.
- A rejected claim keeps its bills and is resubmitted with the same claim number. It is never re-created.
- A Partially Settled claim can be closed with a reason (needed), by a user with the Create Claim privilege. Closing records the reason, user and time, and frees the claim's bills. It changes no bill balance: what the company has not paid stays due on the bills. (A pending write-off request is not checked yet; follow-up once the write-off part is merged.)
- Cancelling is allowed only for a Draft or Rejected claim, by a user with the Create Claim privilege. Cancelling frees its bills for a new claim.

## Locks on the bills

- A bill in an open claim cannot be refunded, returned or cancelled. A Draft or Rejected claim can be cancelled, which frees its bills. A Submitted or Resubmitted claim keeps its bills locked until the claim is rejected (then cancelled) or settled.
- A bill with a write-off cannot be refunded or cancelled until the write-off is cancelled.

## Settlement

- Payment is recorded per bill through Outpatient Credit Collection (or Settle Selected Bills in OPD Due Search), as for any credit bill. Nothing changes in how payment is recorded; partial payment is per bill.
- On a Submitted, Resubmitted or Partially Settled claim, **Settle** opens Outpatient Credit Collection with the claim's company selected and its bills that still have a due added. Amounts can be changed per bill as usual.
- When a credit collection bill is saved, each paid bill that is in an open claim gets a link row (settlement bill, claim, claimed bill, amount). The claim page shows the settled amount (active links), the outstanding amount (due on its bills) and the list of settlement bills.
- The claim status then follows the settlement (a system change, not a user action):
  - every bill in the claim has a due of 0.10 or less: **Settled**;
  - otherwise some amount is linked: **Partially Settled**;
  - otherwise nothing is linked: back to **Submitted** (or **Resubmitted** if it was resubmitted).
- Cancelling a settlement bill (Reports and Audits, Payment Done Search) retires its link rows and recomputes each claim. A Settled claim reopens to Partially Settled, Submitted or Resubmitted. If one of its bills is by then in another open claim, the claim is left Settled and a warning is logged.
- Links are made for Submitted, Resubmitted, Partially Settled, Draft and Rejected claims, but only Submitted, Resubmitted, Partially Settled and Settled claims change status. A Draft or Rejected claim keeps its status until it is submitted or resubmitted; then its status is recomputed at once, so a claim whose bills were already paid goes straight to Partially Settled or Settled.
- If linking fails after a payment or cancellation was saved, the payment stands and the user is told to inform the administrator.
- Payment Vouchers are not part of claims.

## Write-off (rule agreed; screens not built yet)

- A write-off needs two users: one requests it, a different user with the approve privilege approves it. Only an approved write-off reduces the due.

## Privileges

| Privilege | Allows |
|---|---|
| `OpdCreditClaimCreate` | Create Claim, Cancel Claim |
| `OpdCreditClaimSubmit` | Submit |
| `OpdCreditClaimReject` | Record Rejection |
| `OpdCreditClaimResubmit` | Resubmit |
| `OpdCreditClaimSettle` | Settle (opens Outpatient Credit Collection for the claim) |
| `OpdCreditClaimReports` | Claims by Status and Claim Ageing reports |

## Reports

Under Credit Company Payments → Outpatient Credit Management, next to Credit Company Claims. Both need the `OpdCreditClaimReports` privilege.

- **Claims by Status**: filters are claim created date range (required), credit company and status (both optional, any status). One row per claim: claim no, company, department, created, submitted, status, resubmit count, claimed total, outstanding, and "Received / closed". Footer totals; Excel export.
- **Outstanding** of a claim = the sum of the current due of its bills (net total less refunds less settlements), read now, not as at the claim date. **Received / closed** = claimed total less outstanding. A cancelled claim shows 0 outstanding and 0 received / closed (its bills were freed), and is left out of the footer totals.
- **Claim Ageing**: claims in Submitted, Resubmitted or Partially Settled with an outstanding above 0.10, as of now. Age = whole days since the claim's `submittedAt`. A resubmit overwrites `submittedAt` with the resubmit date, so for a Resubmitted claim the age runs from the latest resubmission. Bands per credit company: 0-30, 31-60, 61-90, over 90 days, then total; footer totals; Excel export. There is no claim-level drill-down yet.

## Code

- Entities: `CreditCompanyClaim`, `CreditCompanyClaimItem`, `CreditCompanyClaimSettlement` (link, migration v2.27.0); status enum `CreditCompanyClaimStatus` (holds the flow above).
- Rules and locks: `CreditCompanyClaimService` (`checkBillEligible`, `checkTransition`, `openClaimRefusal`). Business errors throw `CreditCompanyClaimException` (rolls back).
- Settlement: `recordSettlement` (called from `CashRecieveBillController` after each paid bill), `reverseSettlement` (from `CreditCompanyBillSearch.cancelBill`), `recomputeStatus` and the pure rule `statusAfterSettlement`. System status changes use `CreditCompanyClaimStatus.getSystemNext()`, separate from the user flow in `getAllowedNext()`.
- Every refund, return and cancel path for a claimable bill calls `openClaimRefusal` before it saves.

Related: [cancellation-vs-return-policy.md](cancellation-vs-return-policy.md), [credit-company-balance-management.md](credit-company-balance-management.md).
