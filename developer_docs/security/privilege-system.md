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

## Testing Checklist
- Log in with an account that lacks the privilege to confirm the UI element stays hidden or disabled.
- Repeat with a privileged account and verify the workflow succeeds end-to-end.
- Document any new privilege in release notes so administrators know to grant it after deployment.

---
For additional persistence safeguards see the [Persistence Workflow](../persistence/persistence-workflow.md).
