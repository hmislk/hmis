# Playwright E2E — Privileges, departments, ConfigOption and caches

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [20. A privilege-gated button that never renders may be a missing DB row, not a session issue](#20-a-privilege-gated-button-that-never-renders-may-be-a-missing-db-row-not-a-session-issue)
- [24. Granting a privilege that doesn't exist on the checked-out branch silently blanks ALL privileges for that department](#24-granting-a-privilege-that-doesnt-exist-on-the-checked-out-branch-silently-blanks-all-privileges-for-that-department)
- [26. Editing a `ConfigOption` via raw SQL is invisible to the running app — use the admin UI](#26-editing-a-configoption-via-raw-sql-is-invisible-to-the-running-app--use-the-admin-ui)
- [97. Don't `disable` + `enable` the app to clear the L2 cache — restart the domain](#97-dont-disable--enable-the-app-to-clear-the-l2-cache--restart-the-domain)
- [35. Department-scoped dashboards need the department actually switched, not just full privileges](#35-department-scoped-dashboards-need-the-department-actually-switched-not-just-full-privileges)
- [44. A freshly-created test user needs a `WebUserDepartment` row, not just a `Department` field, to log in at all](#44-a-freshly-created-test-user-needs-a-webuserdepartment-row-not-just-a-department-field-to-log-in-at-all)
- [48. Raw SQL `UPDATE` on an already-cached EclipseLink-mapped entity (not just `ConfigOption`) can be invisible to the running app — the shared L2 cache is general, not `ConfigOption`-specific](#48-raw-sql-update-on-an-already-cached-eclipselink-mapped-entity-not-just-configoption-can-be-invisible-to-the-running-app--the-shared-l2-cache-is-general-not-configoption-specific)
- [93. Privileges are scoped per-department — a rendered button check can fail under the "wrong" department even though the user genuinely has the privilege](#93-privileges-are-scoped-per-department--a-rendered-button-check-can-fail-under-the-wrong-department-even-though-the-user-genuinely-has-the-privilege)
- [96. A `@SessionScoped` controller's already-loaded entity field does not pick up a sibling controller's later edit to the same row, even when that edit goes through the app's own JPA facade](#96-a-sessionscoped-controllers-already-loaded-entity-field-does-not-pick-up-a-sibling-controllers-later-edit-to-the-same-row-even-when-that-edit-goes-through-the-apps-own-jpa-facade)
- [107b. A bug that "does not reproduce" locally may be gated by a `ConfigOption` whose default hides it — flip the option before concluding the report is wrong](#107b-a-bug-that-does-not-reproduce-locally-may-be-gated-by-a-configoption-whose-default-hides-it--flip-the-option-before-concluding-the-report-is-wrong)
- [114. To make a SQL-inserted `ConfigOption` visible without a redeploy, click **Reload Config** on the Application Options page](#114-to-make-a-sql-inserted-configoption-visible-without-a-redeploy-click-reload-config-on-the-application-options-page)
- [124. A report's menu button can be privilege-gated per the *session department*, not the department whose data the report covers — switch department, not the report's own filter](#124-a-reports-menu-button-can-be-privilege-gated-per-the-session-department-not-the-department-whose-data-the-report-covers--switch-department-not-the-reports-own-filter)
- [135. A brand-new `Privileges` enum value is invisible in Manage Users → Manage Privileges until it's also registered in `UserPrivilageController`'s hand-built tree — and a mid-session grant needs a fresh login to take effect](#135-a-brand-new-privileges-enum-value-is-invisible-in-manage-users--manage-privileges-until-its-also-registered-in-userprivilagecontrollers-hand-built-tree--and-a-mid-session-grant-needs-a-fresh-login-to-take-effect)
- [138. Menu shows only Home and Request Manager after login on an older branch](#138-menu-shows-only-home-and-request-manager-after-login-on-an-older-branch)

---

## 20. A privilege-gated button that never renders may be a missing DB row, not a session issue

If a `rendered="#{webUserController.hasPrivilege('SomePrivilege')}"` button never
appears even after the §17 logout/relogin-and-reselect-department trick, the
`webuserprivilege` row for that (user, department, privilege) triple may simply not
exist in the local seed data — no amount of re-login fixes a privilege that was never
granted for that department. Check first:

```sql
SELECT ID, PRIVILEGE, DEPARTMENT_ID, RETIRED
FROM webuserprivilege
WHERE WEBUSER_ID = <id> AND PRIVILEGE = 'SomePrivilege';
```

If the row for the target department is absent, insert it (`RETIRED = 0`) for that
`WEBUSER_ID`/`DEPARTMENT_ID`, then follow §17 (logout → login → reselect department)
to force `SessionController.fillUserPrivileges()` to re-read it — the privilege list is
cached per session at login and won't pick up a new row otherwise. This came up testing
`BhtSummeryController.settle()` (`InwardSettleFinalBill`), where the local `buddhika`
user had the privilege for `Store`/`Main Pharmacy` departments but not `Inward`.

**Prefer granting it through the app over an `INSERT`.** *Administration → Manage Users
→ View Staff Users → filter the user → select the row → Manage Privileges → pick the
department → **List Privileges** → tick the node → **Update User Privileges*** does the
same thing through the real screen, and confirms the privilege label a user would look
for. Two things to watch:

- **The tree pre-loads the user's current selection, so check the count before saving.**
  Read it back before clicking Update — it should equal the existing active row count
  plus the one you ticked:
  ```js
  Array.from(document.querySelectorAll('.ui-treenode > .ui-treenode-content .ui-chkbox-box'))
       .filter(b => b.querySelector('.ui-icon-check')).length
  ```
- **The save is a full replace, and it can silently retire a privilege the tree didn't
  represent.** Granting `ReportsProfessionalPayments` for issue #23676 also flipped
  `LabBillSearch` to `RETIRED = 1` for the same department — with `RETIREDAT` and
  `RETIRER_ID` left `NULL`, so nothing in the row says who did it. Snapshot the active
  set before and diff it after:
  ```sql
  SELECT PRIVILEGE FROM webuserprivilege
  WHERE WEBUSER_ID = <id> AND DEPARTMENT_ID = <dept> AND RETIRED = 0 ORDER BY PRIVILEGE;
  ```

Either way, finish with §17 (logout → login → reselect department): the privilege list is
cached per session at login, so a freshly granted privilege does **not** appear until you
log back in — the report button stays absent and it looks like the grant failed.

**Before inserting a row, check whether some *other* department already has it** — picking
that department on the login screen needs no DB write at all and is the faster route:

```sql
SELECT PRIVILEGE, DEPARTMENT_ID FROM webuserprivilege
WHERE WEBUSER_ID = <id> AND RETIRED = 0 AND PRIVILEGE = 'SomePrivilege';
```

Testing issue #23484's pharmacy-admin pages, `PharmacyItemNameEdit` existed for `Inward`
only, so every **Add New**/**Edit** button on `vtm_dto.xhtml`, `store_vtm.xhtml` and
`lab_vtm.xhtml` rendered `disabled` under the default `Main Pharmacy` department — nothing
to do with those pages, and no privilege row needed. Logging out and reselecting `Inward`
enabled all of them. A `disabled` (rather than absent) admin button is the tell: per this
project's convention, privilege-gated controls are disabled, not hidden, so a greyed-out
button means "wrong department", not "broken page".

**`WebUser.department` is not a fixed "home department" — `SessionController.selectDepartment()`
overwrites and persists it (`loggedUser.setDepartment(department); getFacede().edit(loggedUser)`)
every time the department-selection screen is submitted, which is why it pre-fills with
whatever was picked last time.** The catch for privilege testing:
`SessionController.getUserPrivileges()` calls
`fillUserPrivileges(getLoggedUser(), getLoggedUser().getDepartment(), false)` — by the time
this runs, `getLoggedUser().getDepartment()` already equals the department just selected for
*this* login, and `deptIsNull=false` means a `DEPARTMENT_ID IS NULL` privilege row is **never**
matched, no matter which department that is. Query `SELECT DEPARTMENT_ID FROM webuser WHERE
ID=<id>` *after* selecting the department you're about to test with, and insert the privilege
row with that exact `DEPARTMENT_ID` — a NULL-department row silently does nothing, even after
a full logout/login cycle.


## 24. Granting a privilege that doesn't exist on the checked-out branch silently blanks ALL privileges for that department

If you insert a `webuserprivilege` row for a `Privileges` enum value that exists on
*another* branch (e.g. one you tested earlier today) but not on the branch currently
checked out and deployed, the entire menu goes blank and every `hasPrivilege(...)` check
returns `false` for that user **in that department** — not just the one bad privilege.
`WebUserPrivilege.privilege` is `@Enumerated(EnumType.STRING)`; EclipseLink converts the
DB string to the Java enum via `Enum.valueOf(...)` when it hydrates the full result list
for `SessionController.fillUserPrivileges()`, and a single row whose string isn't a valid
constant on the *currently running* code silently poisons that entire fetch — with no
`SEVERE` entry in `server.log` and no visible page error, just empty menus / "not
authorized" everywhere for that department, while other departments the row doesn't
affect work fine (a strong tell if you compare departments). A domain restart or a full
undeploy+redeploy does **not** fix this — it's a data/branch mismatch, not a cache.

Diagnose fast: enable the MySQL general log to a table (`SET GLOBAL log_output='TABLE';
SET GLOBAL general_log='ON';`) and check `mysql.general_log` for the exact
`SELECT ... FROM WEBUSERPRIVILEGE WHERE ...` query, run it directly, then diff the
distinct `PRIVILEGE` values for that user/department against
`grep -oP '(?<=^    )[A-Za-z0-9_]+(?=\(")' src/main/java/com/divudi/core/data/Privileges.java`
(note: some enum lines have a trailing `//` comment that breaks a naive end-of-line
regex — verify any apparent mismatch with a direct `grep` before trusting the diff).

This came up switching from the GRN privilege-guard branch (issue #22019, which added
`PharmacyGrnCancel`/`PharmacyGrnReturnCancel`) to the PO privilege-guard branch (#22020,
checked out fresh from `origin/development` since #22019 wasn't merged yet) — rows
granted while testing #22019 were still sitting in the shared local DB and broke every
privilege check for that department under the PO branch's code. Fix: delete (or retire)
the rows for privileges that don't exist on the currently deployed branch, re-grant only
what the current branch's `Privileges.java` actually declares, then re-login.


## 26. Editing a `ConfigOption` via raw SQL is invisible to the running app — use the admin UI

`ConfigOptionApplicationController.getApplicationOption(key)` reads through EclipseLink's
shared L2 entity cache. A direct `UPDATE configoption SET optionvalue=... WHERE optionkey=...`
via the `mysql` CLI changes the DB row but the already-cached `ConfigOption` entity in the
running Payara instance keeps serving the old value — `getLongValueByKey`/`getBooleanValueByKey`
never see the change, with no error or log entry. This wasted a full test cycle while verifying
a day-limit config for issue #22055 (two `UPDATE` statements had zero effect on rendered button
state).

**Fix:** edit config values through `admin/institutions/admin_mange_application_options.xhtml`
(List Application Options → filter by Key → **Edit Option** → Save). That path goes through
the entity manager and correctly invalidates the cache, and the change is visible on the very
next page load — no redeploy or Payara restart needed. Reserve raw SQL for *reading* config
state (e.g. confirming a key auto-created with the right default on first access), never for
writing it mid-test.

The same cache applies to **any entity**, not just `ConfigOption`. A fixture row that is newly
`INSERT`ed is picked up by the next query (it is not in the cache yet), but an `UPDATE` to a row the
app has already loaded is not (issue #24150: retargeting a `PatientTransferRequest` fixture to a
different room kept rendering the old room). When a hand-built local fixture must be *changed* after
the app has read it, redeploy (or restart the domain) before re-checking, or insert a fresh row instead.


## 97. Don't `disable` + `enable` the app to clear the L2 cache — restart the domain

The disable→enable trick for flushing a poisoned EclipseLink shared cache (§ noted in
earlier sessions) loads the whole application a **second time in the same JVM**, and on
this codebase that reliably ends in `java.lang.OutOfMemoryError: Java heap space` +
`CDI deployment failure` mid-enable, leaving the app 404 (hit during issue #22011
verification). Restart the domain instead — slower, but it actually comes back up:

```bash
asadmin stop-domain <dom>    # "domain is already stopped" is fine — continue
asadmin start-domain <dom>
```

Run the two commands separately (not chained with `&&`): if the domain is already
down, `stop-domain` exits nonzero and a chained `start-domain` would be skipped,
leaving the app offline.


## 35. Department-scoped dashboards need the department actually switched, not just full privileges

While verifying #22213 (theatre stay billing), the Theatre Dashboard's "Awaiting
Theatre Acceptance" / "Pending Return to Ward" lists showed **0** rows even though
a request definitely existed (confirmed via direct DB query) and the logged-in
user had every relevant privilege. Root cause: `PatientTransferController`'s
loader methods (`loadPendingForTheatre()`, `loadInTheatreRequests()`, etc.) filter
on `r.toRoomFacilityCharge.department = sessionController.getDepartment()` — the
**currently selected** department, not "any department the user has access to."
Being logged in under "Inward" and merely navigating to a Theatre page renders it
fine but shows empty lists. Fix: log out and back in, and on the Select Department
screen explicitly pick the department the workflow actually belongs to (here,
"THEATRE") before testing department-scoped actions — the app remembers your last
selection and will silently keep applying it across unrelated page navigations.


## 44. A freshly-created test user needs a `WebUserDepartment` row, not just a `Department` field, to log in at all

Creating a disposable test user via Admin > Manage Users > Add New User
(`admin/users/user_add_new.xhtml`) and setting its `Department` field is not
enough to let it log in. Login checks `listLoggableDepts(user)`
(`SessionController.java`), which queries the `WebUserDepartment` join table
— not the `WebUser.department` column. With no matching `WebUserDepartment`
row, login fails with "This user has no privilage to login to any
Department. Please conact system administrator." even though the user
record itself looks fully configured.

The Add New User form has no field for this; department-login grants are
managed separately via **Manage Users > (select user) > Manage User
Departments**. For a quick disposable test account it's simplest to insert
the row directly:
```sql
INSERT INTO webuserdepartment (CREATEDAT, RETIRED, DEPARTMENT_ID, WEBUSER_ID)
VALUES (NOW(), 0, <department_id>, <webuser_id>);
```
Also useful: no `WebUserRole` in this DB grants `ShowServiceCharges` (verified
via `webuserroleprivilege`) — it's only assigned to individual users
directly in `webuserprivilege`. So any freshly-created user with no role
already lacks it, no extra step needed to test privilege-gated hiding.

Found while verifying issue #22310 (fee row hidden along with its item name
when `ShowServiceCharges` is absent) — needed a throwaway non-privileged
login to confirm the fix without touching any real staff account.


## 48. Raw SQL `UPDATE` on an already-cached EclipseLink-mapped entity (not just `ConfigOption`) can be invisible to the running app — the shared L2 cache is general, not `ConfigOption`-specific

§26 documents this for `ConfigOption` specifically, but `eclipselink.cache.size.default`
in `persistence.xml` applies to every entity class, so the same trap exists for `Institution`,
`Patient`, or any other frequently-read entity **once that row has already been loaded into
the shared L2 cache during the current app run** — visibility depends on persistence-context/
cache state, not a blanket guarantee that every raw SQL update is invisible. Hit while
verifying issue #22371: a direct `UPDATE INSTITUTION SET DEFAULTINSTITUTION=1,
POINTOFISSUENO='COOP' WHERE id=2` via the `mysql` CLI changed the DB row, but the next page
load still showed the old (unset) values in the edit form and the PHN-generation code path
still saw a blank POI — the already-cached `Institution` entity in the running Payara instance
kept serving stale field values, with no error anywhere. Re-doing the exact same change through
the admin UI form (Save button, which goes through `EntityManager.merge`/`edit`) fixed it
immediately, confirming the raw SQL path was the problem. **Rule of thumb: if a row might
already be cached (anything read earlier in the same test session), don't `UPDATE` it via raw
SQL mid-test — use the corresponding admin UI/CRUD screen instead, so the cache gets properly
refreshed, and reserve raw SQL for read-only verification queries.**


## 93. Privileges are scoped per-department — a rendered button check can fail under the "wrong" department even though the user genuinely has the privilege

Seen verifying issue #23510 (Surgery Dashboard "Remove" a validated timed
service). `theater/surgery_bill_summary.xhtml`'s **Validate Surgery** button
is gated by `webUserController.hasPrivilege('InwardSurgeryValidate')`. The
test user held that privilege (`WEBUSERPRIVILEGE` row, `PRIVILEGE =
'InwardSurgeryValidate'`) — but the row's `DEPARTMENT_ID` pointed at "Inward",
not the "THEATRE" department selected at login. Under THEATRE the button
simply didn't render (no error, no disabled state — just absent), which looks
identical to "user lacks the privilege" from the UI alone.

**Diagnose it this way**: don't stop at confirming a `WEBUSERPRIVILEGE` row
exists for the user — check its `DEPARTMENT_ID` against the department
actually selected for the session:

```sql
SELECT ID, PRIVILEGE, DEPARTMENT_ID FROM WEBUSERPRIVILEGE
WHERE WEBUSER_ID = <id> AND PRIVILEGE = '<PrivilegeName>';
```

If the department differs from the one under test, log out and reselect the
department that matches the privilege row — per §1 there is no in-session
department switch. Never grant a new `WEBUSERPRIVILEGE` row yourself to route
around this; that is a privilege/access-control change, out of bounds for
verifying a fix.


## 96. A `@SessionScoped` controller's already-loaded entity field does not pick up a sibling controller's later edit to the same row, even when that edit goes through the app's own JPA facade

Tried to reproduce issue #23523 (`BillBhtController.errorCheck()` silently
no-opping Settle when the admission's current room becomes invalid) by: (1)
adding a service on `inward_bill_service.xhtml` while the room was valid so
an item was staged in `lstBillEntries`, then (2) in a second tab of the same
login session, using `inward_patient_room_details.xhtml`'s "Remove Room" —
a real UI action, going through `RoomChangeController.removeRoom()` and
`patientEncounterFacade.edit(encounter)`, not raw SQL — to null the
encounter's `currentPatientRoom`, then (3) going back to tab one and
clicking **Settle**.

Expected the new guard message; got `Bill Saved` instead, and confirmed via
`BILL` table that Settle fully succeeded, using the *old* room. The DB
correctly showed `CURRENTPATIENTROOM_ID = NULL` before the Settle click.
`BillBhtController`'s `patientEncounter` field — set once when tab one
searched for the BHT — is a Java object this session-scoped bean has held
onto since; `RoomChangeController`'s edit in tab two updates its own
(different) reference to the same row and, evidently, does not force tab
one's already-held reference to see the new field values. Confirmed the fix
itself was genuinely deployed first (`strings` on the compiled `.class` in
`applications/<app>/WEB-INF/classes/...` showed the new message text) before
concluding this was a test-setup limitation, not a dead code path.

**Takeaway for testing this kind of thing:** a same-request-lifecycle race
like this cannot be reliably staged from *outside* the request (a second
tab, a second session, or raw SQL) once the first controller has already
loaded and cached the entity — only an interruption inside the *same*
request/thread (a debugger, a breakpoint, or an actual concurrent user
hitting the exact same in-flight transaction) would show it happening.
Don't spend a long session trying to force this kind of window through the
UI; verify instead via the code path itself (confirm the guarded condition
and message are correct, and that an analogous guard with the identical
condition and message idiom already fires correctly elsewhere in the same
controller/page) and say so plainly in the PR rather than claiming a live
repro that didn't happen.

**This is not only a testing-methodology footnote, though** — CodeRabbit
correctly flagged on PR #23567 that the same staleness is a real
production correctness risk, not just a Playwright limitation: any actual
concurrent use (two staff members on the same admission, or one user in
two tabs) can hit this exact window, silently letting `settleBill()` go
through on stale room data instead of hitting the guard added in #23523.
Tracked separately as issue #23568 rather than fixed inline in #23523's
PR, since the right fix (refreshing `patientEncounter`/`currentPatientRoom`
before validation) needs its own design discussion — this codebase's
EclipseLink cache has bitten targeted-refresh attempts before (see the
"Native settle poisons the Bill entity" gotcha).

Found while verifying issue #23523.


## 107b. A bug that "does not reproduce" locally may be gated by a `ConfigOption` whose default hides it — flip the option before concluding the report is wrong

Issue #23577 ("Cannot navigate to Dashboard for Baby Admission") did not
reproduce on the first attempt: a baby admission's **Inpatient Dashboard**
button opened the dashboard exactly as it should. Every local hospital DB copy
(`coop`, `ruhunu`, `rmh`, `sl`) had *Patient admission and room assignment are
simultaneous processes.* set to `true`, and `getBooleanValueByKey(key, true)`
also defaults it to `true` — so the entire `else` branch that contained the bug
was unreachable locally.

The failing path only exists when that option is **off**. Flipping it (via the
admin UI — §26, raw SQL is invisible to the running app) reproduced the report
on the first click, every time.

**Before recording "did not reproduce" on a bug report, read the controller
method for `configOptionApplicationController.getBooleanValueByKey(...)`
branches and check the local value of each one against its default.** A
reporter on a differently-configured hospital is describing a real code path
you simply are not executing; the option's default is not the only value in
production. The same check applies in reverse when verifying a fix — exercise
both settings of any option that gates the code you touched, since the branch
you did not test is the one someone is running.

Found while fixing issue #23577.


## 114. To make a SQL-inserted `ConfigOption` visible without a redeploy, click **Reload Config** on the Application Options page

Verifying a *new* toggle (one the code reads via `getBooleanValueByKeyReadOnly`,
which by design never creates the row) has a chicken-and-egg problem: the
Application Options admin page (*Administration → Manage Institutions →
Application Options*) only lets you Edit/Delete rows that already exist, so a
key with no row can't be set there. `INSERT` the row directly
(`OPTIONKEY`, `OPTIONVALUE`, `RETIRED=0`, `SCOPE='APPLICATION'`,
`VALUETYPE='BOOLEAN'`) — but per §26/§48 that write is invisible to the
running app because `ConfigOptionApplicationController` caches the whole table
at load. Instead of restarting the domain (§97), click the **Reload Config**
button on that same Application Options page: it re-runs `loadApplicationOptions()`
and the new value takes effect immediately. Used on #23651 to flip
`Inward Final Bill - Bundle Grouped Charge Types` between runs.

Note the department-scoped-key-first resolution (`feedback_config_option_scope_resolution`):
`getBooleanValueByKeyReadOnly("X", …)` with a department selected looks up
`"<Dept> - X"` before the plain `"X"`, so an admin who saved the toggle from a
department context produces a `"Inward - X"` row, not `"X"`. Insert whichever
one matches how it will really be set (the plain global key is usually right).


## 124. A report's menu button can be privilege-gated per the *session department*, not the department whose data the report covers — switch department, not the report's own filter

Also found on issue #23604. `reports/index.xhtml`'s report buttons are each
wrapped `rendered="#{webUserController.hasPrivilege('ReportsGrnSummaryReport')}"`,
and `WebUserController.hasPrivilege()` checks
`sessionController.getUserPrivileges()` — the privilege set loaded for
whichever department was picked on the **Select Department** screen at
login, not the department the report will actually query. A local test user
can hold `ReportsGrnSummaryReport` for one department (e.g. `Inward`) but not
another (e.g. `Main Pharmacy`) — querying
`WEBUSERPRIVILEGE.DEPARTMENT_ID` confirms which. If the button is missing
(or, per §123, the whole accordion panel renders as a set of empty
`<div class="d-flex...">` wrappers with **zero** child buttons — every
`rendered` in the panel evaluating false, not just one), check this before
assuming a defect: query
`SELECT wup.PRIVILEGE, d.NAME FROM WEBUSERPRIVILEGE wup JOIN WEBUSER wu ON wu.ID=wup.WEBUSER_ID LEFT JOIN DEPARTMENT d ON d.ID=wup.DEPARTMENT_ID WHERE wu.NAME='<user>' AND wup.PRIVILEGE LIKE '%<ReportPrivilege>%'`.

If the privilege exists only for a different department, switch to that
department per §17 (`logout.xhtml` → login → **Select Department**) to reach
the report's menu entry — the report page's own `Institution`/`Site`/
`Department-Store` filter fields are independent of the session department,
so once on the page, point those filters back at the department whose data
you actually need to verify.


## 135. A brand-new `Privileges` enum value is invisible in Manage Users → Manage Privileges until it's also registered in `UserPrivilageController`'s hand-built tree — and a mid-session grant needs a fresh login to take effect

Adding a new panel/action gated by a brand-new `Privileges` enum constant (issue #24133: `InpatientDashboardPanelPackage`, `InwardPackageChange`) is not enough on its own to test it — two separate, easy-to-miss gaps:

1. **The privilege-assignment UI has its own registry, separate from the enum.** `Privileges.java`'s
   category `switch` (the block with `case InwardPackageAdmission:` etc.) only controls which broad
   category (`"Inward"`, `"OPD"`, …) a privilege *reports as* — it does **not** make the privilege
   selectable anywhere. The actual tree shown on **Administration → Manage Users → View Staff Users →
   (select user) → Manage Privileges → List Privileges** is hand-built, one `new
   DefaultTreeNode(new PrivilegeHolder(Privileges.X, "Label"), parentNode)` call per privilege, in
   `UserPrivilageController.createPrivilegeHolderTreeNodes()`. A privilege missing from that method
   compiles fine, works fine in `hasPrivilege(...)` checks, and is simply **absent from the tree** — no
   error, just nothing to search for or check. Confirm with `document.body.textContent.includes('<your
   label>')` after "List Privileges" (use `textContent`, not `innerText` — collapsed tree branches are
   `display:none` and `innerText` silently excludes them). Fix: add the missing
   `new DefaultTreeNode(new PrivilegeHolder(Privileges.YourNewPrivilege, "Label"), someExistingParentNode)`
   line next to its siblings (e.g. alongside `InpatientDashboardPanelRoomManagement` under
   `dashboardPanelsNode`, or alongside `InwardPackageAdmission` under `inwardPackageNode`).

2. **`SessionController.getUserPrivileges()` lazily loads once and caches for the whole HTTP session**
   (`if (userPrivilages == null) { userPrivilages = fillUserPrivileges(...); }`). Granting a privilege to
   the currently-logged-in test user via "Update User Privileges" does **not** retroactively affect that
   same browser session — the panel/button stays invisible until a genuinely fresh login. `document.cookie`
   manipulation does **not** force this: the session cookie is `HttpOnly`, invisible to JS, so clearing
   `document.cookie` and reloading just resumes the same session. Use the app's own logout instead —
   `document.querySelector('[id$="btnLogout"]')?.click()` (the logout `p:commandButton`'s id always ends
   in `btnLogout` regardless of the generated `j_idt###` form prefix) — then log back in.


## 138. Menu shows only Home and Request Manager after login on an older branch

The branch's `Privileges` enum lacks constants the DB stores (privileges are stored by name), so no privilege loads. List the missing ones and copy their single-line declarations from development into the enum (additive only):
```bash
F=src/main/java/com/divudi/core/data/Privileges.java
ext(){ grep -oE '^\s*[A-Z][A-Za-z0-9_]*\s*\(' | sed -E 's/[ (\t]//g' | sort -u; }
comm -23 <(git show origin/development:$F | ext) <(ext < $F)
```
To make them grantable in the UI, also wire them into `UserPrivilageController` (§135).
