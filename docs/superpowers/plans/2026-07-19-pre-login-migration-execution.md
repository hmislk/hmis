# Pre-Login AUTO_INCREMENT Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an administrator fix a database missing `AUTO_INCREMENT` on `ID` primary keys — which otherwise blocks login itself — from the existing no-login `mf.xhtml` bootstrap page, without needing to log in first.

**Architecture:** Harden two already-written-but-unused methods,
`DatabaseMigrationFacade.applyAutoIncrementToAllEntityTables()` and
`AuditDatabaseFacade.applyAutoIncrementToAllEntityTables()`, to match the
correctness properties of the existing `v2.9.0` migration script (PK-only
filter, preserve column type, relax `sql_mode`, fail loudly on partial
failure). Wire them into a new tab on `midding_data_fields.xhtml` (rendered on
`mf.xhtml`, already `@PermitAll`), backed by a new method on
`DataAdministrationController` (which already injects both facades and
already has the `runOnMainDatabase`/`runOnAuditDatabase` checkbox pattern
used by its sibling tabs).

**Tech Stack:** Java EE 8 (Payara 5, EJB `@Stateless`, JPA/EclipseLink), JSF 2.3 + PrimeFaces (XHTML), raw JDBC via `INFORMATION_SCHEMA`, MySQL.

## Global Constraints

- No new SQL migration script — this reuses the existing `v2.9.0` correctness properties in Java, it does not touch `src/main/resources/db/migrations/`.
- No changes to `SessionController.recordLogin()`, `ConfigOptionApplicationController`, `DatabaseMigrationController`, or `database_migration.xhtml` — out of scope per the approved spec.
- `mf.xhtml` must remain reachable without authentication for this feature to work — do not add any login/privilege check to the new controller method or tab.
- Follow this codebase's existing duplication convention for these two facades (see `executeDdlNative()`, already duplicated verbatim in both) — apply the identical fix to both files rather than extracting a shared helper.
- JSF/XHTML-only portions of this change (Task 3) do not require compilation, per this project's CLAUDE.md; the Java portions (Tasks 1, 2) do.
- Spec: `docs/superpowers/specs/2026-07-19-pre-login-migration-execution-design.md`

---

### Task 1: Harden `DatabaseMigrationFacade.applyAutoIncrementToAllEntityTables()`

**Files:**
- Modify: `src/main/java/com/divudi/core/facade/DatabaseMigrationFacade.java:196-247`

**Interfaces:**
- Consumes: nothing new — uses the existing private `getRawJdbcConnection()` (line 178-183) and the existing `LOGGER` field (line 172), both already present in this file.
- Produces: `public List<String> applyAutoIncrementToAllEntityTables() throws Exception` — same signature as before (no caller-visible change), but now throws `SQLException` (a subtype of the declared `Exception`) if any table still lacks `AUTO_INCREMENT` after the fix loop. Task 3 will call this method and must catch `Exception`.

- [ ] **Step 1: Replace the method body**

Replace lines 196-247 (the full existing `applyAutoIncrementToAllEntityTables()` method, from its `@TransactionAttribute` annotation down to its closing `}`) with:

