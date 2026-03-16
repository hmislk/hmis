# Daily Return vs All Cashier Summary — Design Intent & Reconciliation

## Core Design Principle

These two financial reports count **different things by design**:

| Report | Counts | Source |
|---|---|---|
| **Daily Return** | Bills | `bill` table directly |
| **All Cashier Summary** | Payments | `payment` table, joined to `bill` |

This is intentional. A bill and its payment are two separate events. The reports are **complementary cross-checks**, not duplicates.

## Expected Behaviour

- **Daily Return** shows what was billed/received from a financial transaction perspective.
- **All Cashier Summary** shows what was actually collected by cashiers (cash, card, cheque, etc. recorded as Payment records).
- Under normal operation the totals reconcile. Any discrepancy indicates a data integrity problem.

## Using the Discrepancy as a Diagnostic Tool

If `All Cashier Summary total < Daily Return total` for the same period, the gap equals the value of **bills that exist without corresponding Payment records**. This can be caused by:

- A system failure during bill creation where the bill was persisted but payment creation threw an exception (see [GitHub #19205](https://github.com/hmislk/hmis/issues/19205))
- A partial transaction rollback leaving an orphan bill
- Manual data corrections applied to one table but not the other

To find the orphan bills:

```sql
SELECT b.ID, b.DEPTID, b.BILLTYPEATOMIC, b.NETTOTAL, b.CREATEDAT
FROM bill b
LEFT JOIN payment p ON p.BILL_ID = b.ID
WHERE p.ID IS NULL
  AND b.RETIRED = 0
  AND b.BILLDATE BETWEEN :fromDate AND :toDate
  AND b.BILLTYPEATOMIC IN (
      'CC_PAYMENT_RECEIVED_BILL',
      'OPD_BILL',
      -- add other cash-in bill types as needed
  )
ORDER BY b.CREATEDAT;
```

## Known Incident — Ruhunu Hospital, 2025-09-25

A `NullPointerException` in `PaymentService.populatePaymentDetails()` (Cash case) caused 4 `CC_PAYMENT_RECEIVED_BILL` records to be saved without Payment records between 11:51–12:41 IST. The NPE was fixed at ~14:10 by commit `e193ffc925`. The orphan bills created a 29,650.00 discrepancy between the two reports for the Sept 16–30 period.

**Bills affected**: RHDOM//25/073072, 073074, 073077, 073095
**Fix applied**: Null-safe checks added in `PaymentService.populatePaymentDetails()`
**Remaining issue**: No rollback or error is shown to the user if payment creation silently returns empty — tracked in [GitHub #19205](https://github.com/hmislk/hmis/issues/19205)

## Development Rule

When modifying bill creation workflows:

1. **Bill save and payment creation must be treated as an atomic operation.** If the payment cannot be created, the bill must be rolled back or retired — never left as an orphan.
2. **Always validate the payment list returned by `paymentService.createPayment()`** before calling `updateCcBalance()` or showing a success message.
3. **Do not "fix" discrepancies by changing report logic** — the discrepancy is the signal. Fix the data integrity issue instead.
