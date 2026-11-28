# Merge rules for dev-hub team sessions

Moved from the root CLAUDE.md; each rule there links here.

Dev-hub team sessions (the user's own Claude sessions) may open a PR without asking when the work was agreed on the hub or comes from an assigned issue. They may merge into `development` only when ALL hold:
1. A `REV OK <sha> — <member name>` on the head sha, from a member whose session did not write the PR. A bare unsigned one does not count. All members post from one shared GitHub account, so the GitHub author is the same on every comment. The REV OK counts only when the reviewing member's own session posted it and signed it with its own name. No session posts or signs a REV OK for another member. The merger checks the hub log for the reviewer's `For sessions: REV OK <sha>` line. If the hub log has no such line, the REV OK does not count.
2. CI green.
3. CodeRabbit has reviewed the head sha (its commit status for that sha is `success` and its description does not say "rate limit", "limit reached" or "in progress"; a rate-limited CodeRabbit still shows green), or the PR records `CR skipped: <reason>`.
4. Review threads resolved.
5. Merged with `gh pr merge --merge --match-head-commit <full sha>` (this repo uses merge commits, not `--squash`).
6. Then delete the head branch (`git push origin --delete <branch>`) unless an open PR targets it (`gh pr list --base <branch> --state open` is not empty; deleting it closes stacked PRs) or it is `development`, `master` or a deploy branch.
7. Never `--admin`.
If the author is not sure the fix is right, two members must e2e-test it before merge (the author plus one other, on a different machine/app) and record both results on the PR. Minor changes need only the REV OK.
Also: never merge into `master` or any `*-production` / `*-staging` branch; merging deploys and stays the user's call. Opening hotfix PRs to them is fine.

## "Done" criteria

For work that needs an e2e test:
1. Before the code, the author and the tester agree the pass/fail "done" criteria.
2. Each criterion names its measure: a screen, a query or a counter. Example: "the settle sends no temporary-table statements, counted with the MySQL statement counter".
3. A reviewer may add a criterion during the REV.
4. One failed criterion fails the e2e.
5. Write the criteria and measures in the issue. The repo is public. Put hardening criteria, and criteria with hospital data, in the hub task notes.
