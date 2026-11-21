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
- Cancelling is allowed only for a Draft or Rejected claim, by a user with the Create Claim privilege. Cancelling frees its bills for a new claim.

## Locks on the bills

- A bill in an open claim cannot be refunded, returned or cancelled. A Draft or Rejected claim can be cancelled, which frees its bills. A Submitted or Resubmitted claim keeps its bills locked until the claim is rejected (then cancelled) or settled (the settlement link comes in a later part).
- A bill with a write-off cannot be refunded or cancelled until the write-off is cancelled.

## Settlement and write-off (rules agreed; the claim link and write-off screens are not built yet)

- Payment is recorded per bill through Outpatient Credit Collection, as for any credit bill. The claim shows the total.
- Payment Vouchers are not part of claims.
- A write-off needs two users: one requests it, a different user with the approve privilege approves it. Only an approved write-off reduces the due.

## Privileges

| Privilege | Allows |
|---|---|
| `OpdCreditClaimCreate` | Create Claim, Cancel Claim |
| `OpdCreditClaimSubmit` | Submit |
| `OpdCreditClaimReject` | Record Rejection |
| `OpdCreditClaimResubmit` | Resubmit |
| `OpdCreditClaimReports` | Claims by Status and Claim Ageing reports |

## Reports

Under Credit Company Payments → Outpatient Credit Management, next to Credit Company Claims. Both need the `OpdCreditClaimReports` privilege.

- **Claims by Status**: filters are claim created date range (required), credit company and status (both optional, any status). One row per claim: claim no, company, department, created, submitted, status, resubmit count, claimed total, outstanding, and "Received / closed". Footer totals; Excel export.
- **Outstanding** of a claim = the sum of the current due of its bills (net total less refunds less settlements), read now, not as at the claim date. **Received / closed** = claimed total less outstanding.
- **Claim Ageing**: claims in Submitted, Resubmitted or Partially Settled with an outstanding above 0.10, as of now. Age = whole days since the claim's `submittedAt`. A resubmit overwrites `submittedAt` with the resubmit date, so for a Resubmitted claim the age runs from the latest resubmission. Bands per credit company: 0-30, 31-60, 61-90, over 90 days, then total; footer totals; Excel export. There is no claim-level drill-down yet.

## Code

- Entities: `CreditCompanyClaim`, `CreditCompanyClaimItem`; status enum `CreditCompanyClaimStatus` (holds the flow above).
- Rules and locks: `CreditCompanyClaimService` (`checkBillEligible`, `checkTransition`, `openClaimRefusal`). Business errors throw `CreditCompanyClaimException` (rolls back).
- Every refund, return and cancel path for a claimable bill calls `openClaimRefusal` before it saves.

Related: [cancellation-vs-return-policy.md](cancellation-vs-return-policy.md), [credit-company-balance-management.md](credit-company-balance-management.md).
