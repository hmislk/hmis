# Reclaiming InnoDB Disk Space with OPTIMIZE TABLE

## Symptom

Production database disk usage is far larger than the logical size of the data would suggest. Typical signal:

- DB size reported by `SUM(data_length + index_length)` is X GB
- A fresh dump-and-restore on a different MySQL server yields a DB **much smaller** than X (often 50–75% smaller)

## Root cause

InnoDB stores rows in B-tree pages. Over years of UPDATEs (and inserts in non-monotonic key order) pages become sparsely filled — sometimes only 30–50% utilised. The *extent-level* free space (`data_free`) stays near zero because pages aren't entirely empty, but the *page-level* density is poor. Disk usage stays high.

A table rebuild (`mysqldump` + restore, or `OPTIMIZE TABLE`) repacks the B-tree to near fill-factor, recovering the wasted space.

## When this matters

- Heavy UPDATE workload over time (billing tables, payment, stock movement)
- INSERTs with non-monotonic primary keys (UUIDs, hash-distributed IDs)
- Rows that churn through many lifecycle state changes

## Diagnostic technique

Do not rely on `data_free` alone — it severely under-reports page-level fragmentation. The reliable way to estimate reclaim is to dump the DB and restore it on a different MySQL server, then compare per-table sizes.

```bash
# Dump (compressed, consistent snapshot, no lock)
mysqldump -h <src-host> -P <src-port> -u <user> -p<password> \
  --single-transaction --quick --routines --triggers --events \
  --set-gtid-purged=OFF --column-statistics=0 --no-tablespaces \
  <db_name> | gzip > dump.sql.gz

# Restore on a separate MySQL server (e.g., a developer machine)
gunzip -c dump.sql.gz | mysql -u <user> -p<password> <db_name>
```

Then compare per-table sizes:

```sql
SELECT table_name,
       ROUND((data_length + index_length) / 1024 / 1024, 1) AS total_mb,
       ROUND(data_free / 1024 / 1024, 1)                   AS free_mb
FROM information_schema.tables
WHERE table_schema = '<db_name>'
ORDER BY (data_length + index_length) DESC
LIMIT 20;
```

Tables where **prod size ≫ restored size** are the reclaim candidates. The delta is page-level waste that `OPTIMIZE TABLE` will recover.

> ⚠️ **MySQL 8 caches `information_schema.tables` stats for 24 hours** (`information_schema_stats_expiry = 86400` by default). For accurate readings before, during, or after an optimize, force a fresh read in the same session:
> ```sql
> SET SESSION information_schema_stats_expiry = 0;
> ```

## What `OPTIMIZE TABLE` actually does

On an InnoDB table, `OPTIMIZE TABLE x` is rewritten internally as `ALTER TABLE x FORCE`, which rebuilds the entire `.ibd` tablespace from scratch. MySQL prints this note, which is **normal and not an error**:

```
Table does not support optimize, doing recreate + analyze instead
```

The rebuild uses online DDL (`LOCK=NONE, ALGORITHM=INPLACE`) by default in MySQL 5.6+. Concurrent DML continues during most of the rebuild — but with caveats (see risk profile below).

## Risk profile

| Risk | Impact | Mitigation |
|---|---|---|
| Brief exclusive metadata lock at start and end of rebuild | DML on the table queues for a short moment | Run during a low-activity window; set `lock_wait_timeout = 60` |
| Needs roughly **2× the table size** as free disk during the swap | Rebuild fails if disk fills | Confirm free disk on the data volume before each big table |
| Sustained IOPS spike for the rebuild duration | Other queries may slow during rebuild | Avoid concurrent managed-backup or replication catch-up windows |
| Long-running open transaction blocks the rebuild from starting | OPTIMIZE waits on metadata lock | Confirm `information_schema.innodb_trx` is empty before starting |
| Cannot be aborted cleanly mid-rebuild | Killing rolls back; time spent is wasted; orphan `#sql-*.ibd` may remain | Budget enough time per table; do not start what you cannot finish |
| Generates significant binlog volume | Replica lag, backup volume growth | Consider replicas in maintenance window planning |

## Pre-flight checks

Run these from a separate session before starting any OPTIMIZE.

```sql
-- 1. Identify the largest tables (your reclaim candidate list)
SET SESSION information_schema_stats_expiry = 0;
SELECT table_name,
       table_rows,
       ROUND((data_length + index_length) / 1024 / 1024, 1) AS total_mb,
       ROUND(data_free / 1024 / 1024, 1)                   AS free_mb
FROM information_schema.tables
WHERE table_schema = '<db_name>'
ORDER BY (data_length + index_length) DESC
LIMIT 20;

-- 2. Long-running InnoDB transactions (must be empty before starting)
SELECT trx_id, trx_state,
       TIMESTAMPDIFF(SECOND, trx_started, NOW()) AS age_sec,
       trx_mysql_thread_id                       AS thread,
       LEFT(trx_query, 80)                       AS query
FROM information_schema.innodb_trx
ORDER BY trx_started
LIMIT 20;

-- 3. Live load snapshot
SHOW GLOBAL STATUS WHERE Variable_name IN
  ('Threads_connected',
   'Threads_running',
   'Innodb_buffer_pool_pages_dirty',
   'Innodb_row_lock_current_waits',
   'Innodb_data_pending_writes');

-- 4. Sessions doing actual work right now
SELECT id, user, db, command, time, state, LEFT(info, 80) AS info
FROM information_schema.processlist
WHERE command != 'Sleep' OR (command = 'Sleep' AND time > 300)
ORDER BY time DESC
LIMIT 20;
```

