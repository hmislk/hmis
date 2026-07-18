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

A migration to fix exactly this already exists and requires no new SQL:
`src/main/resources/db/migrations/v2.9.0/migration.sql` dynamically scans
`INFORMATION_SCHEMA` for every table whose single-column `BIGINT` `ID` primary
key lacks `AUTO_INCREMENT` and re-adds it. It's idempotent and a no-op on
healthy databases.

The problem is reachability, not tooling: `v2.9.0` can only be executed today
through `database_migration.xhtml`, which is gated by
`DatabaseMigrationController.isAuthorized()` — requiring a logged-in SuperAdmin.
On an affected database, login never completes, so this page can never be
reached.

Separately, `mf.xhtml` (backed by `midding_data_fields.xhtml`) already exists
as a **no-login bootstrap page** — it's `@PermitAll`
(`DatabaseMigrationService`, a `@Singleton @Startup` EJB, defaults
`migrationPending = true` on every deploy/restart until an admin explicitly
marks it complete or not-necessary). It currently offers DDL-based table/field
creation, a "missing fields" checker, and a raw "Run Custom SQL" box — but has
no awareness of the versioned migration-tracking system
(`DatabaseMigration` entity / `DatabaseMigrationFacade` /
`MigrationDiscoveryService`) that `database_migration.xhtml` uses.

## Decisions

1. **No new SQL or detection logic.** Reuse `v2.9.0/migration.sql` as-is via
   the existing migration-tracking system. This design is purely about
   reachability.

2. **Extract the execution engine into a shared, stateless service.**
   `DatabaseMigrationController` currently owns
   `executeSingleMigration()`, `executeSqlScript()`, `splitSqlStatements()`,
   `isDdlStatement()`, `usesMysqlConnectionScopedState()`,
   `getIdempotentSkipReason()`, etc. as private methods on a `@SessionScoped`
   bean that reads `sessionController.getLoggedUser()` directly. Move these
   into a new `@Stateless` EJB, `com.divudi.service.MigrationExecutionService`,
   parameterized with a nullable `WebUser executedBy` instead of reading the
   session directly. `DatabaseMigrationController` keeps its
   `isAuthorized()` gate and delegates to the shared service, passing the
   logged-in user. This avoids duplicating the SQL-execution engine (needed
   intact, including the DELIMITER/stored-procedure handling `v2.9.0`
   requires) across two code paths.

3. **New UI: 4th tab on the existing no-login page.** Add a "Pending Schema/
   Data Migrations" tab to `midding_data_fields.xhtml` (rendered on `mf.xhtml`,
   already reachable without login while `databaseMigrationPending` is true).
   It lists migrations from `MigrationDiscoveryService`/
   `DatabaseMigrationFacade` that are not yet `SUCCESS`, each with an
   "Execute" button, plus an "Execute All Pending" button — mirroring
   `database_migration.xhtml`'s existing UX (list, per-item execute, execute-
   all, live log/status per migration).

4. **Backing controller: `DataAdministrationController`** (already backs
   `mf.xhtml`). Add `pendingMigrations` (list), `executeMigration(MigrationInfo)`,
   and `executeAllPendingMigrations()`, delegating to
   `MigrationExecutionService` with `executedBy = null` (the
   `DatabaseMigration.executed_by_id` column is already a nullable FK, per
   `MIGRATION_SYSTEM_DEPLOYMENT.md`'s `CREATE TABLE database_migration`
   definition).

5. **No new authorization exposure.** `mf.xhtml` already self-locks (an admin
   clicks "Mark Migration as Complete"/"Mark as Not Necessary" once the
   database is healthy, setting `databaseMigrationPending = false` and
   restricting the page to admins again). It also already exposes a raw
   arbitrary-SQL executor (existing Tab 3, "Run Custom SQL Commands").
   Executing only vetted, repo-shipped migration files by version number is
   strictly narrower than that existing capability, so this does not widen
   the pre-login attack surface.

6. **No changes to `recordLogin()`/`ConfigOptionApplicationController.init()`
   resiliency.** Out of scope per explicit decision: the mf.xhtml route is the
   sanctioned bootstrap-recovery path; login/config-option code stays as-is.

## Non-goals

- Does not fix `SessionController.recordLogin()`'s uncaught exception path or
  `ConfigOptionApplicationController.init()`'s lack of per-option error
  isolation. Those remain unchanged.
- Does not add a new migration script — `v2.9.0` is reused verbatim.
- Does not change `database_migration.xhtml` or `DatabaseMigrationController`'s
  authorization model.

## Testing

Local verification: temporarily strip `AUTO_INCREMENT` from a test table's
`ID` column (e.g. via `ALTER TABLE ... MODIFY id BIGINT NOT NULL`), confirm:

1. Login fails with the same 1364 error, reproducing the qa4 symptom.
2. `mf.xhtml` is reachable without logging in, and the new tab shows `v2.9.0`
   (or the then-current AUTO_INCREMENT-fix migration) as pending.
3. Clicking "Execute" runs it successfully and the migration is recorded with
   `executed_by = null`.
4. After execution, login succeeds and `ConfigOption` auto-creation no longer
   throws.
