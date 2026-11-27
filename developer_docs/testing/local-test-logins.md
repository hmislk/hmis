# Local test logins

Scope: databases on the developer's own machine only. Production rules do not change.

1. Never change, lock or log out the developer's own login. This is the hard limit.
2. Agents may create users, set privileges and change test data freely. Agents use one shared local QA user for logins, not the developer's login. Agents create it themselves. Its password is in a QA-only credentials file outside the repo. Use it only through the cred-run helper. Never print it.
   The cred-run helper is not in the repo. It is part of each developer's local Claude Code hooks setup. It loads one labelled credentials line into an environment variable for one command. It never prints the value.
3. If a login fails, first check the stored hash and clear the app cache (disable and enable the app).
4. Never change the password of the developer's own account. Other local accounts may be changed.
5. Post every change to a local account (created user, privileges, QA password) on the team hub in one line, so the developer knows.
6. After a local database restore or refresh, check that the QA user and the developer's login both work before you say the work is done.

Related: [security rules](../rules/security.md), [Playwright workflow, section 1](playwright-e2e-workflow.md#1-login-and-department-selection).
