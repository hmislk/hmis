# Playwright E2E — Local Payara, asadmin, DB drift and seed data

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [27. Multi-Payara machines: `asadmin` without `--port` may hit ANOTHER USER'S domain](#27-multi-payara-machines-asadmin-without---port-may-hit-another-users-domain)
- [34. Local dev DB can silently drift behind the entity model — watch for `Unknown column` on unrelated pages](#34-local-dev-db-can-silently-drift-behind-the-entity-model--watch-for-unknown-column-on-unrelated-pages)
- [38. A local dev DB missing columns for an already-shipped entity field surfaces as a hung page, and `ALTER TABLE` alone doesn't fix it — the pool needs a flush](#38-a-local-dev-db-missing-columns-for-an-already-shipped-entity-field-surfaces-as-a-hung-page-and-alter-table-alone-doesnt-fix-it--the-pool-needs-a-flush)
- [39. Local dev DB has no `FrequencyUnit`/`DurationUnit`/`DoseUnit` seed rows — the prescription "Calculate & Add" path is untestable locally](#39-local-dev-db-has-no-frequencyunitdurationunitdoseunit-seed-rows--the-prescription-calculate--add-path-is-untestable-locally)
- [41. A local dev DB with an empty `TRIGGERSUBSCRIPTION` table means notification-generating actions silently produce zero `UserNotification` rows](#41-a-local-dev-db-with-an-empty-triggersubscription-table-means-notification-generating-actions-silently-produce-zero-usernotification-rows)
- [98. Local Payara can come up with a dead MySQL connection pool after any host sleep/restart — every page hangs, not just one touching a stale entity](#98-local-payara-can-come-up-with-a-dead-mysql-connection-pool-after-any-host-sleeprestart--every-page-hangs-not-just-one-touching-a-stale-entity)
- [63. `STAFF.ID` is not `PERSON.ID` — joining `billfee.staff_id` straight to `PERSON.ID` silently returns the wrong person's name](#63-staffid-is-not-personid--joining-billfeestaff_id-straight-to-personid-silently-returns-the-wrong-persons-name)
- [73. `asadmin deploy --contextroot /` from Git Bash gets mangled by MSYS path conversion — set `MSYS_NO_PATHCONV=1`](#73-asadmin-deploy---contextroot--from-git-bash-gets-mangled-by-msys-path-conversion--set-msys_no_pathconv1)
- [74. Exploded Payara deployment lets you hot-swap a single XHTML file for a fast test/fix loop — real redeploy still required before commit](#74-exploded-payara-deployment-lets-you-hot-swap-a-single-xhtml-file-for-a-fast-testfix-loop--real-redeploy-still-required-before-commit)
- [87. The local `coop` DB's `WEBUSER` rows carry stale password hashes from whatever environment they were synced from — production login credentials will not work locally, and even a direct SQL password reset needs a domain restart to take effect](#87-the-local-coop-dbs-webuser-rows-carry-stale-password-hashes-from-whatever-environment-they-were-synced-from--production-login-credentials-will-not-work-locally-and-even-a-direct-sql-password-reset-needs-a-domain-restart-to-take-effect)
- [102. A local "Unknown column" 500 is schema drift — use the app's own migration page](#102-a-local-unknown-column-500-is-schema-drift--use-the-apps-own-migration-page)
- [104. A department/room created by direct SQL needs more than the FK columns](#104-a-departmentroom-created-by-direct-sql-needs-more-than-the-fk-columns)
- [111. The local `coop` DB can have **zero** vacant rooms — free some by SQL before testing any admission flow](#111-the-local-coop-db-can-have-zero-vacant-rooms--free-some-by-sql-before-testing-any-admission-flow)
- [118. Verifying an `@Asynchronous` dispatch: read the thread name in `server.log`, not the wall clock](#118-verifying-an-asynchronous-dispatch-read-the-thread-name-in-serverlog-not-the-wall-clock)
- [119. The local dev box has no email or SMS gateway — verify the queued row, not the delivery](#119-the-local-dev-box-has-no-email-or-sms-gateway--verify-the-queued-row-not-the-delivery)
- [141. Patient phone quick search matches `PATIENTPHONENUMBER` or `PATIENTMOBILENUMBER`, not `PERSON.PHONE`](#141-patient-phone-quick-search-matches-patientphonenumber-or-patientmobilenumber-not-personphone)

---

## 27. Multi-Payara machines: `asadmin` without `--port` may hit ANOTHER USER'S domain

On a box with two Payara installs (e.g. `/home/carecode/payara` domain `rh` admin port **9048**,
and `/home/buddhika/payara` domain1 on default **4848**), a bare `asadmin redeploy/undeploy/deploy`
connects to whoever owns 4848 — which can be the *other user's* server. Tells that you're on the
wrong DAS: `redeploy` fails with **"Cannot determine the path of application"**, `deploy` fails with
**"File not found"** for a WAR that clearly exists (the other user's Payara process can't traverse
your 750-mode home directory), and `list-applications` shows an app list that doesn't match your
domain. An `undeploy` in this state removes the app from the *other* server — before any
state-changing asadmin call, confirm which process owns the admin port you're about to use
(`ss -tlnp | grep <port>` + `ps -o user= -p <pid>`), run `asadmin --port <port> list-applications`
on that same endpoint to confirm the expected app list, and always pass the explicit admin port on
**every** command (`asadmin --port 9048 redeploy/undeploy/deploy ...` for the local `rh` domain —
never the bare default).

Recovery after removing the wrong domain's app: copy the WAR to a path the *target* domain's user
can read (its Payara can't traverse your `0750` home directory) — keep permissions as tight as that
allows (e.g. a dedicated directory rather than bare `/tmp`, no wider than `0644` on the file),
rewrite `WEB-INF/classes/META-INF/persistence.xml` inside the copy to that domain's JNDI names via
`unzip`/`sed`/`zip`, **verify the rewritten `<jta-data-source>` values before deploying**, deploy
with `--port <that domain's admin port>` and `--name`/`--contextroot` matching what was removed,
and **delete the staged copy immediately after** the deploy succeeds. (Hit while deploying for
issue #14863.)


## 34. Local dev DB can silently drift behind the entity model — watch for `Unknown column` on unrelated pages

While testing #22196, clicking "View Request" on an inward pharmacy request
threw `SQLSyntaxErrorException: Unknown column 'VATPERCENTAGE' in 'field
list'` loading `BillItem` rows — the local `coop` DB's `BILLITEM` table
predates the `vatPercentage` field added to the `BillItem` entity, and there
is no DDL/migration step in the local dev workflow that keeps schema in sync
automatically. This is unrelated to whatever feature is under test and will
recur for any page that touches `BillItem`. Fix locally with a plain additive
column matching the sibling `VAT`/`VATPLUSNETVALUE` columns:
`ALTER TABLE BILLITEM ADD COLUMN VATPERCENTAGE DOUBLE NULL DEFAULT NULL AFTER
VAT;` — do not add this to a migration script (it's a local-only environment
gap, not a schema change accompanying a code change). If a fresh
`Unknown column` error appears on an otherwise-unrelated page, check
`SHOW COLUMNS FROM <table>` against the entity's fields before assuming the
feature under test is broken.


## 38. A local dev DB missing columns for an already-shipped entity field surfaces as a hung page, and `ALTER TABLE` alone doesn't fix it — the pool needs a flush

Entity fields that were added to the codebase a while ago (e.g.
`PatientEncounter.professionalPaymentsOnHold` / `...HoldDateTime` /
`...HoldBy` / `...HoldNotes`) can be **missing from a local dev database**
that was never migrated, even though nothing about the current change
touches those fields. Symptoms are confusing because EclipseLink issues a
`SELECT *`-style query for the whole entity on any page that touches it, so
the failure isn't localized to the field you'd expect:

- Direct-navigating to a page via URL (bypassing the app's normal
  click-through flow) can appear to **hang indefinitely** in Playwright
  (`browserBackend.callTool` timeouts on `navigate`/`snapshot`/even
  `tabs list`) rather than showing an error — the request never actually
  hangs server-side, but an error response mid-navigation can leave the
  MCP browser bridge stuck. If a normal in-app link/button navigation to
  the same destination works cleanly and shows the real `SQLSyntaxErrorException:
  Unknown column '...' in 'field list'` page, that confirms it's this
  gotcha, not a broken browser.
- The fix is a plain `ALTER TABLE ... ADD COLUMN ...` matching the
  entity's field type (check the `@Column`/type in the entity class), but
  **the running Payara connection pool caches connections/statement
  metadata from before the ALTER** — re-hitting the page immediately after
  the ALTER still throws the identical "Unknown column" error. Flush the
  pool before retrying:
  `asadmin flush-connection-pool <poolName>` (find the pool name via
  `grep -B2 'jndi-name="jdbc/coop"' domain.xml` → look for the
  `<jdbc-resource pool-name="...">` line, e.g. `poolCoopLocal` for
  `jdbc/coop`).
- Combining this with §20's privilege-row gotcha: if panels are still
  missing after the schema+pool fix, check privileges next — they're
  independent causes of the same "content silently doesn't render" symptom.

Verified while testing the Inward Dashboard "Manage Allergies" /
"Hold Professional Payments" button relocation (issue #22248), where the
local `coop.patientencounter` table was missing all four
`professionalpayments*` columns and `patienttransferrequest` was missing
`theatreroom_id`.


## 39. Local dev DB has no `FrequencyUnit`/`DurationUnit`/`DoseUnit` seed rows — the prescription "Calculate & Add" path is untestable locally

`ward_pharmacy_bht_issue_request_bill.xhtml`'s Prescription section (Dose/Dose
Unit/Frequency/Duration/Duration Unit → "Calculate & Add") requires selecting
a `FrequencyUnit` and `DurationUnit` — both are `Category` subclasses stored
in the single-table `category` (via `@Inheritance` with no strategy = default
`SINGLE_TABLE`, discriminated by `DTYPE`). The local `coop` DB has **zero**
rows with `DTYPE` in (`FrequencyUnit`, `DurationUnit`, `DoseUnit`) — confirmed
via `SELECT DISTINCT DTYPE FROM category`. Both dropdowns render as
`combobox "Select"` with no other options, and submitting anyway fails with
`"Calculation Error: Incomplete prescription: dose, frequency, duration and
duration unit are required"`. **Workaround**: use the "Dispense Request" →
"+ Add Dispense Only" path instead (item autocomplete + plain qty field, no
prescription fields) — but that path has the toDepartment bug from §31, so
still fix `TODEPARTMENT_ID` via SQL afterward. Verified while testing issue
#22312.


## 41. A local dev DB with an empty `TRIGGERSUBSCRIPTION` table means notification-generating actions silently produce zero `UserNotification` rows

Discharging a patient, changing a room, etc. always creates a `Notification`
row, but the actual per-user `UserNotification` rows (what the bell icon and
`/Notification/user_notifications.xhtml` show) only get created for webusers
who hold a matching `TriggerSubscription`
(`NotificationController.createNotification(...)` →
`userNotificationController.createUserNotifications(nn)` →
`TriggerSubscriptionController.fillSubscribedUsersByDepartment(...)`). A
freshly-restored or never-fully-seeded local DB can have **zero rows in
`TRIGGERSUBSCRIPTION`**, in which case discharging any number of patients
produces `Notification` rows but no `UserNotification` rows for anyone —
this looks identical to "the feature doesn't work" but is actually missing
test-fixture data, not a bug.

- Diagnose with `SELECT COUNT(*) FROM TRIGGERSUBSCRIPTION;` — 0 confirms this.
- Fix through the UI, not SQL (per this doc's "use the admin UI" pattern,
  §26): Admin → Manage Users → select the target user → **Manage User
  Subscriptions** → tick **Application-wide** → pick the relevant
  `TriggerType` (e.g. "Inward Patient Room Discharge - System Notification")
  → **Add Subscription**.
- The **Application-wide** checkbox's visible box intercepts Playwright's
  normal click on the underlying `p:selectBooleanCheckbox` input — click via
  a selector scoped to its own JSF id (`chkApplicationWide` in
  `admin/users/user_subscription.xhtml`), not a bare `.ui-chkbox-box` index,
  which picks whichever checkbox happens to be first/nth on the page and can
  silently toggle the wrong control if the page has more than one:
  `document.querySelector('[id$="chkApplicationWide"] .ui-chkbox-box')`.


## 98. Local Payara can come up with a dead MySQL connection pool after any host sleep/restart — every page hangs, not just one touching a stale entity

Unlike §38 (a pool holding connections from *before* an `ALTER TABLE`), this is
the pool holding connections to a MySQL instance that was itself restarted or
the host machine slept/resumed. Symptoms are more severe than §38's
single-page hang: **the app root itself** (`GET /rh`, even the pre-login page)
times out in both a direct `Invoke-WebRequest`/`curl` and
`browser_navigate`/`browser_snapshot` (30-60s timeouts with no response) —
because `ConfigOptionApplicationController.init()` runs on first
request/session and hits the DB immediately. `server.log` shows
`CJCommunicationsException: Communications link failure` /
`SQLNonTransientConnectionException: No operations allowed after connection
closed` from background EJB timers even while `mysql -h <local-mysql-host>
... SELECT 1` succeeds fine from the shell — proving MySQL itself is up and
it's specifically Payara's pool holding dead connections.

**Diagnose**: confirm MySQL responds directly first (rules out "DB is down"),
then confirm Payara's admin port responds to `list-applications` (rules out
"domain is down") — if both succeed but the HTTP listener (9090) times out,
suspect the connection pool.

**Fix**: flush both the main and audit pools (find pool names via
`grep -B2 'jndi-name="jdbc/coop"' domain.xml` /
`grep -B2 'jndi-name="jdbc/ruhunuAudit"' domain.xml` — e.g. `poolCoop` and
`poolRuhunuAuditLocal` locally):
```powershell
& asadmin.bat --port 5858 flush-connection-pool poolCoop
& asadmin.bat --port 5858 flush-connection-pool poolRuhunuAuditLocal
```
No redeploy or domain restart needed — a plain HTTP request succeeds
immediately after the flush. Verified while testing issue #22423 (itself a
stale-audit-pool-connection bug), where the local dev machine's own audit
pool had gone stale exactly the way the issue described.


## 63. `STAFF.ID` is not `PERSON.ID` — joining `billfee.staff_id` straight to `PERSON.ID` silently returns the wrong person's name

When hand-writing a verification/candidate-finding query against `billfee.staff_id`, do **not**
join it directly to `PERSON.ID` — `Staff` is its own entity with its own `ID`, related to `Person`
via `STAFF.PERSON_ID`. `billfee.staff_id JOIN person ON billfee.staff_id = person.id` silently
returns a row (some unrelated person whose `ID` happens to equal the staff's `ID`) instead of an
empty result, so the query looks correct but reports the wrong doctor's name for the due-payment
total. Always go through the extra hop: `JOIN staff s ON bf.staff_id = s.id JOIN person p ON
s.person_id = p.id`. Found while picking Playwright test data for issue #22860 — the initial
candidate list mislabeled staff ID 11865 as "N H W Mahinda" when the correct name (still under the
same ID, same due-fee totals) was "A K Liyanage"; the ID itself was fine to test with, only the
display name was wrong.


## 73. `asadmin deploy --contextroot /` from Git Bash gets mangled by MSYS path conversion — set `MSYS_NO_PATHCONV=1`

Running `asadmin.bat deploy --contextroot / --name rh <war>` from the Bash
tool (Git Bash/MSYS) fails with a `ConfigurationException` complaining it
can't parse `jndi:/server/D:/Program%20Files/Git//WEB-INF/faces-config.xml`.
MSYS auto-converts any bare leading `/` argument (like `--contextroot /`) into
an absolute Windows path rooted at the Git install dir before the argument
ever reaches `asadmin`, corrupting the context root and breaking the app's own
path resolution. `--port <n>` and named paths are unaffected — only a
standalone `/` argument triggers it.

**Fix**: prefix the command with `MSYS_NO_PATHCONV=1` to disable MSYS's
argument path-mangling for that call:
```bash
MSYS_NO_PATHCONV=1 "D:/Payara/bin/asadmin.bat" deploy --contextroot / --name rh "<path>/target/rh-3.0.0.war"
```
Verified while testing issue #22993.


## 74. Exploded Payara deployment lets you hot-swap a single XHTML file for a fast test/fix loop — real redeploy still required before commit

Local Payara deploys the WAR **exploded** (unzipped), not as a jar-in-place —
confirmed at `<payara-install>\glassfish\domains\domain1\applications\rh\`.
Facelets are not hot-reloaded in this configuration (production-mode
caching), so an XHTML edit under `src/main/webapp` needs a redeploy to take
effect — but for a JSF-only fix, copying the single corrected file straight
into the exploded app directory is a much faster iterate-and-recheck loop
than a full `package`/`asadmin redeploy` cycle:
```bash
cp "src/main/webapp/reports/inventoryReports/grn_summary_report.xhtml" \
   "D:/Payara/glassfish/domains/domain1/applications/rh/reports/inventoryReports/grn_summary_report.xhtml"
```
A plain `browser_navigate` reload picks it up immediately (no restart, no
session loss). This is a throwaway shortcut for iterating on a fix, not a
deployment method — always finish with a real `package` + `asadmin undeploy`/
`deploy` (§0a) before treating the change as verified, since that's what
actually proves the WAR builds and packages the fix correctly. Verified while
testing issue #22984 (caught an `outputLabel for=` component-id mismatch this
way in seconds instead of a multi-minute rebuild).

**Caveat — the hot-swap only works if that page has not been rendered yet in
the current app instance.** With `javax.faces.PROJECT_STAGE=Production` (this
project's `web.xml`) the Facelets refresh period is `-1`, so a page is compiled
once and cached for the lifetime of the deployment. Swap the file *before* the
first hit and the reload picks it up; swap it *after* the page has already been
rendered once and every subsequent reload silently serves the stale cached
facelet — the file on disk is right, the browser output is old, and nothing is
logged. Symptom: your newly added component simply isn't in the rendered page.
Fix: `asadmin deploy --force` (a new app classloader drops the cache); a browser
reload or hard refresh will not. Found while verifying issue #23342, where an
A/B run (original file → reproduce, fixed file → verify) needed a real redeploy
between the two halves.

**Second caveat — a swap can also break the view that is already open.** While
verifying issue #23723, `inward_bill_final.xhtml` was swapped while the page was
open in the browser. The swap *was* picked up (a fresh open rendered the new
markup), but the next AJAX postback from the already-open view failed: first
silently (the `p:ajax` listener never ran, so the edit didn't persist), then as
an HTTP 500 `IndexOutOfBoundsException: Index 0 out of bounds for length 0` at
`AttachedObjectListHolder.restoreState`. The saved view state no longer matches
the rebuilt component tree. That is a test artifact, not a defect. After any
swap, leave the page (Home → back through the menus) to get a fresh view before
testing again, and don't trust anything a pre-swap view did after the swap.

**Third caveat — a swapped CSS file is served fresh but the browser keeps the old one.**
JSF resource URLs (`javax.faces.resource/x.css?ln=css`) carry no version, so the
browser reuses its cached copy. Force it with
`browser_evaluate(() => fetch('/rh/faces/javax.faces.resource/x.css?ln=css', {cache:'reload'}))`,
then reopen the page through the menu.


## 87. The local `coop` DB's `WEBUSER` rows carry stale password hashes from whatever environment they were synced from — production login credentials will not work locally, and even a direct SQL password reset needs a domain restart to take effect

Logging into `http://localhost:8080/rh` with the production app-login credentials from the external credentials file (see `developer_docs/deployment/persistence-verification.md`) fails locally with "Invalid User! Login Failure" even though a `WEBUSER` row with that username exists. The local `coop` database is a data snapshot, not a fresh seed — its `WEBUSERPASSWORD` hash predates whatever the current production password is, and there's no way to know it from the codebase.

`SessionController.checkUsersWithoutDepartment()` calls `SecurityController.matchPassword(password, u.getWebUserPassword())`, which uses jasypt's `BasicPasswordEncryptor` (salted digest, not a fixed hash you can look up) — `SecurityController.java`'s `hashAndCheck()`/`matchPassword()`. To log in locally: generate a compatible hash with the same class (the jar is already on the classpath at `~/.m2/repository/org/jasypt/jasypt/1.9.3/jasypt-1.9.3.jar` — compile and run a two-line `BasicPasswordEncryptor().encryptPassword("SomeTestPassword")` snippet), then `UPDATE WEBUSER SET WEBUSERPASSWORD='<hash>' WHERE ID=<id>` directly against the local `coop` DB (safe — local test data, no schema change, see the `dev-issue-unattended` skill's hard limits on this point).

**The password won't take effect until Payara restarts.** `WebUser` is one of the reference entities EclipseLink L2-caches (`eclipselink.cache.size.default=1000` in `persistence_for_local_testing.xml`, explicitly called out for "departments, items, users"), and a plain SQL `UPDATE` doesn't invalidate that cache — the already-running app keeps serving the old hash to every subsequent login attempt from its in-memory copy, so retrying with the new password fails identically. `asadmin restart-domain domain1` (not just redeploying the WAR) clears it. This is the same L2-cache-staleness class of gotcha noted for the COGS report (`feedback_cogs_report_testing_gotcha` memory) — always double-confirm a direct SQL write against a running local Payara actually took effect, rather than assuming it did because the `UPDATE` succeeded.

Found while verifying issue #22990.


## 102. A local "Unknown column" 500 is schema drift — use the app's own migration page

A restored/older local DB can lag the entity model (e.g. `Unknown column 'DURATIONUNIT' in 'field list'`
loading a `TimedItemFee`), producing a 500 on pages that are otherwise unrelated to what you're testing.
The login screen flags this as **"Database Migration Pending"**. Fix it through the app's own UI —
navigate to `/faces/mf.xhtml` and click **Load Latest DDL from Wiki and Update Both Databases** — rather
than hand-writing `ALTER TABLE`. Re-check the column with `SHOW COLUMNS` before resuming the test.
Verified while testing issue #23222.


## 104. A department/room created by direct SQL needs more than the FK columns

When a test scenario needs a *new* Department or RoomFacilityCharge that doesn't already exist
locally (e.g. to simulate a patient's room belonging to a different department than the one they
were admitted from), inserting just the obvious FK columns produces a department that silently
breaks large parts of the UI instead of erroring:

- **`DEPARTMENT.DTYPE` must be set** (`'Department'`, matching the existing rows) — `Department` is
  `@Inheritance`-annotated, so a `NULL` discriminator isn't just "unmapped for this row", it makes
  the **entire polymorphic query return an empty list** (e.g. `SessionController.listLoggableDepts`
  during login), not just exclude the bad row. Symptom: login fails with "This user has no privilage
  to login to any Department" even though the WebUserDepartment row is correct.
- **`DEPARTMENT.DEPARTMENTTYPE` is compared case-sensitively** against the literal used elsewhere in
  the codebase (`'Inward'`, not `'INWARD'`). A mismatch doesn't error — the department loads and the
  header renders, but the entire top menu bar and all page-level toolbars silently disappear because
  their `rendered` conditions never match.
- **`DEPARTMENT.SITE_ID` should be copied from a real department of the same kind** — leaving it
  `NULL` is a further contributor to missing toolbar/menu regions on some pages.
- **Privileges (`WEBUSERPRIVILEGE`) are scoped by `DEPARTMENT_ID`**, and
  `SessionController.getUserPrivileges()` looks them up against `loggedUser.getDepartment()` (the
  session's *currently selected* department after login), not just the user. A brand-new department
  has zero privilege rows for any user, so every `hasPrivilege(...)`-gated button vanishes even
  though the same user has full privileges in their usual department. Fix: copy the relevant
  `WEBUSERPRIVILEGE` rows, changing only `DEPARTMENT_ID`, to the new department.
- **`ROOMFACILITYCHARGE.COMPANY_ID` must be set** when the scenario will exercise an
  institute-scoped search/filter (e.g. "Logged Institute"/"Logged Department" scope buttons) — those
  queries `AND` on `roomFacilityCharge.company = :loggedInstitution`, and a `NULL` company silently
  drops the row from every institute-scoped result with no error, while institute-unscoped ("Any
  Institute") searches still find it fine. This made a genuine fix look like it wasn't working until
  the room's `COMPANY_ID` was backfilled to match the test institution.

Each of the above requires a **Payara restart** to take effect if the row (or a row referencing it)
was already read once in the current server process — EclipseLink's shared L2 cache can otherwise
keep serving the pre-fix version of the entity even though the DB row is already corrected and a
brand new login/HTTP session is used. A plain redeploy is not enough; use
`asadmin stop-domain && asadmin start-domain`.

Verified while testing issue #23377.


## 111. The local `coop` DB can have **zero** vacant rooms — free some by SQL before testing any admission flow

The admission form's Room autocomplete (`roomFacilityChargeController.completeRoom`)
only returns a room when **no** `PATIENTROOM` row exists for it with
`RETIRED=0 AND DISCHARGED=0`, and the room's `CATEGORY.FILLED` is not `1`.
`InwardBeanController.isRoomFilled(room)` applies the same
`discharged=false` test. Restored production-shaped `coop` data is often at
or near full occupancy, so the Room autocomplete legitimately returns **no
suggestions** for any query — an admission simply cannot be completed, and
this looks like a broken autocomplete rather than a data state.

`Room` is `Room extends Category`, so room rows live in `CATEGORY` (`DTYPE='Room'`),
not a `ROOM` table. To free rooms on the **local** DB (disposable — see the
`dev-issue-unattended` hard limits), mark their active `PATIENTROOM` discharged
and clear any stuck `FILLED`:

```sql
UPDATE PATIENTROOM PR
JOIN ROOMFACILITYCHARGE RFC ON PR.ROOMFACILITYCHARGE_ID = RFC.ID
JOIN CATEGORY C ON RFC.ROOM_ID = C.ID
SET PR.DISCHARGED = 1
WHERE PR.RETIRED = 0 AND PR.DISCHARGED = 0
  AND C.NAME IN ('Room 100','Room 101','Room 102','Room 103','Room 104', ...);

UPDATE CATEGORY SET FILLED = 0
WHERE NAME IN ('Room 100','Room 101','Room 102','Room 103','Room 104', ...);
```

Verify with:

```sql
SELECT C.NAME FROM ROOMFACILITYCHARGE RFC JOIN CATEGORY C ON RFC.ROOM_ID = C.ID
WHERE RFC.RETIRED = 0 AND (C.FILLED IS NULL OR C.FILLED <> 1)
  AND C.ID NOT IN (
    SELECT RFC2.ROOM_ID FROM PATIENTROOM PR
    JOIN ROOMFACILITYCHARGE RFC2 ON PR.ROOMFACILITYCHARGE_ID = RFC2.ID
    WHERE PR.RETIRED = 0 AND PR.DISCHARGED = 0)
ORDER BY C.NAME;
```

(A room name repeats once per `ROOMFACILITYCHARGE` fee tier — that is normal.)
This is a local-only shortcut; never run it against a tunnelled/remote DB.
The proper app path is a Physical Discharge, but that is a long workflow just
to reclaim a bed for a test.

Found while verifying #23618-#23622 (admission + appointment-deposit-conversion
flows) — every `completeRoom` query returned nothing until rooms were freed.


## 118. Verifying an `@Asynchronous` dispatch: read the thread name in `server.log`, not the wall clock

A fix that moves work off the request thread (`@Asynchronous` EJB method) has
no visible signature in the UI — the page returns quickly either way, and "the
click felt fast" is not evidence. The proof is in `server.log`: every entry
carries `_ThreadName`, and container-managed async work runs on an EJB pool
thread rather than the HTTP listener.

```
[SEVERE] [com.divudi.ejb.EmailManagerEjb] [tid: _ThreadID=126 _ThreadName=__ejb-thread-pool9]
  Email Gateway URL is not configured.
```

`__ejb-thread-pool9` confirms the dispatch really was asynchronous. A
synchronous call would show `http-thread-pool::http-listener-1(N)` instead.
Grep for the logging class and read the thread name:

```bash
grep -a "YourEjbClassName" /d/Payara/glassfish/domains/domain1/logs/server.log | tail -5
```

The same trick distinguishes a `@Schedule` timer (`__ejb-thread-pool`) from a
user-triggered action, and catches the classic mistake where `@Asynchronous` is
silently ignored because the method was invoked on `this` from inside the same
bean (see the comment in `DatabaseMigrationService.java:80`) — self-invocation
keeps running on the request thread, and the thread name is the only place that
shows up.


## 119. The local dev box has no email or SMS gateway — verify the queued row, not the delivery

`EmailManagerEjb` logs `SEVERE: Email Gateway URL is not configured.` and
`SmsManagerEjb.sendSms()` returns `false` when none of the five
`SMS Sent Using …` config booleans is set. Neither is a defect locally; both
are simply unconfigured. So **no email or SMS feature can be verified
end-to-end on a local deployment** — the send will always fail.

Write the assertion against the persisted row instead, which is what the
feature actually controls:

```sql
SELECT receipientemail, messagesubject, messagetype, sentsuccessfully, pending
FROM appemail WHERE messagetype = '<YourMessageType>';
```

A correct implementation still produces the row, with the right recipient,
subject, body and foreign keys, and records the gateway's real verdict
(`sentsuccessfully=0`, `pending=1`) plus a log line. That distinguishes the
three cases a green screen cannot: *never attempted* (no row — the bug), *
attempted and refused by the gateway* (row + WARNING — correct behaviour
locally), and *delivered* (row with `sentsuccessfully=1` — only reachable on a
deployment with a configured gateway).

Companion to §41 (an empty `TRIGGERSUBSCRIPTION` table silently produces zero
notifications): check the subscription rows exist *and* the recipient has an
address on file before concluding anything from a quiet run.

## 141. Patient phone quick search matches `PATIENTPHONENUMBER` or `PATIENTMOBILENUMBER`, not `PERSON.PHONE`

Symptom: a phone number picked from `person.phone` opens the "new patient" form instead of the patient. `PatientController.quickSearchPatientLongPhoneNumber` matches the numeric `PATIENT.PATIENTPHONENUMBER` **or** `PATIENT.PATIENTMOBILENUMBER` (leading 0 dropped). Pick a number held by exactly one patient across both columns, or a selection list appears instead of an auto-select:
```sql
SELECT n FROM (
  SELECT id, patientPhoneNumber n FROM patient WHERE retired=0 AND patientPhoneNumber IS NOT NULL
  UNION SELECT id, patientMobileNumber FROM patient WHERE retired=0 AND patientMobileNumber IS NOT NULL
) x GROUP BY n HAVING COUNT(DISTINCT id)=1 LIMIT 5;
```
