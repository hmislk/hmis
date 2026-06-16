---
name: generate-ddl
description: >
  Generate a full-schema DDL script (CREATE TABLE + ALTER TABLE ADD COLUMN)
  into the project's tmp/ folder by temporarily enabling EclipseLink DDL
  generation, rebuilding, and redeploying the local app. Reverts all source
  changes and restores the original deployment afterward. Use when asked to
  generate/regenerate the DDL for the Database-Schema-DDL-Generation-Guide
  wiki page, or to produce a schema-sync script for a fresh/behind database.
disable-model-invocation: true
allowed-tools: Bash, Read, Edit
---

# Generate Full-Schema DDL Script

Produces a self-contained SQL script (new tables + missing columns on
existing tables) at `<project-root>/tmp/createDDL.jdbc`, then leaves the
working tree and the deployed app exactly as they were before this skill ran.

This is a **temporary, local-only** procedure — none of the intermediate
edits are ever committed.

## How it works

Two files need a temporary edit, and both must point at the **same absolute
path**, computed fresh every run (it differs per developer machine):

```bash
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
DDL_DIR="$PROJECT_ROOT/tmp"
```

1. **`src/main/resources/META-INF/persistence.xml`** — EclipseLink only
   emits `CREATE TABLE` + FK `ALTER TABLE ADD CONSTRAINT` statements in
   `sql-script` mode; it does not diff against a live DB.
2. **`src/main/java/com/divudi/service/DdlFileEnhancerService.java`** — a
   `@Startup @Singleton` EJB that runs once at app start, reads the
   generated `createDDL.jdbc`, and appends an `ALTER TABLE ... ADD COLUMN
   ...` for every column of every `CREATE TABLE` it finds (closing the gap
   above). Its output directory is a **hardcoded compile-time constant**
   (`APPLICATION_LOCATION`), so it must be edited and rebuilt — there is no
   runtime config for it today.

## Steps

### 1. Back up the two files before touching them

Use a project tmp subfolder for backups (never the system `/tmp`):

```bash
mkdir -p "$DDL_DIR/.generate-ddl-backup"
cp src/main/resources/META-INF/persistence.xml "$DDL_DIR/.generate-ddl-backup/persistence.xml.bak"
cp src/main/java/com/divudi/service/DdlFileEnhancerService.java "$DDL_DIR/.generate-ddl-backup/DdlFileEnhancerService.java.bak"
```

This captures whatever local state was already there (e.g. a local JNDI
swap in `persistence.xml` per [verify-persistence](../verify-persistence/SKILL.md)) so it can be restored byte-for-byte, not just reset to git HEAD.

### 2. Add DDL-generation properties to persistence.xml

Inside the `<properties>` block of **both** `hmisPU` and `hmisAuditPU`, add:

```xml
<property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
<property name="eclipselink.ddl-generation.output-mode" value="sql-script"/>
<property name="eclipselink.application-location" value="$DDL_DIR"/>
```

(Substitute the actual computed `$DDL_DIR` value — XML doesn't expand shell
variables.) Use the `Edit` tool with the existing closing
`</properties>`/last `<property>` line as anchor, same as the existing
`eclipselink.jdbc.result-set-access-optimization` property block.

### 3. Point DdlFileEnhancerService at the same directory

Edit the constant:

```java
private static final String APPLICATION_LOCATION = "$DDL_DIR";
```

(Again, substitute the real computed path — this is a Java string literal,
not a shell expansion.)

### 4. Rebuild and redeploy

```bash
mvn -q package -DskipTests
```

Find the built WAR and force-deploy it (this also triggers the singleton's
`@PostConstruct`, which is what runs the column-enhancement step):

```bash
WAR=$(ls target/*.war | head -1)
/home/buddhika/payara/bin/asadmin deploy --force=true "$WAR"
```

**If deploy fails with a JNDI lookup error for a datasource** (e.g.
`jdbc/ruhunuAudit` not found): this is a pre-existing local-environment
mismatch unrelated to DDL generation — do NOT silently "fix" it as part of
this skill. Run `/home/buddhika/payara/bin/asadmin list-jdbc-resources` to
see what's actually registered, report the mismatch to the user, and ask
before changing `<jta-data-source>` (that line may already be a deliberate
uncommitted local override).

### 5. Verify the output

```bash
test -f "$DDL_DIR/createDDL.jdbc" && echo FOUND
grep -c "^CREATE TABLE" "$DDL_DIR/createDDL.jdbc"
grep -c "ADD COLUMN" "$DDL_DIR/createDDL.jdbc"
```

Both counts should be in the hundreds/thousands, not zero. If `ADD COLUMN`
count is 0, the enhancer didn't run — check the deployed app actually
restarted (a `--force=true` redeploy always re-triggers `@PostConstruct`;
if it didn't, the JNDI/deploy step above likely failed).

### 6. Restore both files exactly

```bash
cp "$DDL_DIR/.generate-ddl-backup/persistence.xml.bak" src/main/resources/META-INF/persistence.xml
cp "$DDL_DIR/.generate-ddl-backup/DdlFileEnhancerService.java.bak" src/main/java/com/divudi/service/DdlFileEnhancerService.java
rm -rf "$DDL_DIR/.generate-ddl-backup"
```

Confirm with `git diff` that the only remaining diff (if any) is whatever
pre-existing local-only change was already there before step 1 — never more.

### 7. Rebuild and redeploy again to restore the running app

```bash
mvn -q package -DskipTests
/home/buddhika/payara/bin/asadmin deploy --force=true "$(ls target/*.war | head -1)"
```

If this redeploy fails for the same pre-existing JNDI reason noted in step
4, say so explicitly — don't leave the user thinking the app is back to a
known-good state when it isn't. The previous (DDL-generation-enabled)
deployment will keep running in that case until the underlying datasource
issue is fixed.

## After this skill finishes

`$DDL_DIR/createDDL.jdbc` contains the full script. To publish it, update
the **Database-Schema-DDL-Generation-Guide** wiki page in the sibling
`../hmis.wiki` repo: replace the `## Full DDL File Contents` code block with
the new content and update the `## Last Update - YYYY.MM.DD HH.MM -
(Name)` line above it. Use [publish-wiki](../publish-wiki/SKILL.md) to
commit and push.

## Notes

- Never commit `persistence.xml` or `DdlFileEnhancerService.java` with the
  DDL-generation edits in place — they are local-machine-specific
  (hardcoded absolute paths) and would break CI/CD and other developers'
  checkouts. [verify-persistence](../verify-persistence/SKILL.md) already
  checks for stray `eclipselink.application-location` hardcoded paths
  before push.
- The two persistence units writing to the same file/directory rely on
  EclipseLink appending rather than truncating — this matches the
  previously-documented working DDL (which includes both regular and audit
  tables, e.g. `AUDITEVENT`).