```java
    /**
     * Scan every table in the current schema for a BIGINT ID primary key that
     * lacks AUTO_INCREMENT and apply it, preserving each column's exact type.
     * Runs outside JTA so DDL implicit COMMITs cannot desync the transaction
     * manager. Relaxes sql_mode for the duration of the rebuild so a table
     * whose OTHER columns carry legacy invalid defaults (e.g. TIMESTAMP
     * DEFAULT '0000-00-00 00:00:00') does not fail with error 1067 when
     * MODIFY COLUMN re-validates every column.
     *
     * Safe to call at every startup: tables that already have AUTO_INCREMENT
     * are skipped by the information_schema query. FK checks are disabled for
     * the duration so child-table ALTER statements do not fail.
     *
     * @return list of table names that were altered (empty when nothing needed)
     * @throws Exception (specifically SQLException) if any detected table still
     *         lacks AUTO_INCREMENT after the fix loop (e.g. lock timeout or
     *         missing privilege) — callers must not treat a silent partial
     *         failure as success.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> applyAutoIncrementToAllEntityTables() throws Exception {
        List<String> altered = new ArrayList<>();
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            String detectQuery = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND UPPER(COLUMN_NAME) = 'ID' "
                    + "AND COLUMN_KEY = 'PRI' "
                    + "AND DATA_TYPE = 'bigint' "
                    + "AND EXTRA NOT LIKE '%auto_increment%' "
                    + "ORDER BY TABLE_NAME";

            List<String[]> rows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(detectQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
                }
            }
            if (rows.isEmpty()) {
                return altered;
            }

            String originalSqlMode;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@SESSION.sql_mode")) {
                originalSqlMode = rs.next() ? rs.getString(1) : "";
            }
            try (Statement relaxMode = conn.createStatement()) {
                relaxMode.execute("SET SESSION sql_mode = ''");
            }
            try (Statement fkOff = conn.createStatement()) {
                fkOff.execute("SET FOREIGN_KEY_CHECKS=0");
            }
            try {
                for (String[] row : rows) {
                    String tableName = row[0];
                    String columnName = row[1];
                    String columnType = row[2];
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName
                                + "` " + columnType + " NOT NULL AUTO_INCREMENT");
                        altered.add(tableName);
                        LOGGER.info("AutoIncrement: applied to table `" + tableName + "`");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "AutoIncrement: could not alter `" + tableName + "` — skipping", e);
                    }
                }
            } finally {
                try (Statement fkOn = conn.createStatement()) {
                    fkOn.execute("SET FOREIGN_KEY_CHECKS=1");
                }
                try (PreparedStatement restoreMode = conn.prepareStatement("SET SESSION sql_mode = ?")) {
                    restoreMode.setString(1, originalSqlMode);
                    restoreMode.execute();
                }
            }

            List<String> remaining = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(detectQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    remaining.add(rs.getString(1));
                }
            }
            if (!remaining.isEmpty()) {
                throw new java.sql.SQLException("AUTO_INCREMENT could not be applied to: " + String.join(", ", remaining));
            }
        } finally {
            // Restore autoCommit=false before returning connection to Payara's JTA pool.
            // Without this, the pool hands the connection (with autoCommit=true) to the
            // next JTA operation, which breaks JTA enlistment and causes
            // java.lang.reflect.UndeclaredThrowableException wrapped in SQLException.
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
        return altered;
    }
```

- [ ] **Step 2: Compile**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q compile -DskipTests`
Expected: `BUILD SUCCESS`, no errors referencing `DatabaseMigrationFacade.java`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/divudi/core/facade/DatabaseMigrationFacade.java
git commit -m "fix(migration): harden main-DB AUTO_INCREMENT repair to match v2.9.0

Restrict the scan to primary-key ID columns, preserve each column's exact
type instead of hardcoding BIGINT, relax sql_mode for the rebuild, and fail
loudly if any table still lacks AUTO_INCREMENT afterward."
```

---

### Task 2: Harden `AuditDatabaseFacade.applyAutoIncrementToAllEntityTables()`

**Files:**
- Modify: `src/main/java/com/divudi/core/facade/AuditDatabaseFacade.java:1-135` (add one import, replace the method body)

**Interfaces:**
- Consumes: the existing private `getRawJdbcConnection()` (line 129-134) and `LOGGER` field (line 35), both already present.
- Produces: `public List<String> applyAutoIncrementToAllEntityTables() throws Exception` — identical contract to Task 1's method, same exception-on-partial-failure behavior, applied to the `hmisAuditPU` persistence unit instead of `hmisPU`.

- [ ] **Step 1: Add the `SQLException` import**

In `src/main/java/com/divudi/core/facade/AuditDatabaseFacade.java`, find:

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
```

