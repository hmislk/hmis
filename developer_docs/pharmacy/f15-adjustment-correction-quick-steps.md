# Correcting the F15 adjustment values — quick steps

Short version of the [full runbook](f15-adjustment-bfd-backfill-runbook.md). Follow that one
for the SQL, the rollback snapshot and the reasoning; this page is the click-by-click order.

Issue #23411. PR #23427.

---

## ⚠️ Read this first

**Coop staging uses the live coop production database.**

`https://stg-migrated.carecode.org/coop/` connects through `jdbc/coopStg` →
`jdbc:mysql://10.30.2.6:3306/coop`, which is db3 — the same schema the hospital is using
right now. Verified 2026-09-02 on the staging VM.

So on coop staging:

| Button | Safe? |
|---|---|
| **Preview …** | Yes. Computes and prints, saves nothing. |
| **Backfill …** | **No — this writes to production.** |

There is no such thing as "trying it on staging first" for coop. The Preview button is the
try-it-first. Treat every Backfill click as a production change.

---

## Step 1 — Merge and deploy

1. Merge PR #23427 into `development`.
2. Deploy to coop staging: push `development` into `coop-stg-migrated`, which triggers the
   `COOP-STG Build & Deployment Pipeline` workflow. (It can also be started manually —
   the workflow has `workflow_dispatch`.)
3. Wait for the workflow to go green and check the app is up:
   `https://stg-migrated.carecode.org/coop/faces/index1.xhtml`

## Step 2 — Record what F15 shows now

So the change can be shown to be an improvement rather than asserted to be one.

1. Go to **Pharmacy → Reports → Pharmacy Analytics → Summary Reports → F15 Daily Stock Values**
   (`/faces/pharmacy/reports/summary_reports/daily_stock_values_report_optimized.xhtml`)
2. Pick a date in the affected period — **2026-07-06** is a good one — set Department to
   **Main Pharmacy**, click **Generate Report**.
3. Screenshot the **Adjustment Transactions** section. Today it shows **0.00**.

## Step 3 — Preview (saves nothing)

1. Go to **`/faces/dataAdmin/admin_functions.xhtml`** — the Admin Backfill page. Needs the
   `Admin` privilege.
2. Set the **From** and **To** dates at the top of the page. **Do one month at a time**
   — start with `2026-06-01` → `2026-06-30`.
3. Open the **Pharmacy** section (click the "Pharmacy" accordion header to expand it).
4. Find the row **"Backfill BFD for Retail Rate Adjustment Bills"**.
5. Click **Preview Retail Rate BFDs**.

You get a report like this in the right-hand column:

```text
=== DRY RUN - nothing was saved - Retail Rate Adjustment BFD Backfill ===
Candidates in range: 2
Would correct: 2
Skipped:       0 (nothing to correct)
Unresolved:    0 (stored values could not be interpreted - left untouched)
Errors:        0
Net value that would be added to F15: -267311.70

Bill No           | Date             | Status       | Reading  | Net value
------------------|------------------|--------------|----------|-------------
MP//26/034398     | 2026-06-30 12:31 | WOULD_UPDATE | VALUE    | -25031.20
MP//26/034399     | 2026-06-30 12:31 | WOULD_UPDATE | VALUE    | -242280.50
```

## Step 4 — Check the preview before writing anything

**Stop and ask if any of these are true:**

- `Unresolved` is not 0 — the stored values on those bills could not be read with
  confidence. Investigate before continuing; do not apply hoping the rest is fine.
- `Errors` is not 0.
- A **Net value** looks wrong for the item — an order of magnitude away from what a price
  change on that quantity should be.
- A single bill is large enough to matter to the accounts. The arithmetic can be right
  while the adjustment itself was somebody's mistake. Confirm with the pharmacy.

The **Reading** column says how each bill's stored values were interpreted — `RATE` for
bills entered through the pharmacy page, `VALUE` for ones created through the API. Both are
expected; you do not need to do anything about it.

## Step 5 — Apply

Only after step 4 is clean, and only with agreement that production may be written.

1. Same page, same date range.
2. Click **Backfill Retail Rate Adjustment BFDs** and confirm the dialog.
3. The output now says how many bills were corrected instead of "DRY RUN".

If it reports an **error**, the whole run was rolled back — one click is one transaction, so
nothing was saved. Re-run the Preview to confirm the range is untouched before trying again.

## Step 6 — Check F15 again

1. Back to F15, same date and department as step 2.
2. The **Adjustment Transactions** section now shows the values instead of 0.00.
3. The section total should equal the **Net value** the preview reported for that range.

Then re-run the **Preview** for the same range: it should say **`Would correct: 0`**.
(`Candidates in range` may stay above zero — bills whose real change is zero are listed as
`Skipped` every time. That is fine.)

## Step 7 — Repeat, then do stock adjustments

- Repeat steps 3–6 for **July 2026** (`2026-07-01` → `2026-07-31`).
- Then the same six steps using the **Preview / Backfill Stock Adjustment BFDs** buttons,
  one row above. Coop has about 2,320 stock-adjustment bills needing repair — May, June,
  July and August 2026 — so work month by month there too.

## Step 8 — Write down what was done

Date range, which button, the counts from the preview, the net value added, who approved it.
Each corrected bill also carries a `[BFD Backfill]` note in its bill comments recording the
time, the reading used and the values written.

---

## Getting the fix onto coop production

Steps 1–8 correct the **data**, and because coop staging shares the production database
that correction is already live for the hospital's numbers.

But the **code** fix — the one that stops new bills being written wrongly — is still only on
staging. Coop production runs the `coop-prod-migrated` branch and needs its own deployment.
Decide with the team whether that goes out as part of a normal release or as a hotfix.

## Scope beyond coop

The missing-BFD problem is not coop-only, and coop is not the largest:

| Hospital | Stock-adjustment bills with no BFD |
|---|---|
| rmh | ~3,268 |
| coop | ~2,321 |
| asiri | ~1,810 |
| ruhunu | 14 |

Same steps apply at each, once the code is deployed there. `suwani` predates the
`BillFinanceDetails` table and is not applicable.
