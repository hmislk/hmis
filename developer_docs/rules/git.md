# Git rules (detail)

Moved from the root CLAUDE.md; each rule there links here. The rules are unchanged.

- `git add`: never `git add -A` or `git add .`. Stage explicit paths only. This repo can carry many untracked local artifacts, so a sweep is a data-leak risk, not just noise. A scripted commit must print `git status --porcelain` and the exact file list, and refuse any path outside the intended set.
- Branching: branch from `origin/development` and target PRs at `development`, never `master` (hotfixes: use the `/hotfix-deploy` skill). Check for existing code against `origin/development`.
- One PR per issue: before opening a PR for issue #N, run `gh pr list --state all --search N`. If one is open, add to it; if one is merged, the fix is already in `development`. Never open a second PR for the same issue.
- Hotfix branch names: branches targeting a production/staging branch end in `-hotfix` (CI `check-branch` warns otherwise; QA-tested `home-qa*` heads are also accepted). Use the `/hotfix-deploy` skill. Exception: `rh-local-staging` accepts only `development` as the PR head (CI `branch_merge_validation.yml`).
- One open PR per deploy branch: check `gh pr list --state open --base <branch>`; if one exists, add to its branch (even another developer's) and update its title/body. CI `single-deploy-pr` fails the newer PR.
- Merged PR branch: before pushing to an existing PR's branch, run `gh pr view <N> --json state`. If it is MERGED, put the commit on a new branch from `origin/development` and open a new PR; a push to a merged branch never reaches `development`.
