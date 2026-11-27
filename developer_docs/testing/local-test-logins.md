# Local test logins

Scope: LOCAL test databases only. Production rules do not change.

1. Never change, lock or log out the developer's own login.
2. Use ONE shared local QA user per local database. It is its own webuser with the privileges the tests need. Never use the developer's login. Its password is on one labelled line of the credentials file. Use it only through the cred-run helper.
3. If a login fails, first check the stored hash and clear the app cache (disable and enable the app). Do not reset a password to fix it.
4. Never reset the password of any existing user. Only the QA user may be re-set.
5. Throwaway passwords, loops over or masking of secret-file lines, and secret-file scans stay forbidden.
6. After a local database restore or refresh, check that the QA user can log in before you say the work is done.

Related: [security rules](../rules/security.md), [Playwright workflow, section 1](playwright-e2e-workflow.md#1-login-and-department-selection).
