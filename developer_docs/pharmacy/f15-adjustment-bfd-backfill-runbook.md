# Runbook — correcting F15 adjustment values (BillFinanceDetails backfill)

**Applies to:** pharmacy adjustment bills that show Rs. 0.00 in the F15 Daily Stock Values
report's "Adjustment Transactions" section, and in its Level 1 / Level 2 drill-downs.

**Issue:** #23411. Earlier related work: #22580, #18774.

---

## 1. What is actually wrong

F15's adjustment section reads `BillFinanceDetails` (BFD), not `bill.netTotal`. Bills whose
BFD was never written, or was written with zero values, therefore show 0.00 even though a
real stock or price movement happened.

Two separate causes produced such bills:

| Cause | Bill types affected | Fixed in code by |
|---|---|---|
| `BillFinanceDetails` attach intermittently failed to persist | `PHARMACY_STOCK_ADJUSTMENT` | #22580 |
| The save path never wrote a BFD or `bill.total` at all | `PHARMACY_RETAIL_RATE_ADJUSTMENT` (single-item page) | #23411 |

## 2. Why you must preview before applying

Until #23411, two writers disagreed about the meaning of
`PharmaceuticalBillItem.beforeAdjustmentValue` / `afterAdjustmentValue` on a rate adjustment:

* **RATE** — the UI page stored the old and new **unit rates**.
* **VALUE** — the adjustment REST API stored **qty x rate**, the extended value on each side.

The old backfill assumed RATE unconditionally. On an API-written bill that multiplies an
already-extended value by the quantity a second time. On coop production it would have
written **-262,874,342.50** into F15 for bill `MP//26/034399`, whose real value change is
**-242,280.50** — a 1085x overstatement, and it would have looked like a plausible figure.

The current backfill resolves the convention per line by testing each reading against
`billItem.netValue` (the signed change value, written correctly by both paths). A line that
matches neither is reported as **UNRESOLVED** and left untouched. Nothing is guessed.

**Always run the Preview and read the figures before applying.** A single wrong bill here
lands directly in a financial report.

## 3. Where to run it

**Admin Backfill page:** `/faces/dataAdmin/admin_functions.xhtml` → **Pharmacy** accordion.
Requires the `Admin` privilege.

Four buttons, in two pairs:

| Button | Effect |
|---|---|
| Preview Stock Adjustment BFDs | Computes and reports. Writes nothing. |
| Backfill Stock Adjustment BFDs | Applies. |
| Preview Retail Rate BFDs | Computes and reports. Writes nothing. |
| Backfill Retail Rate Adjustment BFDs | Applies. |

The date range comes from the From/To pickers at the top of the page. The same computation
is also reachable at `POST /api/pharmacy/backfill_bfd` and through the adjustment API's own
backfill endpoint — all three share `PharmacyBfdBackfillService`, so they cannot disagree.

## 4. Procedure

### 4.1 Establish the baseline

Before touching anything, record what F15 currently reports, so the change can be shown to
be an improvement rather than asserted to be one.

1. Open F15 (`/faces/pharmacy/reports/summary_reports/daily_stock_values_report_optimized.xhtml`)
   for a date in the affected range and screenshot the Adjustment Transactions section.
2. Count what needs repair:

```sql
SELECT b.billTypeAtomic,
       DATE_FORMAT(b.createdAt, '%Y-%m') AS month,
       COUNT(*) AS bills,
       SUM(b.billFinanceDetails_id IS NULL) AS missing_bfd,
       SUM(b.total = 0) AS zero_total
FROM bill b
WHERE b.retired = 0
  AND b.billTypeAtomic IN ('PHARMACY_STOCK_ADJUSTMENT', 'PHARMACY_RETAIL_RATE_ADJUSTMENT')
GROUP BY 1, 2
ORDER BY 1, 2;
```

### 4.2 Preview

Set the From/To dates and click the matching **Preview** button. Work one month at a time on
a large population — the preview lists every bill it would change, and a month is a length of
output someone will actually read.

The preview reports:

```
=== DRY RUN - nothing was saved - Retail Rate Adjustment BFD Backfill ===
Candidates in range: 27
Would correct: 27
Skipped:       0 (nothing to correct)
Unresolved:    0 (stored values could not be interpreted - left untouched)
Errors:        0
Net value that would be added to F15: -1090203.92

Bill No           | Date             | Status       | Reading  | Net value
------------------|------------------|--------------|----------|-------------
OP//26/000344     | 2026-01-01 17:18 | WOULD_UPDATE | RATE     | 253.23
MP//26/034399     | 2026-06-30 12:31 | WOULD_UPDATE | VALUE    | -242280.50
...
```

### 4.3 Check the preview before applying

