# Merge rules for dev-hub team sessions

Moved from the root CLAUDE.md; each rule there links here. The rules are unchanged.

Dev-hub team sessions (the user's own Claude sessions) may open a PR without asking when the work was agreed on the hub or comes from an assigned issue. They may merge into `development` only when ALL hold:
1. A `REV OK <sha> — <member name>` from a non-author member on the head sha, posted on the PR by that member (a bare unsigned one, or one the merger posts, does not count).
2. CI green.
3. CodeRabbit has reviewed the head sha (its commit status for that sha is `success` and its description does not say "rate limit", "limit reached" or "in progress"; a rate-limited CodeRabbit still shows green), or the PR records `CR skipped: <reason>`.
4. Review threads resolved.
5. Merged with `gh pr merge --merge --match-head-commit <full sha>` (this repo uses merge commits, not `--squash`).
6. Then delete the head branch (`git push origin --delete <branch>`) unless an open PR targets it (`gh pr list --base <branch> --state open` is not empty; deleting it closes stacked PRs) or it is `development`, `master` or a deploy branch.
7. Never `--admin`.
If the author is not sure the fix is right, two members must e2e-test it before merge (the author plus one other, on a different machine/app) and record both results on the PR. Minor changes need only the REV OK.
Also: never merge into `master` or any `*-production` / `*-staging` branch; merging deploys and stays the user's call. Opening hotfix PRs to them is fine.
