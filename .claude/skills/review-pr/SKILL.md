---
name: review-pr
description: >
  Handle CodeRabbit and Codex review comments on a GitHub PR. Fetches the PR,
  investigates each comment against the codebase, discusses validity with the user,
  batches valid fixes, and guides through push and reply steps.
allowed-tools: Read, Grep, Glob, Bash
argument-hint: "<pr-url-or-number>"
---

# PR Review Workflow

Handle review comments (CodeRabbit, Codex, human reviewers) on a GitHub pull request.

## Arguments

- `$0` — GitHub PR URL or PR number (required)

## Steps

### 1. Fetch and Checkout

```bash
git fetch origin
gh pr checkout $0
```

Always fetch first — automated tools may have pushed fixes not yet local.

### 2. Load PR Comments

```bash
gh pr view $0 --comments
pr_number=$(gh pr view $0 --json number --jq '.number')
gh api repos/hmislk/hmis/pulls/$pr_number/comments
```

List all review comments. Group them by file/topic.

### 3. Investigate Each Comment

For each comment:
- Read the referenced file and line range with `Read`
- Search for related patterns with `Grep`
- Determine if the comment is valid, a false positive, or project-specific intent

**Common false positives in this project:**
- Null checks where lazy init already handles it (e.g., `getBillFinanceDetails()`)
- Constructor changes — NEVER modify existing constructors (HMIS rule)
- "Fix" for intentional typos like `purcahseRate` — database compatibility
- Native SQL suggestions when JPQL works fine
- Bootstrap CSS classes when project uses PrimeFaces

### 4. Present Findings to User

For each comment, report:
- Comment summary
- Your assessment: **Valid** / **False positive** / **Discuss**
- Reasoning with file:line reference

Wait for user confirmation before making any changes.

### 5. Batch Valid Fixes

Apply all confirmed fixes. Group into one or a few logical commits:

```text
Fix CodeRabbit review comments (#<issue>)

Closes #<issue>

Co-Authored-By: Claude <noreply@anthropic.com>
```

### 6. Pre-Push Checklist

- `persistence.xml` uses `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` — not hardcoded JNDI names
- No credentials or `.env` files staged
- JSF-only changes do not need compilation

### 7. Push

```bash
git push
```

Verify CI passes before proceeding.

### 8. Reply to Existing Reviewer Threads

> **⚠️ Cardinal rules — read before posting anything:**
>
> 1. **Do NOT create new top-level inline comments** on the PR. Each one becomes a thread the author must manually resolve. This is noise.
> 2. **Reply only after fixing** (or explicitly dismissing). Bots cannot fix anything — describe action taken, not requested.
>    - Valid + fixed → "Fixed in `<commit-sha>`: `<what was changed>`"
>    - Dismissed → "Dismissed because: `<specific reasoning>`"
> 3. **Replies go UNDER existing reviewer threads only** (`/replies` endpoint). Never as new top-level comments.
> 4. **Self-review items go in the commit message** (or PR description if deferred). Never as new inline comments.
> 5. **ONE re-review request at the end**, not per item.
>
> Order: **discuss → fix → push → reply to existing threads → one re-review request**.

For each existing reviewer comment, post a threaded reply under the parent. The four endpoints are easy to confuse:

| Want | Endpoint | Use? |
|---|---|---|
| **Reply under existing reviewer comment (threaded)** | `POST /pulls/{pr}/comments/{id}/replies` | **YES — this is the only legitimate reply form** |
| New standalone inline comment on a line | `POST /pulls/{pr}/comments` | NO — creates a fresh thread the author must manually resolve |
| Bundled review | `POST /pulls/{pr}/reviews` | NO — grouping wrapper, doesn't match how bots post |
| General PR comment (only genuine PR-wide notes) | `POST /issues/{n}/comments` | Sparingly — only for actual PR-wide announcements |

For self-review items you spot on your own PR — **don't post a new inline comment**. Fix in the next commit and reference the file:line in the commit message:

```text
refactor(cashier): address self-review items

- PaymentSettlementController.java:117 — add comment on shallow snapshot
- settle_non_cash.xhtml:203 — use lastSettlementBill.institution.name
```

Find existing comment IDs to reply to:
```bash
gh api "repos/hmislk/hmis/pulls/<PR>/comments" \
  --jq '.[] | "\(.id) \(.user.login) \(.path):\(.line // .original_line)"'
```

Post a threaded reply:
```bash
gh api -X POST "repos/hmislk/hmis/pulls/<PR>/comments/<COMMENT_ID>/replies" \
  -f body="Fixed in <commit-sha>: <what was changed>."
```

Do NOT resolve other reviewers' conversations. CodeRabbit auto-resolves on detection.

Full API recipes and cleanup commands: see [Posting PR Comments via gh CLI](../../../developer_docs/git/pr-review-workflow.md#posting-pr-comments-via-gh-cli).

### 9. Re-Request Review

Click "Re-request review" on the PR page after pushing. Do not rely on reviewers noticing the new push.

### 10. Post-Merge Cleanup

After merge:
```bash
git branch -d <branch-name>
```

Also confirm "Delete branch" was checked in the GitHub merge dialog.

## Reference

Full workflow documentation: `developer_docs/git/pr-review-workflow.md`
