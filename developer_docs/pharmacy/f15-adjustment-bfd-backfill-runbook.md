# Runbook — correcting F15 adjustment values (BillFinanceDetails backfill)

**Applies to:** pharmacy adjustment bills that show Rs. 0.00 in the F15 Daily Stock Values
report's "Adjustment Transactions" section, and in its Level 1 / Level 2 drill-downs.

**Issue:** #23411. Earlier related work: #22580, #18774.

**In a hurry?** [Quick steps](f15-adjustment-correction-quick-steps.md) is the
click-by-click order without the reasoning.

> **Coop staging writes to production.** The coop staging app's datasource resolves to
> the live coop production database — the same schema the hospital is using. On coop there
> is no "try it on staging first": the Preview button is the try-it-first, and every
> Backfill click is a production change. Verified 2026-09-02; see
> [Confirming which database an environment uses](#confirming-which-database-an-environment-uses).

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
-- Bound this to the same range you will give the backfill, or the counts cannot be
-- compared with what F15 shows for that range.
SELECT b.billTypeAtomic,
       DATE_FORMAT(b.createdAt, '%Y-%m') AS month,
       COUNT(*) AS bills,
       SUM(b.billFinanceDetails_id IS NULL) AS missing_bfd,
       SUM(b.total = 0) AS zero_total
FROM bill b
WHERE b.retired = 0
  AND b.billTypeAtomic IN ('PHARMACY_STOCK_ADJUSTMENT', 'PHARMACY_RETAIL_RATE_ADJUSTMENT')
  AND b.createdAt BETWEEN '2026-06-01 00:00:00' AND '2026-07-31 23:59:59'
GROUP BY 1, 2
ORDER BY 1, 2;
```

### 4.2 Preview

Set the From/To dates and click the matching **Preview** button. Work one month at a time on
a large population — the preview lists every bill it would change, and a month is a length of
output someone will actually read.

The preview reports:

```text
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
-- bfd.netTotal is one figure per BILL, so the line values must be summed per bill first;
-- comparing it against each joined billitem row reports false MISMATCHes on multi-line bills.
SELECT b.id, b.deptId,
       ROUND(f.netTotal, 2)       AS bfd_net,
       ROUND(SUM(bi.netValue), 2) AS expected,
       CASE WHEN ABS(f.netTotal - SUM(bi.netValue)) < 0.01 THEN 'MATCH' ELSE 'MISMATCH' END AS verdict
FROM bill b
JOIN billitem bi ON bi.bill_id = b.id AND bi.retired = 0
JOIN billfinancedetails f ON f.id = b.billFinanceDetails_id
WHERE b.billTypeAtomic = 'PHARMACY_RETAIL_RATE_ADJUSTMENT'
  AND b.retired = 0
  AND b.comments LIKE '%[BFD Backfill]%'
GROUP BY b.id, b.deptId, f.netTotal;
```

For a purchase rate adjustment the same check applies, but its value sits in
`f.totalPurchaseValue` with `f.totalRetailSaleValue` at zero by design.

Then reopen F15 for the same date as the baseline and confirm the Adjustment Transactions
section now shows the values, and that the section total equals the preview's reported net.

Re-run the Preview over the same range. The check is **`Would correct: 0`**, not
`Candidates in range: 0` — a bill whose genuine value change is zero stays a candidate on
every run and is reported SKIPPED, which is correct rather than a failure. Investigate any
`Unresolved` or `Errors` before considering the range done.

### 4.6 Record it

Note in the issue or change log: the date range, which button, the counts from the preview,
the net value added, and who approved it. `bill.comments` carries the same information
per bill, including which reading was used.

## 5. Rollback

**Take a snapshot before every apply, whatever the size of the range.** The backfill also
repairs BFDs that already exist, so there is no safe generic undo: deleting the BFD and
zeroing the totals would discard fields the backfill never touched, and would be wrong for
any bill that had legitimate values before the run.

Name the snapshot for the run, not the day. The procedure has you apply month by month and
type by type, so a date-only name would collide on the second run of a day — `CREATE TABLE`
would fail, or worse you would restore from the wrong range. Use bill type + range:

```sql
-- Run this BEFORE each apply, with exactly the same type and range you are about to apply.
-- Naming: bfd_snap_<short type>_<from>_<to>
CREATE TABLE bfd_snap_retailrate_20260601_20260630 AS
SELECT b.id AS bill_id, b.total, b.netTotal, b.comments, b.billFinanceDetails_id,
       f.grossTotal, f.netTotal AS bfd_net, f.totalRetailSaleValue, f.totalCostValue,
       f.totalPurchaseValue, f.totalQuantity,
       f.totalBeforeAdjustmentValue, f.totalAfterAdjustmentValue
FROM bill b
LEFT JOIN billfinancedetails f ON f.id = b.billFinanceDetails_id
WHERE b.retired = 0
  AND b.billTypeAtomic = 'PHARMACY_RETAIL_RATE_ADJUSTMENT'
  AND b.createdAt BETWEEN '2026-06-01 00:00:00' AND '2026-06-30 23:59:59';
```

To reverse, restore each bill's `total`, `netTotal`, `comments` and BFD columns from that
snapshot inside one transaction. Only delete a `billfinancedetails` row where the snapshot
shows `billFinanceDetails_id` was NULL — i.e. the backfill created it.

Afterwards a run is identifiable from the `[BFD Backfill]` block appended to
`bill.comments`, which records the time, bill type, reading used and values written. Match
on the range you snapshotted rather than the date alone — several runs can share a date:

```sql
SELECT b.id, b.deptId, b.billTypeAtomic, b.total, b.netTotal, b.comments
FROM bill b
WHERE b.comments LIKE '%[BFD Backfill]%'
  AND b.billTypeAtomic = 'PHARMACY_RETAIL_RATE_ADJUSTMENT'
  AND b.createdAt BETWEEN '2026-06-01 00:00:00' AND '2026-06-30 23:59:59';
```

To distinguish two runs over the same bills, put something identifying in the audit comment
the tool records — the admin page writes a fixed comment, so if you need a finer trail use
`POST /api/pharmacy/backfill_bfd`, whose `auditComment` you control.

### One run is one transaction

The service is a `@Stateless` EJB with the default `REQUIRED` attribute, so a single click
of a Backfill button is a single transaction: every bill in the range commits together, or
none does. A persistence failure aborts the whole run and the page reports the error — it
will not silently save some bills and skip others. If a run reports an error, treat the
range as untouched and re-run the Preview to confirm before trying again.

## 6. Confirming which database an environment uses

Never assume an environment named "staging" has its own data. Check before writing:

1. Connection details for every environment live in the operations credentials store
   outside this repository, not here.
2. On the app server, read the datasource the app is deployed with and follow it to its
   pool, then read that pool's `url` property:

   ```bash
   grep -A2 'jndi-name="<the app's datasource>"' \
     /opt/payara5/glassfish/domains/domain1/config/domain.xml
   # then find the matching <jdbc-connection-pool name="..."> and read its url property
   ```

3. Compare the host and schema in that URL against the production entry in the credentials
   store. If they match, the environment is production for all write purposes, whatever it
   is called.

## 7. Notes and limits

- **Cost values on stock adjustments.** `pharmaceuticalbillitem.costRate` is 0 on most
  historical bills. The service falls back to the `StockHistory` snapshot taken at the moment
  of the adjustment, and only then to the item batch's current rate. When it has to use the
  current rate it says so in the note, because the batch rate may have moved since — the
  retail value is exact, the cost value is an estimate.
- **Purchase rate adjustments** are supported, but only through the API paths — there is no
  admin button for them, because none has been asked for. Their value lands in the purchase
  column and their retail value is zero *by design*: a purchase rate change does not alter
  what the stock sells for. Candidate selection therefore tests `bfd.netTotal`, the bill's
  primary value change, and never the retail column — testing retail would mark every
  correctly written purchase-rate bill as broken and rewrite it on every run.
- **Wholesale and cost rate adjustments** are not supported: no writer populates the audit
  fields for them, so there is nothing to reconstruct from.
- **The `suwani` schema** predates `BillFinanceDetails` entirely — the tool does not apply.
- **Other hospitals.** As of 2026-09-02 the missing-BFD problem is fleet-wide (asiri ~1810
  bills, rmh ~3268, coop ~2340, ruhunu 14), while the VALUE-semantics rows exist only at coop,
  which is the only site where the adjustment API has been used. The procedure is the same
  everywhere; the preview will say which reading applies.
