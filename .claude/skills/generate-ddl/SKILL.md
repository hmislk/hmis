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
swap in `persistence.xml` for local testing) so it can be restored
byte-for-byte, not just reset to git HEAD.

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
`@PostConstruct`, which is what runs the column-enhancement step). First
look up the **actual currently-deployed app name** — do not assume `rh`; on
some machines the app is deployed as `rh-3.0.0` (derived from the WAR
filename) instead. `list-applications` lists *every* deployed app (not just
this one), so don't just grab the first line — filter for the `rh`/`rh-<version>`
naming convention and require exactly one unambiguous match, failing loudly
rather than silently defaulting to `rh` if that's not the case:

```bash
WAR=$(ls target/*.war | head -1)
MATCHES=$(/home/buddhika/payara/bin/asadmin list-applications | awk '{print $1}' | grep -E '^rh(-[0-9][0-9.]*)?$' || true)
MATCH_COUNT=$(printf '%s\n' "$MATCHES" | grep -c . || true)
if [ "$MATCH_COUNT" -ne 1 ]; then
  echo "ERROR: expected exactly one deployed app matching rh/rh-<version>, found $MATCH_COUNT: $MATCHES" >&2
  exit 1
fi
DEPLOYED_NAME="$MATCHES"
/home/buddhika/payara/bin/asadmin redeploy --name "$DEPLOYED_NAME" "$WAR"
```

Always pass the looked-up `--name`, matching whatever app name this
particular machine actually has deployed — `dev-issue` and `playwright-e2e`
default to `rh`, but that's only a convention, not a guarantee. Omitting
`--name` lets `asadmin` derive the app name from the WAR filename instead of
redeploying the existing app, which can leave two separate apps competing
for the same hardcoded `/rh` context root (`glassfish-web.xml`). If the
error above fires (zero or multiple matches), stop and ask the user rather
than guessing which app to redeploy.

**If deploy fails with a JNDI lookup error for a datasource** (e.g.
`jdbc/ruhunuAudit` not found): this is a pre-existing local-environment
mismatch unrelated to DDL generation — do NOT silently "fix" it as part of
this skill. Run `/home/buddhika/payara/bin/asadmin list-jdbc-resources` to
see what's actually registered, report the mismatch to the user, and ask
before changing `<jta-data-source>` (that line may already be a deliberate
uncommitted local override).

**If the `redeploy` command itself fails** (for the JNDI reason above or any
other), re-run `list-applications` before retrying anything — a failed
`redeploy` can leave the app fully undeployed rather than rolled back to the
previous working version:

```bash
/home/buddhika/payara/bin/asadmin list-applications
```

If `$DEPLOYED_NAME` is no longer listed, do **not** retry `redeploy` (it will
fail again with "Application ... is not deployed"). Fall back to a plain
`deploy` instead, setting `--contextroot` explicitly since a fresh `deploy`
doesn't infer it the way `redeploy` does:

```bash
/home/buddhika/payara/bin/asadmin deploy --name "$DEPLOYED_NAME" --contextroot rh "$WAR"
```

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

Re-check the deployed app name rather than assuming it's still the same —
use the same `$DEPLOYED_NAME` lookup as step 4:

```bash
mvn -q package -DskipTests
WAR=$(ls target/*.war | head -1)
MATCHES=$(/home/buddhika/payara/bin/asadmin list-applications | awk '{print $1}' | grep -E '^rh(-[0-9][0-9.]*)?$' || true)
MATCH_COUNT=$(printf '%s\n' "$MATCHES" | grep -c . || true)
if [ "$MATCH_COUNT" -ne 1 ]; then
  echo "ERROR: expected exactly one deployed app matching rh/rh-<version>, found $MATCH_COUNT: $MATCHES" >&2
  exit 1
fi
DEPLOYED_NAME="$MATCHES"
/home/buddhika/payara/bin/asadmin redeploy --name "$DEPLOYED_NAME" "$WAR"
```

If this redeploy fails for the same pre-existing JNDI reason noted in step
4, say so explicitly — don't leave the user thinking the app is back to a
known-good state when it isn't. The previous (DDL-generation-enabled)
deployment will keep running in that case until the underlying datasource
issue is fixed.

If the failed `redeploy` left the app undeployed (check with
`asadmin list-applications` — `$DEPLOYED_NAME` will be absent), fall back to:

```bash
/home/buddhika/payara/bin/asadmin deploy --name "$DEPLOYED_NAME" --contextroot rh "$WAR"
```

Report this fallback explicitly too — the app is restored, but via `deploy`
rather than `redeploy`, which is worth flagging since it means the earlier
`redeploy` genuinely failed rather than just being slow.

### 8. Publish the DDL to the wiki

Write the updated wiki page. The wiki lives in the sibling `../hmis.wiki`
repo. If that directory does not exist, print a warning and skip steps 8–9
(the DDL file is still useful locally).