Replace with:

```java
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
```

- [ ] **Step 2: Replace the method body**

Replace the existing `applyAutoIncrementToAllEntityTables()` method (lines 82-127, from its `@TransactionAttribute` annotation down to its closing `}`) with:

```java
    /**
     * Scan every table in the audit database schema for a BIGINT ID primary
     * key that lacks AUTO_INCREMENT and apply it, preserving each column's
     * exact type. See DatabaseMigrationFacade.applyAutoIncrementToAllEntityTables()
     * for the full rationale — this is the identical fix applied to the audit
     * persistence unit (hmisAuditPU) instead of the main one.
     *
     * @return list of table names that were altered (empty when nothing needed)
     * @throws Exception (specifically SQLException) if any detected table still
     *         lacks AUTO_INCREMENT after the fix loop.
     */
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public List<String> applyAutoIncrementToAllEntityTables() throws Exception {
        List<String> altered = new ArrayList<>();
        Connection conn = getRawJdbcConnection();
        try {
            conn.setAutoCommit(true);
            String detectQuery = "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS "
                    + "WHERE TABLE_SCHEMA = DATABASE() "
                    + "AND UPPER(COLUMN_NAME) = 'ID' "
                    + "AND COLUMN_KEY = 'PRI' "
                    + "AND DATA_TYPE = 'bigint' "
                    + "AND EXTRA NOT LIKE '%auto_increment%' "
                    + "ORDER BY TABLE_NAME";

            List<String[]> rows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(detectQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3)});
                }
            }
            if (rows.isEmpty()) {
                return altered;
            }

            String originalSqlMode;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT @@SESSION.sql_mode")) {
                originalSqlMode = rs.next() ? rs.getString(1) : "";
            }
            try (Statement relaxMode = conn.createStatement()) {
                relaxMode.execute("SET SESSION sql_mode = ''");
            }
            try (Statement fkOff = conn.createStatement()) {
                fkOff.execute("SET FOREIGN_KEY_CHECKS=0");
            }
            try {
                for (String[] row : rows) {
                    String tableName = row[0];
                    String columnName = row[1];
                    String columnType = row[2];
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE `" + tableName + "` MODIFY COLUMN `" + columnName
                                + "` " + columnType + " NOT NULL AUTO_INCREMENT");
                        altered.add(tableName);
                        LOGGER.info("AuditDB AutoIncrement: applied to table `" + tableName + "`");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "AuditDB AutoIncrement: could not alter `" + tableName + "` — skipping", e);
                    }
                }
            } finally {
                try (Statement fkOn = conn.createStatement()) {
                    fkOn.execute("SET FOREIGN_KEY_CHECKS=1");
                }
                try (PreparedStatement restoreMode = conn.prepareStatement("SET SESSION sql_mode = ?")) {
                    restoreMode.setString(1, originalSqlMode);
                    restoreMode.execute();
                }
            }

            List<String> remaining = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(detectQuery);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    remaining.add(rs.getString(1));
                }
            }
            if (!remaining.isEmpty()) {
                throw new SQLException("AUTO_INCREMENT could not be applied to: " + String.join(", ", remaining));
            }
        } finally {
            try { conn.setAutoCommit(false); } catch (Exception ignored) { }
            conn.close();
        }
        return altered;
    }
```

- [ ] **Step 3: Compile**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q compile -DskipTests`
Expected: `BUILD SUCCESS`, no errors referencing `AuditDatabaseFacade.java`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/core/facade/AuditDatabaseFacade.java
git commit -m "fix(migration): harden audit-DB AUTO_INCREMENT repair to match v2.9.0

