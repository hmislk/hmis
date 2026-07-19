# Execute Pending Migrations from the No-Login Bootstrap Page

## Context

On the qa4 environment, every login attempt fails with a 500
(`javax.ejb.EJBException: Transaction aborted`, ultimately
`java.sql.SQLException: Field 'ID' doesn't have a default value`, MySQL 1364).
Root cause: several tables (at least `LOGINS`, `CONFIG_OPTION`) lost
`AUTO_INCREMENT` on their `ID` primary key, likely during a database restore.
Entities map `@GeneratedValue(strategy = IDENTITY)`, which requires the
underlying column to be `AUTO_INCREMENT` — without it, every `INSERT` into an
affected table fails.

This blocks the app in two places:

1. `SessionController.recordLogin()` (`SessionController.java:307-335`) inserts
   a `Logins` audit row during `selectDepartment()`/
   `loginActionWithoutDepartment()`. When that insert fails, the exception is
   not caught and the whole login/department-selection action fails — **no
   user, including an admin, can complete login** on an affected database.
2. `ConfigOptionApplicationController.init()` (`@PostConstruct`,
   `@ApplicationScoped`) creates missing `ConfigOption` rows on nearly every
   page load via `createOptionIfNotExists()`. On an affected database this
   insert also fails, and since it's uncaught, it breaks page rendering
   application-wide.

A migration to fix exactly this already exists and requires no new SQL logic
to design from scratch: `src/main/resources/db/migrations/v2.9.0/migration.sql`
dynamically scans `INFORMATION_SCHEMA` for every table whose single-column
`BIGINT` `ID` primary key lacks `AUTO_INCREMENT` and re-adds it, preserving
each column's exact type, relaxing `sql_mode` for the rebuild, and failing
loudly (`SIGNAL SQLSTATE '45000'`) if any table couldn't be fixed. It's
idempotent and a no-op on healthy databases. It can only be executed today
through `database_migration.xhtml`, which is gated by
`DatabaseMigrationController.isAuthorized()` — requiring a logged-in
SuperAdmin. On an affected database, login never completes, so this page can
never be reached.

