# Dual-Version ID Allocator Separation (Temporary)

**Status:** TEMPORARY measure — remove once every production deployment that shares a
database with a newer build has been upgraded to `GenerationType.IDENTITY`.

**Added for:** Ruhunu, June 2026. The Ruhunu production build and the development
build run side by side against the same main and audit databases.

## Symptom

```
Internal Exception: java.sql.SQLIntegrityConstraintViolationException:
Duplicate entry '18251' for key 'REPORTLOG.PRIMARY'
Error Code: 1062
Call: INSERT INTO REPORTLOG (ID, CREATEDAT, ...) VALUES (?, ?, ...)
```

The INSERT lists the `ID` column explicitly — that identifies the **old build**
(`GenerationType.AUTO`) as the failing side. `REPORTLOG` lives in the **audit
database** (`ReportLogFacade` uses `hmisAuditPU` on both branches), so both
databases are affected, not just the main one.

## Root cause: two independent ID allocators on one table

| Build | Strategy | Allocator |
|---|---|---|
| Old production (e.g. `ruhunu-prod`, 189 entities) | `GenerationType.AUTO` | EclipseLink table sequencing: one global `SEQ_GEN` row in the `SEQUENCE` table, preallocated in blocks of 50, ID sent explicitly on INSERT |
| Development build (198 entities) | `GenerationType.IDENTITY` | MySQL `AUTO_INCREMENT` on each table |

Neither allocator sees the other's reservations. InnoDB's auto_increment counter
always chases the highest inserted ID, which makes the collision *immediate*, not
occasional:

1. Old build preallocates sequence block `[18251..18300]`, inserts `18251`.
2. The table's auto_increment counter jumps to `18252`.
3. New build inserts → takes `18252`.
4. Old build inserts its next preallocated value → `18252` → **duplicate key**.

## Why a plain sequence bump cannot fix it

Bumping `SEQ_COUNT` to any value N just moves the same race to N: as soon as the
old build inserts at the new range, the auto_increment counter follows it, and the
new build's next insert lands exactly on the old build's next preallocated value.
The gap size is irrelevant.

## The fix: disjoint ranges

`IdAllocatorSeparation.separate(Connection)` (package
`com.divudi.core.facade`) runs per schema:

1. **Inventory** every table with a `BIGINT` primary key named `ID` (case detected
   via `INFORMATION_SCHEMA`, per the cross-deployment case-sensitivity rule) and
   find the **global max ID** across all of them.
2. **Raise every `SEQUENCE` row** to `globalMax + 10,000`
   (`SEQUENCE_SAFETY_MARGIN`). The old build stops re-issuing IDs the new build
   has already used. The margin absorbs new-build inserts that happen between the
   scan and step 3.
3. **Push every table's `AUTO_INCREMENT` counter** to `globalMax + 1,000,000,000`
   (`AUTO_INCREMENT_OFFSET`). The new build now allocates in a band the old
   build's sequence would need a billion allocations to reach.

The bands stay disjoint because InnoDB only moves an auto_increment counter
*upward*, and only when an insert is **at or above** it — the old build's explicit
low-range IDs never touch the high counters.

```
IDs:  [1 .. globalMax]   (globalMax+10k ...→]            [globalMax+1e9 ...→
       existing rows      old build (SEQUENCE, explicit)   new build (AUTO_INCREMENT)
```

Both builds may still share the `SEQUENCE` table itself (the dev build's one
remaining `AUTO` entity, `ShiftStaffRequirement`, also draws from `SEQ_GEN`) —
that is safe: table sequencing uses row-locked `UPDATE`s, so concurrent processes
get disjoint blocks.

## Where the code lives

| Piece | Location |
|---|---|
| Core logic (shared) | `src/main/java/com/divudi/core/facade/IdAllocatorSeparation.java` |
| Main DB entry point | `DatabaseMigrationFacade.separateIdAllocatorsForDualVersionOperation()` |
| Audit DB entry point | `AuditDatabaseFacade.separateIdAllocatorsForDualVersionOperation()` |
| Controller action | `DataAdministrationController.separateIdAllocatorsForDualVersionOperation()` |
| UI | `admin_functions.xhtml` → tab **"ID Generation — Dual Version Operation"** |

The facade methods run on a raw JDBC connection outside JTA
(`TransactionAttributeType.NOT_SUPPORTED`, `autoCommit=true`) because
`ALTER TABLE` triggers MySQL implicit commits that desync the JTA transaction
manager — same pattern as `executeDdlNative` / `applyAutoIncrementToAllEntityTables`.

## Runbook

1. Deploy the development build containing this function.
2. Log in as a user with the **Admin** privilege.
3. Admin → Data Administration → **Admin Functions** → tab
   **"ID Generation — Dual Version Operation"** → **Separate ID Allocators**
   (confirm dialog). The output column reports, per database: tables scanned,
   global max ID, old sequence values, the new sequence target, and how many
   tables had their counters moved.
4. **Immediately restart the old-version Payara.** It still holds a preallocated
   sequence block in memory and keeps colliding until it re-reads the bumped
   `SEQUENCE` row. The new build needs no restart.

### Verification (read-only, either DB)

```sql
SELECT SEQ_NAME, SEQ_COUNT FROM SEQUENCE;            -- ≈ global max + 10,000
SELECT MAX(ID) FROM REPORTLOG;                        -- old-build rows: low range
SELECT AUTO_INCREMENT FROM INFORMATION_SCHEMA.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'REPORTLOG';  -- ≈ global max + 1e9
```

After the restart, old-build inserts continue in the low range and new-build rows
appear above one billion.

## Side effects & safety

- New-build rows get IDs above ~1,000,000,000. Harmless: every ID is `BIGINT`,
  and IDs are surrogate keys (bill numbers etc. are generated separately).
- Re-running is safe (idempotent in effect): max IDs only grow, `SEQUENCE` rows
  below the target are raised, `ALTER ... AUTO_INCREMENT` to a value below a
  counter that is already higher is ignored by InnoDB.
- Empty tables are included deliberately — the new build must allocate high
  everywhere, including tables it has not written yet.
- No row data is modified; only the `SEQUENCE` counters and table metadata.

## Permanent fix / removal

Upgrade the old production branch to `GenerationType.IDENTITY` for all entities
(as already done for Coop), with migration `v2.9.0` restoring `AUTO_INCREMENT`
on any ID primary key missing it. IDENTITY then simply continues from wherever
the counters stand — the billion-offset rows need no cleanup. After that
upgrade, remove the admin tab, the controller method, the two facade methods,
and `IdAllocatorSeparation`.