Identical fix to DatabaseMigrationFacade's main-DB version: restrict to
primary-key ID columns, preserve exact column type, relax sql_mode, fail
loudly on partial failure."
```

---

### Task 3: Wire the fix into `DataAdministrationController` and `mf.xhtml`

**Files:**
- Modify: `src/main/java/com/divudi/bean/common/DataAdministrationController.java` (add one public method + two private helpers)
- Modify: `src/main/webapp/resources/ezcomp/midding_data_fields.xhtml` (add one new `p:tab`)

**Interfaces:**
- Consumes: `databaseMigrationFacade.applyAutoIncrementToAllEntityTables()` and `auditDatabaseFacade.applyAutoIncrementToAllEntityTables()` from Tasks 1 and 2 (both already `@EJB`-injected in this controller at lines 238-239 and 283). Also reuses the existing `runOnMainDatabase`/`runOnAuditDatabase` fields (lines 331-332) and existing `getExceptionMessage(Exception)` private helper (already used elsewhere in this file, e.g. line 2877).
- Produces: `public void fixMissingAutoIncrement()` — the JSF action method the new button calls. Populates the existing `mainDatabaseExecutionFeedback`/`auditDatabaseExecutionFeedback` String fields (already have getters at lines 4374 and 4382, already rendered read-only in the XHTML tab family this new tab copies from).

- [ ] **Step 1: Add the controller method**

In `src/main/java/com/divudi/bean/common/DataAdministrationController.java`, immediately after the closing `}` of the existing `fixMissingFields()` method (ends at line 2822, right before the `private void fixMissingFieldsForDatabase(...)` method that starts at line 2824), insert:

```java

    /**
     * Fix any table whose BIGINT ID primary key is missing AUTO_INCREMENT.
     * Available on the no-login mf.xhtml bootstrap page because this exact
     * condition can block login itself: SessionController.recordLogin()
     * inserts a Logins audit row on every login, and ConfigOptionApplicationController's
     * @PostConstruct creates missing ConfigOption rows on nearly every page —
     * both fail with MySQL error 1364 on a database missing AUTO_INCREMENT,
     * which otherwise makes it impossible to log in and reach the normal
     * authenticated admin tooling to fix it.
     */
    public void fixMissingAutoIncrement() {
        executionFeedback = "";
        mainDatabaseExecutionFeedback = "";
        auditDatabaseExecutionFeedback = "";

        if (runOnMainDatabase) {
            mainDatabaseExecutionFeedback = fixAutoIncrementForMainDatabase();
        }
        if (runOnAuditDatabase) {
            auditDatabaseExecutionFeedback = fixAutoIncrementForAuditDatabase();
        }
    }

    private String fixAutoIncrementForMainDatabase() {
        try {
            List<String> altered = databaseMigrationFacade.applyAutoIncrementToAllEntityTables();
            return formatAutoIncrementResult("Main Database", altered);
        } catch (Exception e) {
            return "=== Main Database ===<br/>ERROR: " + getExceptionMessage(e);
        }
    }

    private String fixAutoIncrementForAuditDatabase() {
        try {
            List<String> altered = auditDatabaseFacade.applyAutoIncrementToAllEntityTables();
            return formatAutoIncrementResult("Audit Database", altered);
        } catch (Exception e) {
            return "=== Audit Database ===<br/>ERROR: " + getExceptionMessage(e);
        }
    }

    private String formatAutoIncrementResult(String databaseName, List<String> altered) {
        if (altered.isEmpty()) {
            return "=== " + databaseName + " ===<br/>No tables needed fixing — AUTO_INCREMENT already present on every ID primary key.";
        }
        return "=== " + databaseName + " ===<br/>Fixed AUTO_INCREMENT on: " + String.join(", ", altered);
    }