**The SQL must NOT be inlined into the wiki page.** The script is ~650 KB,
which exceeds GitHub's page-rendering limit (~512 KiB) — GitHub silently
truncates the *rendered* page mid-statement, so anyone copying the SQL from
the page would apply an incomplete script. Instead, commit the DDL as a
plain file (`files/createDDL.sql`) in the wiki repo and have the page link
to its raw URL, which always serves the complete file.

```bash
WIKI_DIR="$(git rev-parse --show-toplevel)/../hmis.wiki"
WIKI_FILE="$WIKI_DIR/Database-Schema-DDL-Generation-Guide.md"
UPDATE_TS="$(date '+%Y.%m.%d %H.%M')"
AUTHOR="$(git config user.name | awk '{print $NF}')"   # last name only

if [ ! -d "$WIKI_DIR" ]; then
  echo "WARNING: wiki repo not found at $WIKI_DIR — skipping wiki publish"
else
  mkdir -p "$WIKI_DIR/files"
  cp "$DDL_DIR/createDDL.jdbc" "$WIKI_DIR/files/createDDL.sql"

  cat > "$WIKI_FILE" << WIKI_HEADER
This page explains how to generate and apply the full database schema for the
application, including all missing tables and fields. This is especially
useful when setting up a fresh instance of the application or restoring a
database structure.

## Steps to Generate the DDL File

1. Locate the \`persistence.xml\` file in your project you use for development.
2. Replace its contents with the configuration from \`persistence_for_database_generation_script.xml\`.
3. Adjust the values in that file, especially the location where the DDL file should be generated on your computer.
4. Run the application once. This will generate the full database schema as a DDL script in the specified file location.
5. Open the generated DDL file and copy its contents.
6. In the application where you want to update the database, go to **Menu > Administration > Manage Metadata > Add Missing Fields**, paste the copied DDL content into the provided text area, and click the **Update Database** button.
7. The latest version of the DDL file is available for download below so that you need not generate it yourself.

## Last Update - $UPDATE_TS - ($AUTHOR)

## Download the Full DDL File

**[Download createDDL.sql](https://raw.githubusercontent.com/wiki/hmislk/hmis/files/createDDL.sql)**

> **Why a download link instead of inline SQL?** The full script is ~650 KB —
> larger than GitHub's page-rendering limit — so when it was pasted into this
> page, GitHub silently truncated the displayed SQL mid-statement. Anyone
> copying from the rendered page would have applied an incomplete script.
> Always use the raw file linked above; never copy the SQL from a rendered
> wiki page.

To apply it, go to **Menu > Administration > Manage Metadata > Add Missing
Fields** and click **Load Latest DDL from Wiki** — the server downloads the
file above directly, then click **Update Database**. This is the recommended
path: pasting the full ~650 KB script into the text area can exceed the
server's maximum POST size and fail with a "Post too large" error.

On older application versions that do not have the **Load Latest DDL from
Wiki** button, download the file and paste its contents into the text area
as described in step 6 above — and if you get a "Post too large" error,
upgrade the application or apply the script in smaller parts.
WIKI_HEADER
  echo "Wiki page written: $WIKI_FILE"
  echo "DDL file written:  $WIKI_DIR/files/createDDL.sql"
fi
```

### 9. Commit and push the wiki

```bash
if [ -d "$WIKI_DIR" ]; then
  cd "$WIKI_DIR"
  # Pull in any concurrent wiki edits; if this file conflicts, keep ours
  git stash --include-untracked
  git pull --rebase origin master || true
  git stash pop || true
  # Only touch --theirs if the pop actually left an unmerged (conflicted)
  # entry for this file. Outside a real conflict `git checkout --theirs`
  # on a fully-merged path checks it out from HEAD/index, silently
  # discarding the working-tree content stash pop just restored — which
  # then makes the "diff --cached --quiet" check below wrongly conclude
  # there's nothing to commit.
  if git ls-files -u -- Database-Schema-DDL-Generation-Guide.md files/createDDL.sql | grep -q .; then
    git checkout --theirs Database-Schema-DDL-Generation-Guide.md files/createDDL.sql 2>/dev/null || true
  fi
  git add Database-Schema-DDL-Generation-Guide.md files/createDDL.sql
  git rebase --continue 2>/dev/null || true
  # Commit (skip if nothing staged, e.g. rebase already applied it)
  git diff --cached --quiet || git commit -m "docs(wiki): update DDL generation guide with $(date '+%Y-%m-%d') schema"
  git push origin master
  cd -
fi
```

If the push is rejected again (another concurrent push), re-run the
`pull --rebase` + `push` cycle once more by hand — two concurrent DDL
regenerations are rare enough that a single retry is sufficient.

## Notes

- Never commit `persistence.xml` or `DdlFileEnhancerService.java` with the
  DDL-generation edits in place — they are local-machine-specific
  (hardcoded absolute paths) and would break CI/CD and other developers'
  checkouts. Double-check `git diff` on both files before every commit
  during this workflow; `commit-code` also flags a staged `persistence.xml`
  for review.
- The two persistence units writing to the same file/directory rely on
  EclipseLink appending rather than truncating — this matches the
  previously-documented working DDL (which includes both regular and audit
  tables, e.g. `AUDITEVENT`).
