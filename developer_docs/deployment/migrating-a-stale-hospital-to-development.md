# Migrating a long-stale hospital DB to `development` HEAD

When a hospital has been running a very old build for months/years and you bring
it up to `development` HEAD, `mf.xhtml` "Load Latest DDL from Wiki" is **not
enough**. It adds missing tables and columns but never fixes:

- columns whose **type** drifted from what the current entities expect,
- `AUTO_INCREMENT` that a mid-run DDL error stripped,
- illegal legacy defaults (`TIMESTAMP DEFAULT '0000-00-00'`),
- rows with `NULL`/blank enum-discriminator or key columns.

This guide captures the failure modes seen doing this for **Suwani** on
2026-09-03 (issue history in that session; see also
`memory/project_suwani_cicd_migration_2026_09_03.md`).

---

## 0. Before you start

- Full `mysqldump` of the hospital DB **and** its audit DB, downloaded off the
  VM. Keep the currently-deployed WAR as the rollback artifact.
- Disable the app (`asadmin disable <app>`) so nothing writes during the DDL.
- Know which branch/commit you are deploying and that it builds with JDK 11.

## 1. `eclipselink.deploy-on-startup` — re-add it for migrated deployments

**Symptom:** after deploy, inserts intermittently fail with

```
java.sql.SQLException: Field 'ID' doesn't have a default value   (Error Code 1364)
Call: INSERT INTO <TABLE> (...columns without ID...) VALUES (...)
   at org.eclipse.persistence...DatabaseAccessor.executeDirectNoSelect(...)
   at org.eclipse.persistence...StatementQueryMechanism.insertObject(...)
```

…even though the column **is** `AUTO_INCREMENT` and a hand-written
`INSERT` omitting `ID` works fine. It is intermittent — some inserts to the
same table in the same session succeed.

**Cause:** #20695 removed `eclipselink.deploy-on-startup=true` from
`persistence.xml` (it was added in #20138 to move PU init off the first request;
removed because it made CI `asadmin deploy` time out at 600 s against Azure).
Without it EclipseLink initialises each persistence unit lazily on first use,
and on a **freshly deployed WAR against a freshly migrated DB** that lazy init
can skip IDENTITY-sequencing setup for a `ClientSession`. Those sessions then
emit a plain `executeDirectNoSelect` INSERT that omits the generated PK.
A long-warm hospital (e.g. southernlanka, 37k audit rows) does not show this —
its sessions are all fully initialised through months of use.

**Fix:** put `deploy-on-startup` back **for the migrated prod branch**:

```xml
<property name="eclipselink.deploy-on-startup" value="true"/>
```

in **both** `hmisPU` and `hmisAuditPU`. The CI-timeout concern that motivated
#20695 does not apply on the new Azure estate — the migrated CI/CD workflow
already sets `AS_ADMIN_READTIMEOUT=1800000` (30 min), so the ~20 s extra deploy
time is free. After this, INSERTs correctly use
`ValueReadQuery … SELECT LAST_INSERT_ID()`.

## 2. Column-type drift — `mf.xhtml` will not fix it

`mf.xhtml` / `DataAdministrationController.createTablesAndFieldsForAllCreateStatements`
only runs `CREATE TABLE` and `ALTER TABLE … ADD COLUMN`. A column that already
exists with the wrong **type** is left as-is.

**How to find them:** parse the wiki `createDDL.sql`
(`https://raw.githubusercontent.com/wiki/hmislk/hmis/files/createDDL.sql`,
single-line `CREATE TABLE X (col TYPE, …)` per line) and compare each column's
type-category against `information_schema.columns` for the hospital DB. Flag
category changes that MySQL/EclipseLink cannot silently coerce on INSERT:

| category pair | why it breaks |
|---|---|
| `varchar` ↔ `int` | `@Enumerated` switched ORDINAL⇄STRING; writing `'Cash'` to an int column → `Incorrect integer value` |
| `int` → `bigint` on a `*_ID` FK | old schema too narrow for current IDs |
| `varchar` ↔ `double` / `decimal` | numeric entity field stored as text (or vice-versa) |
| `blob` ↔ `longtext` | `@Lob String` read from a `blob` column fails |
| `bit(1)` → `datetime` | a `Date` field that used to be a flag |

Do **not** treat `southernlanka-prod` (or any other running hospital) as the
reference — those branches carry older entity mappings. The authority is the
**entity code on the branch you are deploying** + the wiki DDL.

