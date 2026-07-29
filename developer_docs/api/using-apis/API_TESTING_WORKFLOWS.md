# API Testing Guide: Verifying Payments & Balances

How to verify, via the read-only APIs, that a bill's payments produced the correct balance
updates. Useful for confirming a bill was processed correctly or diagnosing a balance
discrepancy — not for creating bills.

## APIs Used

- **Bill details**: `GET /api/costing_data/bill?number=<billNumber>` or
  `GET /api/costing_data/by_bill_id/{id}` → returns the bill including its `payments[]` array.
- **Balance history** (`/api/balance_history/...`), filterable by `billId`, plus an entity ID,
  `fromDate`/`toDate`, `limit` (default 100):

| Payment Method | History Endpoint | Balance Field Pattern |
|---|---|---|
| Cash / Card / Cheque | `/drawer_entries` | `beforeBalance` → `afterBalance` |
| PatientDeposit | `/patient_deposits` | `balanceBeforeTransaction` → `balanceAfterTransaction` (decreases) |
| Credit (credit company) | none — no history tracking; verify manually | — |
| Staff_Welfare | `/staff_welfare_histories` | `balanceBeforeTransaction` → `balanceAfterTransaction` |
| Agent / collecting centre | `/agent_histories` | `balanceBeforeTransaction` → `balanceAfterTransaction`, check commission split |

All require the `Finance` header.

## Verification Pattern

1. Fetch the bill; assert `payments[]` is non-empty and `sum(p.paidValue) == bill.netTotal`
   (compare floats with `abs(a - b) < 0.01`).
2. For each payment, fetch the matching history endpoint filtered by `billId`.
3. Find the history entry whose `transactionValue` matches the payment's `paidValue`.
4. Assert `afterBalance == beforeBalance + transactionValue` (drawer) or
   `balanceAfterTransaction == balanceBeforeTransaction - transactionValue` (deposit-style
   balances, which decrease on use).

This same before/after arithmetic check applies uniformly regardless of bill type
(`PHARMACY_RETAIL_SALE_WITH_PAYMENT`, `OPD_BATCH_BILL_WITH_PAYMENT`, `INWARD_FINAL_BILL`,
`CHANNEL_BOOKING_WITH_PAYMENT_ONLINE_SETTLED`, etc.) — only which history endpoint(s) apply
changes, per the payment methods present on the bill.

## Refunds, Cancellations, Batches

- **Refunds**: drawer/deposit entries can carry **negative** `transactionValue`; apply the same
  before/after arithmetic check.
- **Cancelled bills**: expect reversing entries rather than absence of entries — verify the net
  effect across all entries for the `billId`, not just the latest one.
- **Batch verification**: loop the single-bill pattern above over a list of bill numbers and
  collect pass/fail per bill rather than failing fast, so one bad bill doesn't hide others.

## Troubleshooting

| Symptom | Cause / Fix |
|---|---|
| `401 Not a valid key` | API key invalid, retired user, or expired — check `ApiKey.dateOfExpiary` |
| Bill not found | Bill number is case-sensitive; confirm exact format (e.g. `PHARM/2025/0001`) |
| Payments array empty | Some bill types legitimately have no payments (transfers, issues) |
| Balance arithmetic mismatch | Use `abs(a - b) < 0.01`, not `==`; also check for other concurrent transactions on the same entity in the window |

## Notes

- Date filters: `yyyy-MM-dd HH:mm:ss`. Default result limit is 100 — pass `limit` for more.
- See [API_COSTING_DATA.md](API_COSTING_DATA.md) for the bill API and
  [API_BALANCE_HISTORY.md](API_BALANCE_HISTORY.md) for the balance history APIs.