Safe-to-start signals:

- `Threads_running = 1` (the diagnostic query itself)
- No rows from `innodb_trx`
- `Innodb_buffer_pool_pages_dirty = 0`
- `Innodb_row_lock_current_waits = 0`
- No app sessions in non-`Sleep` state

## Recommended sequence

1. **Capture baseline.** Save before-sizes of target tables and total DB size.
2. **Smoke test on a small fragmented table** (~30–50 MB). Confirms locking/disk behaviour on this specific instance. Should finish in seconds.
3. **Verify with fresh stats** after the smoke test:
   ```sql
   SET SESSION information_schema_stats_expiry = 0;
   SELECT table_name,
          ROUND((data_length + index_length) / 1024 / 1024, 1) AS total_mb,
          ROUND(data_free / 1024 / 1024, 1)                   AS free_mb
   FROM information_schema.tables
   WHERE table_schema = '<db_name>' AND table_name = '<table_name>';
   ```
4. **Mid-size fragmented tables** (~200–600 MB). Each typically 1–3 minutes.
5. **Heavy tables last** (1 GB+). Each typically 5–15 minutes.
6. **Re-measure between each table** — never assume the rebuild reclaimed space; confirm.
7. **Skip tables where prod size ≈ restored size** — they are already well-packed and rebuilding them wastes IOPS for zero gain.

### Per-table template

```sql
SET SESSION lock_wait_timeout = 60;
OPTIMIZE TABLE <table_name>;

SET SESSION information_schema_stats_expiry = 0;
SELECT table_name,
       ROUND((data_length + index_length) / 1024 / 1024, 1) AS total_mb,
       ROUND(data_free / 1024 / 1024, 1)                   AS free_mb
FROM information_schema.tables
WHERE table_schema = '<db_name>' AND table_name = '<table_name>';
```

## Tables typically worth optimizing in HMIS

Based on the workload profile, these UPDATE-heavy tables are the usual reclaim candidates:

- `bill` — frequent status, total, and reference updates over the bill lifecycle
- `billitem` — line-level updates from billing edits and lifecycle transitions
- `pharmaceuticalbillitem` — pharmacy-specific bill items with stock-linked updates
- `stockhistory` — stock movement log; high update churn on remaining-quantity columns
- `patientreportitemvalue` — lab result value updates during verification
- `payment` — settlement and reconciliation updates

Tables that typically **do NOT** benefit (already insert-only or well-packed):

- `*archive` tables — written once, never updated
- Small reference / master tables (`item`, `department`, `institution`)
- Audit tables (append-only)
- Insert-only log tables (`drawerentry`, `logins`, etc.)

The `data_free` figure can be misleading for both groups. The dump-and-restore comparison is the only reliable predictor.

## Verification

After each optimize, confirm reclaim with fresh stats. If `total_mb` shows no change, the table was already packed — that is a valid outcome, not a failure.

After completing all optimizes, compare total DB size to baseline:

```sql
SET SESSION information_schema_stats_expiry = 0;
SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 1) AS total_db_mb
FROM information_schema.tables
WHERE table_schema = '<db_name>';
```

## Recurrence and maintenance cadence

Fragmentation re-accumulates as updates continue. For UPDATE-heavy tables (`bill`, `billitem`, `pharmaceuticalbillitem`), a **quarterly** OPTIMIZE during off-hours keeps things tight without ever letting the DB balloon to multiples of its logical size. Restoring this cadence is far cheaper and less risky than a one-off bulk recovery.

A sensible quarterly script targets the top 5–10 reclaim candidates by `(data_length + index_length)`, runs the per-table template on each, and logs before/after sizes.

## Common gotchas

- **"Table does not support optimize" is normal** for InnoDB — it just means MySQL is running the equivalent `ALTER TABLE … FORCE` instead.
- **`information_schema.tables` cache hides progress.** Always `SET SESSION information_schema_stats_expiry = 0;` for any size verification, or you will see pre-optimize numbers for up to 24 h.
- **`data_free` is an unreliable predictor.** Small `data_free` does **not** mean a table is well-packed if it has high update churn. Trust the dump-and-restore comparison.
- **`*archive` tables look big but rarely benefit.** They are insert-only and already dense.
- **Concurrent OPTIMIZEs on the same instance compete for IOPS** and may serialize on the binlog. Run one at a time per server.
- **Managed-MySQL backup/snapshot windows** can stretch optimize times dramatically. Confirm no managed snapshot is running before starting a large table.
- **InnoDB's minimum extent allocation** leaves a few MB reported as `data_free` even on a freshly rebuilt table. That is not reclaimable and does not indicate fragmentation.
- **Killing OPTIMIZE mid-rebuild rolls back cleanly** but you have spent the time. Worst case you may also be left with an orphan `#sql-*.ibd` tablespace file that needs cleanup.

## Related

- [MySQL Developer Guide](mysql-developer-guide.md) — credential management, debugging, common queries
- [MySQL Performance Configuration](mysql-performance-configuration.md) — buffer pool, log file, server-side tuning
- [Backup and Restore Operations](backup-restore-operations.md) — production dump/restore workflows