- **Unresolved > 0** — stop and investigate those bills individually. Do not apply hoping the
  rest is fine; find out what wrote them first.
- **Errors > 0** — stop. Read the note against each bill.
- **A net value that looks implausible** — an order of magnitude away from the item's price
  times the quantity — stop. That is the signature of the semantics bug this tool exists to
  avoid, and it means an assumption has broken somewhere.
- **Large individual movements** — anything material should be confirmed with the pharmacy
  before it is written into a financial report. The arithmetic can be right while the
  underlying adjustment was a mistake someone has since forgotten about.

Spot-check a few rows against the source data:

```sql
SELECT b.id, b.deptId,
       bi.qty,
       p.beforeAdjustmentValue AS before_val,
       p.afterAdjustmentValue  AS after_val,
       bi.netValue             AS signed_change,
       (p.afterAdjustmentValue - p.beforeAdjustmentValue) * bi.qty AS as_rates,
       (p.afterAdjustmentValue - p.beforeAdjustmentValue)          AS as_values
FROM bill b
JOIN billitem bi ON bi.bill_id = b.id
JOIN pharmaceuticalbillitem p ON p.billItem_id = bi.id
WHERE b.id IN (/* bill ids from the preview */);
```

Whichever of `as_rates` / `as_values` equals `signed_change` is the reading the tool used, and
`signed_change` is the value it writes.

### 4.4 Apply

Click the matching **Backfill** button and confirm. For each corrected bill it writes:

- `BillFinanceDetails`: `grossTotal`, `netTotal`, `totalRetailSaleValue`, `totalCostValue`,
  `totalPurchaseValue`, `totalQuantity`, `totalBeforeAdjustmentValue`, `totalAfterAdjustmentValue`
- `bill.total` and `bill.netTotal` — reports that read these rather than the BFD would
  otherwise stay at zero
- an audit block appended to `bill.comments`

### 4.5 Verify

```sql
-- Every corrected bill should agree with its own recorded change value.
SELECT b.id, b.deptId,
       ROUND(f.netTotal, 2)  AS bfd_net,
       ROUND(bi.netValue, 2) AS expected,
       CASE WHEN ABS(f.netTotal - bi.netValue) < 0.01 THEN 'MATCH' ELSE 'MISMATCH' END AS verdict
FROM bill b
JOIN billitem bi ON bi.bill_id = b.id
JOIN billfinancedetails f ON f.id = b.billFinanceDetails_id
WHERE b.billTypeAtomic = 'PHARMACY_RETAIL_RATE_ADJUSTMENT'
  AND b.retired = 0
  AND b.comments LIKE '%[BFD Backfill]%';
```

Then reopen F15 for the same date as the baseline and confirm the Adjustment Transactions
section now shows the values, and that the section total equals the preview's reported net.

Re-running the Preview over the same range should now report `Candidates in range: 0`. The
operation is idempotent; a bill whose genuine value change is zero is reported as SKIPPED
every time, which is correct rather than a failure.

### 4.6 Record it

Note in the issue or change log: the date range, which button, the counts from the preview,
the net value added, and who approved it. `bill.comments` carries the same information
per bill, including which reading was used.

## 5. Rollback

There is no undo button. Each corrected bill carries a `[BFD Backfill]` block in
`bill.comments` recording the time, the reading used, and the values written, so a run is
identifiable and reversible by hand:

```sql
SELECT b.id, b.deptId, b.total, b.netTotal, b.comments
FROM bill b
WHERE b.comments LIKE '%[BFD Backfill]%'
  AND b.comments LIKE '%2026-09-02%';   -- the run date
```

To reverse, null out `bill.billFinanceDetails_id`, delete the orphaned `billfinancedetails`
row, and reset `bill.total` / `bill.netTotal` to 0. Take a backup of the affected rows before
applying if the range is large.

## 6. Notes and limits

- **Cost values on stock adjustments.** `pharmaceuticalbillitem.costRate` is 0 on most
  historical bills. The service falls back to the `StockHistory` snapshot taken at the moment
  of the adjustment, and only then to the item batch's current rate. When it has to use the
  current rate it says so in the note, because the batch rate may have moved since — the
  retail value is exact, the cost value is an estimate.
- **Wholesale and cost rate adjustments** are not supported: no writer populates the audit
  fields for them, so there is nothing to reconstruct from.
- **The `suwani` schema** predates `BillFinanceDetails` entirely — the tool does not apply.
- **Other hospitals.** As of 2026-09-02 the missing-BFD problem is fleet-wide (asiri ~1810
  bills, rmh ~3268, coop ~2340, ruhunu 14), while the VALUE-semantics rows exist only at coop,
  which is the only site where the adjustment API has been used. The procedure is the same
  everywhere; the preview will say which reading applies.
