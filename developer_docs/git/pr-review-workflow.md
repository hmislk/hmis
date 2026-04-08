# PR Review Workflow — Handling CodeRabbit and Codex Comments

This guide covers the standard workflow for reviewing and resolving comments from automated reviewers (CodeRabbit, Codex) and human reviewers on GitHub pull requests.

## Full Workflow

```
git fetch origin
→ checkout branch locally
→ investigate comments with Claude
→ discuss with user (SOS)
→ batch-fix valid comments
→ check persistence.xml
→ verify CI is green
→ push
→ reply to each comment individually
→ re-request review
→ merge → delete branch (GitHub UI)
→ git branch -d locally
```

## Step-by-Step

### 1. Fetch and Checkout

```bash
git fetch origin
git checkout <branch-name>
```

Always fetch first — Codex may have auto-pushed fixes you don't have locally.

### 2. Investigate Comments

- Read the PR link and each reviewer comment carefully
- Use Claude to read the relevant code context (`Read`, `Grep`, `Glob`)
- Understand *why* the comment was raised before deciding if it's valid

### 3. Discuss Before Fixing (SOS)

- Not all review comments are correct — automated tools often flag false positives
- Discuss each comment with the user before acting
- Common false positives in this project:
  - Suggesting null checks where lazy init already handles it (e.g., `getBillFinanceDetails()`)
  - Recommending constructor changes (violates HMIS rule: never modify existing constructors)
  - Flagging intentional typos like `purcahseRate` (database compatibility)
  - Suggesting native SQL when JPQL is perfectly adequate

### 4. Batch Valid Fixes

- Group all valid fixes into one commit (or a few logical commits)
- Do NOT make one commit per comment — keep history clean
- Use imperative mood in commit message, include `Closes #N` if this resolves the issue

### 5. Pre-Push Checklist

- **persistence.xml** — must use `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`, not hardcoded JNDI names. See [Persistence Configuration Guide](../deployment/persistence-verification.md)
- No credentials or `.env` files staged
- JSF-only changes (XHTML, no Java) do not require compilation

### 6. Push and Reply

```bash
git push
```

Then on GitHub, reply to **each reviewer comment individually** with:
- What was fixed (if valid)
- Why it was not changed (if dismissed) — be specific

Do **not** resolve other reviewers' conversations yourself. CodeRabbit auto-resolves when it detects the fix. For human reviewers, let them resolve.

### 7. Re-Request Review

After pushing fixes, click **"Re-request review"** on the PR page to notify reviewers. Do not wait for them to notice the new push.

### 8. Verify CI Before Replying

Check that CI is green after pushing before replying to comments. A fix that breaks CI is not ready.

### 9. Merge and Cleanup

- Merge via GitHub UI (squash or merge commit per project convention)
- Check **"Delete branch"** on the merge confirmation dialog
- Delete locally:

```bash
git branch -d <branch-name>
```

## Notes

- All PRs target `development`, never `master`
- "Re-request review" is distinct from just pushing — always click it
- Replying to each comment individually maintains a clean audit trail
- The `/review-pr` skill automates the investigation and fix steps of this workflow