Separately, `mf.xhtml` (backed by `midding_data_fields.xhtml` /
`DataAdministrationController`) already exists as a **no-login bootstrap
page** — it's `@PermitAll` (`DatabaseMigrationService`, a `@Singleton
@Startup` EJB, defaults `migrationPending = true` on every deploy/restart
until an admin explicitly marks it complete or not-necessary). It currently
offers DDL-based table/field creation, a "missing fields" checker, and a raw
"Run Custom SQL" box, gated by the same `runOnMainDatabase`/
`runOnAuditDatabase` checkboxes and already injecting both
`DatabaseMigrationFacade` and `AuditDatabaseFacade`.

**Key discovery:** both of those facades already contain an
`applyAutoIncrementToAllEntityTables()` method — a pure-Java equivalent of the
`v2.9.0` fix — but neither is called from anywhere in the codebase. They are
dead code, seemingly written for exactly this scenario and never wired up.
Their current implementation is rougher than `v2.9.0`: no `COLUMN_KEY='PRI'`
restriction, no case-insensitive column-name match, hardcodes `BIGINT` instead
of preserving each column's actual type, doesn't relax `sql_mode`, and
silently logs-and-continues instead of failing loudly when a table can't be
fixed.

## Decisions

1. **No new detection SQL.** Harden the existing (currently unused)
   `applyAutoIncrementToAllEntityTables()` methods on `DatabaseMigrationFacade`
   (main DB) and `AuditDatabaseFacade` (audit DB) to match `v2.9.0`'s
   correctness properties, then call them — rather than extracting a shared
   execution engine out of `DatabaseMigrationController` and running the SQL
   file through the versioned migration-tracking system. This is a much
   smaller change: no new service class, no changes to
   `DatabaseMigrationController` or its authorization model at all.

2. **Harden both `applyAutoIncrementToAllEntityTables()` methods** (identical
   fix applied to both `DatabaseMigrationFacade.java` and
   `AuditDatabaseFacade.java`, matching this codebase's existing convention of
   duplicating this exact kind of native-JDBC helper per facade — see
   `executeDdlNative()`, already duplicated verbatim between the two):
   - Restrict the `INFORMATION_SCHEMA.COLUMNS` scan to `COLUMN_KEY = 'PRI'`
     (primary keys only) and match `COLUMN_NAME` case-insensitively
     (`UPPER(COLUMN_NAME) = 'ID'`), matching `v2.9.0`.
   - Select `COLUMN_TYPE` per row and use it verbatim in the `ALTER TABLE ...
     MODIFY COLUMN` statement instead of hardcoding `BIGINT`, so a table with
     e.g. `bigint(20) unsigned` keeps its exact type.
   - Relax `sql_mode` to `''` for the duration of the rebuild (save the
     original value first, restore it in a `finally`), so a table with a
     legacy invalid default (e.g. `TIMESTAMP DEFAULT '0000-00-00 00:00:00'`)
     doesn't fail with error 1067 when `MODIFY COLUMN` re-validates every
     column.
   - After the fix loop, re-run the detection query. If any table is still
     missing `AUTO_INCREMENT`, throw an `SQLException` listing the remaining
     table names instead of returning silently — callers must be able to
     detect partial failure.

3. **New UI: 4th tab on the existing no-login page.** Add a "Fix Missing
   AUTO_INCREMENT" tab to `midding_data_fields.xhtml` (rendered on `mf.xhtml`,
   already reachable without login while `databaseMigrationPending` is true),
   with a single "Fix AUTO_INCREMENT" button gated by the existing
   `runOnMainDatabase`/`runOnAuditDatabase` checkboxes, mirroring the
   existing "Fix Missing Fields" tab's structure exactly.

4. **Backing controller: `DataAdministrationController`** (already backs
   `mf.xhtml`, already injects both `databaseMigrationFacade` and
   `auditDatabaseFacade`). Add one new method,
   `fixMissingAutoIncrement()`, that calls
   `databaseMigrationFacade.applyAutoIncrementToAllEntityTables()` when
   `runOnMainDatabase` is checked and
   `auditDatabaseFacade.applyAutoIncrementToAllEntityTables()` when
   `runOnAuditDatabase` is checked, reporting the altered-table list (or
   error) into the existing `mainDatabaseExecutionFeedback`/
   `auditDatabaseExecutionFeedback` fields already rendered by that tab
   family.

5. **No new authorization exposure.** `mf.xhtml` already self-locks (an admin
   clicks "Mark Migration as Complete"/"Mark as Not Necessary" once the
   database is healthy, setting `databaseMigrationPending = false` and
   restricting the page to admins again). It also already exposes a raw
   arbitrary-SQL executor (existing Tab 3, "Run Custom SQL Commands").
   Calling a fixed, narrowly-scoped Java method is strictly narrower than
   that existing capability, so this does not widen the pre-login attack
   surface.

6. **No changes to `recordLogin()`/`ConfigOptionApplicationController.init()`
   resiliency.** Out of scope per explicit decision: the mf.xhtml route is the
   sanctioned bootstrap-recovery path; login/config-option code stays as-is.

7. **`v2.9.0` stays untouched and unrun by this path.** Since this fix
   bypasses the `DatabaseMigration` tracking entity entirely, `v2.9.0` will
   still show as "pending" on `database_migration.xhtml` after this tab is
   used. That's an accepted, harmless gap: once login works again, an admin
   who later executes `v2.9.0` through the normal page gets a fast no-op (the
   scan finds nothing left to fix) and the tracked history is closed out
   properly then.

## Non-goals

- Does not fix `SessionController.recordLogin()`'s uncaught exception path or
  `ConfigOptionApplicationController.init()`'s lack of per-option error
  isolation. Those remain unchanged.
- Does not touch `database_migration.xhtml`, `DatabaseMigrationController`, or
  the `DatabaseMigration` tracking entity/table.
- Does not add a `MigrationExecutionService` or otherwise refactor
  `DatabaseMigrationController`.

## Testing

No unit-test harness exists for this subsystem — both facades talk to a live
JDBC connection pulled from EclipseLink's JNDI datasource, which needs a real
Payara + MySQL, matching why no existing tests touch
`DatabaseMigrationFacade`/`AuditDatabaseFacade` today. Verify manually
instead, per this project's own migration-development-guide.md "Local
Testing" convention (backup, exercise through the admin UI, restore if
needed):

1. On the local dev database, strip `AUTO_INCREMENT` from a disposable test
   table's `BIGINT` `ID` primary key (e.g. `ALTER TABLE some_table MODIFY id
   BIGINT NOT NULL`) and confirm an `INSERT` into it now fails with MySQL
   1364, reproducing the qa4 symptom.
2. Confirm `mf.xhtml` is reachable without logging in (fresh session, no
   prior auth), and the new tab is visible with its Fix button.
3. Click "Fix AUTO_INCREMENT" with `runOnMainDatabase` checked; confirm the
   feedback panel lists the affected table and the column is fixed (query
   `INFORMATION_SCHEMA.COLUMNS` directly to confirm `EXTRA` now contains
   `auto_increment`).
4. Re-click the button with the database already healthy; confirm it reports
   no tables altered (idempotency).
5. Repeat steps 1 and 3 against the audit database/`runOnAuditDatabase`
   checkbox.
6. Confirm a normal login now succeeds (the login-blocking symptom is gone)
   once the affected tables — `LOGINS`, `CONFIG_OPTION` in the original qa4
   case — are fixed this way.
