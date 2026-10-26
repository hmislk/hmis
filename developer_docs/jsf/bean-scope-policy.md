# Bean Scope Policy

> **⚠️ Never change a bean's scope without discussing it with the lead developer first.**
> This includes adding, removing or replacing `@SessionScoped`, `@ViewScoped`,
> `@RequestScoped`, `@ApplicationScoped`, `@ConversationScoped`, `@Dependent`,
> `@Singleton`, `@Stateless`, `@Stateful` or a legacy `javax.faces.bean` scope
> on an existing class.

## Why

Most JSF controllers are `@SessionScoped` on purpose. Pages, menu actions and
other beans rely on a controller's state surviving navigation: a navigation
method sets fields and then redirects, a second bean injects the controller and
reads what the first page selected, and print/reprint flows come back to the
same instance.

Moving one controller to `@ViewScoped` gives every view its own instance, so
that state is silently lost. The resulting bugs show up on other pages than the
one you changed, which makes them hard to catch in review and testing.

The move away from session scope is planned as **one coordinated migration**,
together with a future framework upgrade. Changing beans one at a time inside
unrelated fixes makes the codebase inconsistent and that migration harder.

## What to do instead

- Fix the issue within the bean's current scope. A stale-state problem on a
  session-scoped controller is usually fixed by resetting fields in the
  navigation method that opens the page (see
  [Navigation Patterns](navigation-patterns.md)), not by changing the scope.
- If you believe a scope change is really needed, **stop and raise it with the
  lead developer** (on the issue, before writing the code). Explain which pages
  and beans use the controller and how each entry path keeps working.

## Enforcement

CI check **`bean-scope-guard`** (`.github/workflows/bean_scope_guard.yml`,
script `.github/scripts/check_bean_scope.py`) fails any PR that changes the
scope annotations of an existing Java class. New classes are not checked.

The check is overridden only by the PR label **`scope-change-approved`**, which
the lead developer adds after agreeing to the change. The label counts only when
it was applied by a login listed in `.github/scope-change-approvers`; a label
added by anyone else still fails the check. Do not add this label yourself.

The guard script and the approver list are read from the base branch, so a PR
cannot relax them for itself, and a PR that edits the guard files fails the
check unless approved.

Run it locally before pushing:

```bash
python3 .github/scripts/check_bean_scope.py origin/development
```