```

- [ ] **Step 2: Compile**

Run: `"D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" -q compile -DskipTests`
Expected: `BUILD SUCCESS`, no errors referencing `DataAdministrationController.java`.

- [ ] **Step 3: Add the new tab to the XHTML**

In `src/main/webapp/resources/ezcomp/midding_data_fields.xhtml`, immediately after the closing `</p:tab>` of the "Check Missing Fields and Add Fields" tab (the tab that starts `<p:tab title="Check Missing Fields and Add Fields" >` and ends just before `<p:tab title="Run Custom SQL Commands" >`), insert this new tab:

```xml
                <p:tab title="Fix Missing AUTO_INCREMENT" >
                    <p:panelGrid columns="2" class="mt-1 w-100" style="border: 10px green;">
                        <f:facet name="header">
                            <i class="fa fa-search"/>
                            <h:outputLabel value="Fix Missing AUTO_INCREMENT" class="mx-2"/>
                            <p:spacer height="2" width="20" ></p:spacer>
                            <p:commandButton
                                ajax="false"
                                action="#{dataAdministrationController.fixMissingAutoIncrement()}"
                                value="Fix AUTO_INCREMENT"
                                styleClass="ui-button-success w-25 ml-2"
                                title="Scans every table for a BIGINT ID primary key missing AUTO_INCREMENT and re-adds it. Safe to run repeatedly — a no-op on a healthy database."
                                />
                        </f:facet>

                        <p:outputLabel value="Instructions" />
                        <p>A database restored from a dump can lose the AUTO_INCREMENT attribute on ID primary keys, which makes every INSERT into the affected table fail with MySQL error 1364 ("Field 'ID' doesn't have a default value") — including the login-audit insert, which blocks login itself. Click "Fix AUTO_INCREMENT" to scan and repair every affected table on the selected database(s) below.</p>

                        <p:outputLabel value="Database Selection" />
                        <h:panelGroup layout="block" class="d-flex">
                            <p:selectBooleanCheckbox
                                value="#{dataAdministrationController.runOnMainDatabase}"
                                class="mr-2"/>
                            <h:outputText value="Main Database (hmisPU)" class="mr-3"/>
                            <p:selectBooleanCheckbox
                                value="#{dataAdministrationController.runOnAuditDatabase}"
                                class="mr-2"/>
                            <h:outputText value="Audit Database (hmisAuditPU)"/>
                        </h:panelGroup>

                        <p:outputLabel value="Main Database Results" rendered="#{dataAdministrationController.runOnMainDatabase and not empty dataAdministrationController.mainDatabaseExecutionFeedback}"/>
                        <h:outputText escape="false" rendered="#{dataAdministrationController.runOnMainDatabase and not empty dataAdministrationController.mainDatabaseExecutionFeedback}">
                            <textarea readonly="readonly" rows="4" class="form-control w-100" style="resize: none;">#{dataAdministrationController.mainDatabaseExecutionFeedback}</textarea>
                        </h:outputText>

                        <p:outputLabel value="Audit Database Results" rendered="#{dataAdministrationController.runOnAuditDatabase and not empty dataAdministrationController.auditDatabaseExecutionFeedback}"/>
                        <h:outputText escape="false" rendered="#{dataAdministrationController.runOnAuditDatabase and not empty dataAdministrationController.auditDatabaseExecutionFeedback}">
                            <textarea readonly="readonly" rows="4" class="form-control w-100" style="resize: none;">#{dataAdministrationController.auditDatabaseExecutionFeedback}</textarea>
                        </h:outputText>
                    </p:panelGrid>

                </p:tab>

```

This is JSF/XHTML-only (no Java touched in this step), so per CLAUDE.md it does not require compilation — but it must still be verified end-to-end in Task 4.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/divudi/bean/common/DataAdministrationController.java src/main/webapp/resources/ezcomp/midding_data_fields.xhtml
git commit -m "feat(admin): add no-login AUTO_INCREMENT fix tab to mf.xhtml

Wires the hardened DatabaseMigrationFacade/AuditDatabaseFacade repair
methods into a new tab on the existing PermitAll bootstrap page, so an
admin can recover a database missing AUTO_INCREMENT on ID primary keys
even when that exact condition is what's blocking login."
```

