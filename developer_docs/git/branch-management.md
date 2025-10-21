# Branch Management Workflow

Standard approach for creating, maintaining, and merging branches in the HMIS repository.

## Naming Convention
- Use issue-centric names: `<issueNumber>-short-description` (e.g. `12875-implement-full-lab-workflow`).
- Keep names lowercase with hyphens; avoid personal initials or ambiguous abbreviations.
- Reserve long-running environment branches (`ruhunu-dev`, `rh-stg`, etc.) for deployment automation only.

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

## After Merge
- Delete the feature branch locally and remotely to keep the namespace tidy.
- Confirm the project board item moved to the next column.
- Coordinate with QA if deployment is required (see [QA Deployment Guide](../deployment/qa-deployment-guide.md)).

---
For GitHub CLI assistance, refer to [GH CLI Usage](../github/gh-cli-usage.md).
