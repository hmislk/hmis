# Branch Management Workflow

Standard approach for creating, maintaining, and merging branches in the HMIS repository.

## Naming Convention
- Use issue-centric names: `<issueNumber>-short-description` (e.g. `12875-implement-full-lab-workflow`).
- Keep names lowercase with hyphens; avoid personal initials or ambiguous abbreviations.
- Reserve long-running environment branches (`*-production`, `*-staging`, `home-qa*`) for deployment automation only.

## Daily Workflow
1. Create branches from the active development base (`development` unless the issue specifies otherwise).
2. Sync with upstream frequently: `git fetch` + `git rebase origin/development` before pushing.
3. Keep commits focused and include auto-close keywords (`Closes #12345`) in the final merge commit or PR description.
4. Update the GitHub project board status when moving between stages (In Progress → Ready for Review → Done).

## Pull Request Integration
- Target the appropriate environment branch (usually `development`; use client-specific branches only when instructed).
- Use the HMIS PR template and link the issue with an auto-close keyword.
- Request review from the domain owner and follow the [Commit Conventions](commit-conventions.md).
- Ensure persistence safeguards (no hardcoded JNDI values) before marking ready for merge.

## Changeset Guard (public repo)
The repo is public, and a file that lands in a commit stays in the history even after a later delete.
- **Install the hooks once per clone:** `./scripts/install-dev-hooks.sh`. It adds the gitleaks pre-commit hook and a `pre-push` hook that blocks a push whose outgoing commits add a risky file. Bypass one push only, after checking every named file: `HMIS_ALLOW_RISKY_PUSH=1 git push ...`. If the clone already has its own `.git/hooks/pre-push`, the installer leaves it alone and warns; add `bash "$(git rev-parse --show-toplevel)/scripts/hooks/pre-push" "$@" || exit 1` to that hook yourself.
- **CI (`changeset_guard.yml`) runs the same check on every PR whose branch or base contains the workflow file** (`changeset-guard` job) and fails on files added under `tmp/` or `.playwright-mcp/`, root-level `*.png`/`*.jpg`/`*.jpeg`/`*.pdf`, `tmp_*.py`, `*.sql` outside the app's SQL folders, `*.sql.gz`/`*.dump`/`*.bak`, `*.log`, `*.tgz`/`*.zip`, `.env`, `*credentials*`, `*.pem`, `id_rsa*`, and any file over 5 MB.
- Rules and allow-list: `.github/changeset-guard-patterns.txt`. A legitimate new path gets an `allow` line in the same PR. `allow` does not exempt `deny` rules (credentials, keys) or files over 5 MB. To fix a failure, remove the file from the commit that adds it (rebase/reset), not in a new commit.
- The `hotfix-label` job fails when a PR into a `*-production`/`*-staging` branch lacks the `hotfix` label (`rh-local-staging` is exempt). It re-runs when the label or the base branch changes.

## After Merge
- Delete the feature branch locally and remotely to keep the namespace tidy.
- Confirm the project board item moved to the next column.
- Coordinate with QA if deployment is required (see [QA Deployment Guide](../deployment/qa-deployment-guide.md)).

---
For GitHub CLI assistance, refer to [GH CLI Usage](../github/gh-cli-usage.md).