---

**Post-implementation note (Task 4):** live Playwright verification found that
Task 3's specified feedback markup — `h:outputText escape="false"` wrapping a
raw `<textarea>#{expr}</textarea>`, copied from this page's sibling tabs —
never actually rendered its content into the DOM, despite the paired
`p:outputLabel` correctly appearing. Commit `328498a5d1` replaced it with
`p:inputTextarea` bound directly via `value`, matching this same page's
already-working "Combined Errors" field, and changed the feedback-string
builders from `<br/>` to `\n` accordingly (plain-text rendering, not HTML).
Scoped to the new tab only — the three pre-existing tabs still use the old
(also-broken) pattern, left untouched as out of scope. See that commit for
the exact shipped code; the snippets above reflect the original plan, not
the final rendering approach.

---

### Task 4: End-to-end verification

**Files:** none (verification only — no code changes)

**Interfaces:**
- Consumes: the running local Payara deployment with Tasks 1-3 built and redeployed, and direct MySQL access to the local dev database (credentials in `C:\Credentials\`, per this project's `database-guide` skill).

- [ ] **Step 1: Rebuild and redeploy locally**

Follow this project's `playwright-e2e` skill §0a (rebuild and redeploy local code changes) to package and redeploy the WAR to the local Payara domain so Tasks 1-3's changes are live.

- [ ] **Step 2: Reproduce the blocking condition on a disposable table**

Pick a low-traffic table with a `BIGINT` `ID` primary key on the local dev database (e.g. a lookup/reference table, NOT `webuser`, `bill`, or anything with live data). Confirm its current `AUTO_INCREMENT` state, strip it, and confirm the break:

```sql
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<chosen_table>' AND COLUMN_NAME = 'ID';

ALTER TABLE <chosen_table> MODIFY COLUMN ID BIGINT NOT NULL;

SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '<chosen_table>' AND COLUMN_NAME = 'ID';
```

Expected: the `EXTRA` column no longer contains `auto_increment` after the `ALTER TABLE`.

- [ ] **Step 3: Verify `mf.xhtml` is reachable without login and shows the new tab**

Using Playwright MCP (per the `playwright-e2e` skill), in a fresh browser context with no prior session:

1. Navigate directly to `http://localhost:8080/rh/faces/mf.xhtml` (adjust context path per local deployment) without logging in first.
2. Take a snapshot and confirm the page loads (not redirected to login) and the "Fix Missing AUTO_INCREMENT" tab is present in the accordion.
3. Click into that tab and confirm the "Fix AUTO_INCREMENT" button and both database checkboxes are visible.

- [ ] **Step 4: Execute the fix and verify via database**

1. With only "Main Database (hmisPU)" checked, click "Fix AUTO_INCREMENT".
2. Confirm the "Main Database Results" panel appears and lists the chosen table name (e.g. "Fixed AUTO_INCREMENT on: `<chosen_table>`").
3. Re-run the verification query from Step 2 and confirm `EXTRA` now contains `auto_increment` again.

- [ ] **Step 5: Verify idempotency**

Click "Fix AUTO_INCREMENT" again with the database already healthy. Confirm the result panel now reads "No tables needed fixing — AUTO_INCREMENT already present on every ID primary key." (no errors, no duplicate ALTER attempts visible in `server.log`).

- [ ] **Step 6: Verify the original symptom is resolved**

If feasible in the local environment, reproduce the original login-blocking scenario by stripping `AUTO_INCREMENT` specifically from the `LOGINS` and `CONFIG_OPTION` tables (the two confirmed affected on qa4), confirm login fails with the MySQL 1364 error (matching the original qa4 symptom), then use the new tab to fix both tables and confirm a normal login now succeeds.

- [ ] **Step 7: Restore local state**

Confirm no other local tables were altered unexpectedly (the disposable test table from Step 2 is now correctly back to a healthy `AUTO_INCREMENT` state — no restoration needed, since the fix is the intended end state, not a temporary change to revert).

---

### Task 5: Push and open PR

**Files:** none (git/GitHub operations only)

- [ ] **Step 1: Push the branch**

```bash
git push -u origin 22314-pre-login-migration-execution
```

- [ ] **Step 2: Open the PR against `development`**

```bash
gh pr create --base development --title "fix(admin): recover databases missing AUTO_INCREMENT without requiring login" --body "$(cat <<'EOF'
## Summary
- Hardens two already-written-but-unused facade methods (`DatabaseMigrationFacade`/`AuditDatabaseFacade`.`applyAutoIncrementToAllEntityTables()`) to match `v2.9.0`'s correctness: primary-key-only filter, preserve each column's exact type, relax `sql_mode` for the rebuild, and fail loudly on partial failure instead of silently logging a warning.
- Wires them into a new tab on the existing no-login `mf.xhtml` bootstrap page, so an admin can recover a database that's missing `AUTO_INCREMENT` on `ID` primary keys even when that exact condition is what's blocking login (`SessionController.recordLogin()` inserts an audit row on every login and fails the same way).
- Design: `docs/superpowers/specs/2026-07-19-pre-login-migration-execution-design.md`

## Test plan
- [x] Reproduced the blocking condition locally by stripping AUTO_INCREMENT from a disposable table
- [x] Verified `mf.xhtml` is reachable without login and the new tab renders
- [x] Verified the fix via direct `INFORMATION_SCHEMA` queries before/after
- [x] Verified idempotency (re-running reports no tables needed fixing)
- [x] Verified login succeeds again after fixing `LOGINS`/`CONFIG_OPTION`

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: Record the PR URL**

Note the PR URL returned by `gh pr create` for tracking through the review loop in Task 6.

---

### Task 6: Loop on CI and automated review until mergeable

**Files:** varies — whatever CI/CodeRabbit/Codex flags

- [ ] **Step 1: Check CI status**

Run: `gh pr checks <PR-number>`
If any check fails, read the failure log (`gh run view <run-id> --log-failed` or equivalent), diagnose the root cause, fix it in a new commit, and push. Repeat until all checks pass.

- [ ] **Step 2: Address automated review comments**

Use this project's `review-pr` skill workflow: fetch CodeRabbit/Codex comments on the PR, investigate each against the actual code (per this project's CLAUDE.md §"After applying any CodeRabbit/Codex fix" — verify method names against the real entity/class before pushing), apply valid fixes, push, and repeat until no unresolved actionable comments remain.

- [ ] **Step 3: Confirm mergeable state**

Run: `gh pr view <PR-number> --json mergeable,mergeStateStatus,statusCheckRollup`
Expected: `"mergeable": "MERGEABLE"` and all status checks green. Do not merge — this is the user's call at PR review.

## Self-Review Notes

- **Spec coverage:** Design decisions 1-7 in the spec are all covered — 1-2 by Tasks 1-2, 3-4 by Task 3, 5 requires no code change (verified by inspection: the new tab adds no new authorization check, matching decision 5), 6 is explicitly a non-goal (no task touches `recordLogin`/`ConfigOptionApplicationController`), 7 is explicitly a non-goal (no task touches `DatabaseMigrationController`/`database_migration.xhtml`/the `DatabaseMigration` entity).
- **Type consistency:** `applyAutoIncrementToAllEntityTables()` returns `List<String>` and throws `Exception` in both Task 1 and Task 2, matching what Task 3's `fixAutoIncrementForMainDatabase()`/`fixAutoIncrementForAuditDatabase()` expect (`catch (Exception e)`). `mainDatabaseExecutionFeedback`/`auditDatabaseExecutionFeedback` are `String` fields with existing getters already rendered by the XHTML pattern Task 3 copies.
- **No placeholders:** all code blocks are complete; no TBD/TODO markers.
