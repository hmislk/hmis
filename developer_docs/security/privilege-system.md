# Privilege System Reference

HMIS relies on enum-driven privileges to gate sensitive UI flows. Follow this guide whenever you add or audit privileges.

## Core Principles
- **Declare privileges in `Privileges.java`** – append to the existing enum without reordering values.
- **Check privileges in controllers** via `webUserController.hasPrivilege(...)` to keep access rules consistent between pages.
- **Assign privileges** through the *User Privileges* admin interface; never seed them directly in the database.
- **Name privileges descriptively** so usage is obvious from the enum value alone.

## Common Privileges
| Privilege | Description |
| --------- | ----------- |
| `StockTransactionViewRates` | Exposes rate and value fields in stock transactions for authorized staff.
| `PharmacyTransferViewRates` | Allows viewing rates/values within pharmacy transfer issue reports.
| `Developers` | Toggles all bill formats for validation during development and QA.

## Implementation Workflow
1. **Define the privilege** in `Privileges.java` with any supporting comments.
2. **Reference it from the UI/controller** using `webUserController.hasPrivilege(Privileges.XYZ)`.
3. **Hide the UI otherwise** – combine privilege checks with JSF `rendered` attributes.
4. **Coordinate with administrators** to assign the new privilege to appropriate roles in production.

### Adding a New Privilege

> 🚨 **All three steps below are mandatory.** Adding only the enum value is NOT sufficient — the privilege will silently never appear in the admin UI, so administrators can never grant it. This was missed for `InpatientClinicalDischarge` in PR #19658, reported in issue #19677.

Follow this checklist whenever a new privilege is required:

**Step 1 — Declare it in `Privileges.java`**
- Inspect `src/main/java/com/divudi/core/data/Privileges.java` and **reuse an existing privilege** if one with matching behaviour already exists. For backward compatibility, *never* rename or edit legacy enum values even if they contain spelling or convention issues.
- If a new entry is necessary, add it to the most relevant section of the enum, keeping the alphabetical or functional grouping that already exists.
- Add a `case YourPrivilege:` entry in the `getCategory()` method in the same file so the privilege maps to the correct module.

**Step 2 — Register it in `UserPrivilageController.java`**
- Extend the `UserPrivilageController` implementation—specifically the method that builds the privilege tree—to place the new privilege as a `DefaultTreeNode` in the appropriate branch. Without this step the privilege **will not appear** in the admin UI at `/admin/users/user_privileges.xhtml` and can never be assigned.
- Example: `new DefaultTreeNode(new PrivilegeHolder(Privileges.YourPrivilege, "Display Label"), parentNode);`

**Step 3 — Guard the UI**
- Reference the privilege from XHTML using `rendered="#{webUserController.hasPrivilege('YourPrivilege')}"`.
- Update the admin privilege maintenance page `src/main/webapp/admin/users/user_privileges.xhtml` if static privilege rows are used (tree-based pages handle this automatically via Step 2).

## Stateless (API-Key) Privilege Checks

The UI's real privilege check — `webUserController.hasPrivilege(...)` → `SessionController.fillUserPrivileges(user, user.getDepartment(), false)` — only matches **direct** `WebUserPrivilege` grants (no `WebUserRolePrivilege`/role fallback) scoped to the user's **current department** (exact match, no NULL/global fallback in the typical path).

> 🚨 **When adding a stateless equivalent for an API-key endpoint, don't default to `UserManagementApi.isAdmin()`'s pattern.** `isAdmin()` checks direct grants with no department filter at all, OR falls back to a role-based grant — that's specific to how `Admin` happens to be granted, not a general template. First check `fillUserPrivileges`'s JPQL for the privilege in question; if it's department-scoped with no role fallback (true for most), scope the stateless check the same way, using the API key's resolved `WebUser.getDepartment()` as the stand-in for "current department." Only copy `isAdmin()`'s broader pattern if you've verified that's genuinely how that specific privilege works. **Reject a resolved user with a `null` department outright** — `fillUserPrivileges(user, null, false)` adds no department predicate, so passing a null department straight through would match that user's direct grants in *any* department instead of denying the check. Found while building the drawer-balance reset API (issue #24433, PR #24437) — a `code-review high` pass caught that copying `isAdmin()` verbatim granted broader authority than the UI equivalent.

## Gotcha: one invalid DB privilege string silently empties a user's whole menu

`WebUserPrivilege.privilege` is `@Enumerated(EnumType.STRING)`. If a `WEBUSERPRIVILEGE` row's `PRIVILEGE` column holds a string that isn't a current `Privileges` enum constant name (e.g. seeded directly via SQL for a privilege that was never actually added to the enum, or whose enum name later changed), `SessionController.fillUserPrivileges()`'s `findByJpql` call throws while EclipseLink deserializes that one row — and that exception isn't caught anywhere in the chain (`getUserPrivileges()` → `WebUserController.hasPrivilege()` → every `rendered="#{webUserController.hasPrivilege(...)}"` in `menu.xhtml`). The visible symptom is *not* an error page: the app keeps rendering, but almost the entire top menu silently disappears (only the handful of items with no privilege gate survive), identical in appearance to the user genuinely having no privileges. No exception appears in `server.log`.

**Diagnose:** compare that user's distinct granted privilege strings against the enum's current constant names:
```bash
grep -oP '^\s+[A-Za-z_][A-Za-z0-9_]*(?=\()' src/main/java/com/divudi/core/data/Privileges.java | sort -u > /tmp/enum_privileges.txt
mysql -u<user> -p<pass> <db> -N -e "SELECT DISTINCT PRIVILEGE FROM WEBUSERPRIVILEGE WHERE WEBUSER_ID=<id>;" | sort -u > /tmp/db_privileges.txt
comm -23 /tmp/db_privileges.txt /tmp/enum_privileges.txt   # prints any DB value the enum doesn't recognize
```
A `CREATEDAT IS NULL` row for the offending grant is a strong tell that it was inserted directly via SQL rather than through the *User Privileges* admin UI (which always sets it) — exactly the "never seed them directly in the database" rule above, and this is the concrete failure mode that rule exists to prevent.

**Fix:** retire (`RETIRED=1`), don't hard-delete, the offending row(s) — same as any other retirable grant. A fresh login is enough to pick this up; `userPrivilages` is cached per-`SessionController` instance (session-scoped), not application-wide, so no redeploy is needed. Found while testing issue #24471, where a single orphaned `CoveringLetters` grant (a privilege name that has never existed in this repo's history) on the local dev DB broke all menu-based Playwright navigation for `buddhika`, and survived a Payara JVM kill+restart and a full rebuild/redeploy of unrelated code because neither touches `WEBUSERPRIVILEGE` data.

## Testing Checklist
- Log in with an account that lacks the privilege to confirm the UI element stays hidden or disabled.
- Repeat with a privileged account and verify the workflow succeeds end-to-end.
- Document any new privilege in release notes so administrators know to grant it after deployment.

---
For additional persistence safeguards see the [Persistence Workflow](../persistence/persistence-workflow.md).
