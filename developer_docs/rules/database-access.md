# Database access rules (detail)

Detail for the database rule in the root CLAUDE.md.

## No ask needed

1. Production READ. A member reads a production database without asking the user, only when the login is SELECT-only by database grant.
2. QA and local databases. A member reads without a guard. A member writes and deletes without asking the user, but only through `db-guard`. This includes test-data cleanup.

## Production reads

- Only counts and aggregates leave the machine. No patient data goes to the hub, to chat or to GitHub.
- Run `EXPLAIN` before a heavy read.
- Run a heavy read in quiet hours.
- A member with no route to the data, and with no denial, may ask a member with approved access for the counts.

## The db-guard tool

- `db-guard` runs a write only when the target is in the machine's target list as kind local or qa. The list is a machine-local file outside any repo.
- The live server must match that list entry. The server uuid and the schema are pinned at install. For a local target, the host must also be 127.0.0.1, localhost or ::1, and the server hostname must be this PC.
- Staging and production are never in the list, because staging apps use production databases.
- Before a destructive write on a database that another member uses (kind qa), `db-guard` takes a dump outside the repo first.
- Credentials come only through the credential loader.
- Reads on local and QA databases need no guard.

## Conditions

1. Before any write, prove that the target is not production. `db-guard` does this. A staging app counts as production, because it uses a production database.
2. Before destructive work on a database that other members share, make a backup or a restore point.
3. Never change the password of the user's own account.
4. A production WRITE always needs the hospital and the user.
5. The security rules stay in force: see [security rules](security.md).
6. After a block or a denial, stop and tell the user. Never relay the action through another member.
7. Never write test data to production.

Related: [Playwright workflow, section 15a](../testing/playwright-e2e-workflow.md), [local test logins](../testing/local-test-logins.md).