**Before ALTERing a column with data:** check the row count and whether the
existing values convert cleanly, e.g. for `varchar → double`:

```sql
SELECT SUM(col IS NOT NULL AND col <> '')                                   AS non_empty,
       SUM(col REGEXP '^-?[0-9]+(\\.[0-9]+)?$')                             AS clean_numeric
FROM   <table>;
```

`ALTER TABLE … MODIFY COLUMN` on a multi-million-row table rebuilds it (minutes);
plan for it. Wrap enum/FK type changes in `SET FOREIGN_KEY_CHECKS=0; … ; SET
FOREIGN_KEY_CHECKS=1;` and relax `STRICT_TRANS_TABLES` in the session if you
have benign legacy values.

## 3. `AUTO_INCREMENT` stripped mid-DDL

If the DDL-from-wiki run errors partway (`Duplicate column`, `Row size too
large`), a `MODIFY COLUMN ID …` inside its loop can leave `ID` **without**
`AUTO_INCREMENT`. Scan for it and restore:

```sql
SELECT TABLE_NAME
FROM   information_schema.COLUMNS
WHERE  TABLE_SCHEMA = DATABASE()
  AND  COLUMN_NAME = 'ID'
  AND  DATA_TYPE IN ('bigint','int')
  AND  EXTRA NOT LIKE '%auto_increment%';

-- restore (FK targets need checks off):
SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE <t> MODIFY COLUMN ID BIGINT NOT NULL AUTO_INCREMENT;
SET FOREIGN_KEY_CHECKS = 1;
```

`configoption.ID` losing this is especially nasty: `ConfigOptionApplicationController.@PostConstruct`
seeds default options, the INSERT fails, the whole `@ApplicationScoped` bean is
left broken, and unrelated pages NPE later (e.g. OPD billing's payment-method
dropdown → `EnumController.fillPaymentMethodsForOpdBilling`).

## 4. "Row size too large" adding a column to a wide table

Some legacy tables (`reportitem`, `userpreference`) have ~40+ `VARCHAR(255)`
`latin1` columns and already exceed InnoDB's 8126-byte inline row limit.
`ADD COLUMN` then fails `ERROR 1118`. Convert a batch of empty/short columns to
`TEXT` (off-page, JPA-transparent for `String`) to free the inline budget, then
add the column.

## 5. Illegal legacy `TIMESTAMP` defaults

`FROMTIME TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00'` sits fine but any
`ALTER TABLE` on that table fails `ERROR 1067 Invalid default value` under
`NO_ZERO_DATE`. Fix the default (`CURRENT_TIMESTAMP`, matching the sibling
column) before/with the other ALTERs.

## 6. Old encrypted usernames

Very old hospitals stored `webuser.NAME` jasypt-encrypted with
`BasicTextEncryptor` password `"health"` (`SecurityController.decrypt`). Current
code queries `WHERE u.name = :plaintextLowercase`. Decrypt every active
`webuser.NAME` and write it back as plaintext, or nobody can log in.

## 7. Privilege model drift

Old per-user `webuserprivilege` rows may not satisfy the current code's
`webUserController.hasPrivilege('Admin')` / `isAdmin()` checks even when the
`PRIVILEGE` string matches. Fastest unblock: mint a Finance `ApiKey` for a user
that `isAdmin()` accepts (its JPQL check is department-independent) and call
`POST /api/users/{id}/departments/{deptId}/privileges/all`, then re-login.

## 8. Order of operations

1. Backups + disable app.
2. `mf.xhtml` → "Load Latest DDL from Wiki and Update Both Databases", then
   "Fix AUTO_INCREMENT", then "Mark Migration as Complete".
3. Full `AUTO_INCREMENT` scan (§3) + restore.
4. Illegal-default fixes (§5).
5. Column-type drift scan (§2) + ALTERs. Re-scan after `AUTO_INCREMENT` fixes —
   an FK-target ALTER can strip it again.
6. Add `eclipselink.deploy-on-startup=true` to the migrated branch (§1),
   deploy.
7. Username de-obfuscation (§6), privilege bootstrap (§7).
8. **End-to-end test a real workflow** (an OPD bill *settle*, not just page
   render) — the payment/audit inserts are where §1 and §2 surface.
9. Data migrations (`/faces/admin/database_migration.xhtml`) last.
