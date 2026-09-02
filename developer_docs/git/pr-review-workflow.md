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
→ push
→ verify CI is green
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

### 4a. Code Review After Applying CodeRabbit Fixes — MANDATORY

**After applying any CodeRabbit (or other automated reviewer) suggestion, always review the changed code with the `java-health-code-reviewer` agent before pushing.** Automated tools frequently generate suggestions with wrong method names, incorrect types, or API calls that do not exist in this codebase, causing compilation errors.

Specifically verify:
- All method names exist on the actual entity class (e.g., `isCompleted()` not `getCompleted()` for primitive `boolean` fields in JPA entities)
- All referenced fields/variables are in scope
- Generated `diff` hunks integrate cleanly with surrounding code (no missing lines, no off-by-one context)
- The fix does not introduce a regression (e.g., null-safety, logic inversion)

> **Why:** CodeRabbit has broken builds multiple times in this project by generating method calls that don't exist (`getCompleted()` on a `boolean` field, wrong constructor signatures, etc.). Never apply a suggestion blindly.

### 5. Pre-Push Checklist

- **persistence.xml** — must be reverted to CI/CD placeholders before pushing. See [Start Issue Workflow § Persistence.xml Swap](start-issue-workflow.md#persistencexml-swap) and [Persistence Configuration Guide](../deployment/persistence-verification.md)
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

See [§ Posting PR Comments via gh CLI](#posting-pr-comments-via-gh-cli) below for the correct API endpoints — using the wrong one bundles comments under a review wrapper or posts them as general PR comments that aren't tied to a line.

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

## Posting PR Comments via gh CLI

> **⚠️ THE CARDINAL RULES**
>
> 1. **Do NOT create new top-level inline comments** on a PR. Every new top-level inline comment becomes its own discussion thread that the PR author must manually resolve. Filing 7 self-review items as new top-level inline comments creates 7 chores for the author. This is noise, not review.
>
> 2. **Reply only after you have fixed (or explicitly dismissed) the comment.** Bots (CodeRabbit, Codex) **cannot fix anything** — they only identify issues. Replying "please resolve" / "please address" / "please add the null guard" treats the bot as if it were an assignee, which it is not. Reply content must describe action **taken**, not action **requested**:
>    - **Valid + fixed** → "Fixed in `<commit-sha>`: `<what was changed>`"
>    - **Dismissed** → "Dismissed because: `<specific reasoning>`"
>
>    **⚠️ CodeRabbit Chat consequence:** wording like "please resolve", "please address", "please add X" on a CodeRabbit thread is interpreted as a Chat command. CodeRabbit will then **open a NEW PR** (named `📝 CodeRabbit Chat: Implement requested code changes` on branch `coderabbitai/chat/<sha>`) attempting the fix. That auto-PR is generated against a snapshot of the diff that may already be stale, often duplicates or conflicts with work the author has done, and has produced unsafe diffs (e.g. deleting a `params.put(...)` line on PR #20979). If you accidentally trigger one: **close it without merging** and delete the `coderabbitai/chat/<sha>` branch.
>
> 3. **Replies go UNDER existing reviewer threads** (`POST /pulls/{pr}/comments/{id}/replies`) — never as new top-level comments. The reply stays in the parent's thread and the bot/reviewer can resolve their own thread on detection.
>
> 4. **Self-review items go in the commit message and/or PR description**, NOT as new top-level inline comments. If you spot something while reviewing your own PR, fix it (or document the decision to defer) in the commit message and update the PR description checklist. Don't sprinkle review items as new inline comments — that creates manual-resolve noise.
>
> 5. **ONE re-review request at the end**, not one per item. After all fixes are pushed and all existing reviewer threads have been replied to, click **"Re-request review"** (or post a single summary comment if no human reviewer is assigned). Do not nag reviewers per-item.
>
> The sequence is always: **discuss → fix → push → reply to existing threads → one re-review request**. Never reply before pushing the fix. Never file new top-level inline comments as a substitute for a commit-message note.

Four different ways exist to post comments on a PR. Picking the wrong one is the most common mistake. Each behaves differently in the GitHub UI:

| What you want | Endpoint | gh / API command | Visible as |
|---|---|---|---|
| Reply UNDER an existing inline comment (threaded) | `POST /pulls/{pr}/comments/{id}/replies` | `gh api -X POST repos/<o>/<r>/pulls/<pr>/comments/<id>/replies -f body="..."` | Nested reply in the thread |
| Standalone inline comment on a specific line | `POST /pulls/{pr}/comments` | `gh api -X POST repos/<o>/<r>/pulls/<pr>/comments -f commit_id=<sha> -f path=<file> -F line=<n> -f side=RIGHT -f body="..."` | Top-level inline comment (same shape as CodeRabbit/Codex) |
| Multiple inline comments grouped under a "review" | `POST /pulls/{pr}/reviews` with `comments[]` | `gh pr review` or `gh api -X POST repos/<o>/<r>/pulls/<pr>/reviews --input <json>` | All grouped under a single review wrapper — NOT what bots do |
| General PR-level comment (conversation tab) | `POST /issues/{n}/comments` | `gh pr comment <pr> --body "..."` | NOT tied to a line; lands in the Conversation tab |

### When to use which

- **Replying to a bot/human reviewer comment** → use the `/replies` endpoint. The reply stays in the parent thread, the bot/reviewer can auto-resolve their own thread on detection, and the author does NOT have to manually resolve it. **This is the only acceptable place to post a reply on a PR review.**
- **A new review item from your own self-review** → do **NOT** post it as a new top-level inline comment. Put it in the **commit message** that fixes it (preferred) or note it in the **PR description** if it's being deferred. Creating new top-level threads forces the author to manually resolve each one, which is noise. This was the wrong move on PR #20977 and PR #20983 — both required cleanup.
- **A genuinely PR-wide announcement** (e.g. "merge blocked pending Linear ticket") → `gh pr comment` is acceptable for actual PR-wide observations only, and used sparingly.
- **Avoid** `POST /pulls/{pr}/reviews` with bundled `comments[]`. It groups comments under one "review" wrapper that doesn't match how bots post, and on your own PR you cannot even use `event: REQUEST_CHANGES`.

### How to handle self-review items (the right way)

If you spot a problem in your own PR after pushing:

1. **Fix it** in the next commit on the same branch.
2. **Reference the file:line in the commit message** so reviewers can find the change in the diff:
   ```text
   refactor(cashier): address self-review items
   
   - PaymentSettlementController.java:117 — add comment on shallow snapshot
   - settle_non_cash.xhtml:203 — use lastSettlementBill.institution.name
   - PaymentSettlementController.java:150 — drop redundant state reset
   ```
3. **Push.** Reviewers see the new commit in the PR diff. No new threads created. No manual resolves needed.
4. If you're deferring an item rather than fixing it, **add a line to the PR description** explaining the decision — do not file an inline comment.

### Practical recipes

**List existing inline comments with IDs (find a parent to reply to):**
```bash
gh api "repos/hmislk/hmis/pulls/<PR>/comments" \
  --jq '.[] | "\(.id) \(.user.login) \(.path):\(.line // .original_line)"'
```

**Threaded reply under an existing comment** (this is the only legitimate reply form):
```bash
gh api -X POST "repos/hmislk/hmis/pulls/<PR>/comments/<COMMENT_ID>/replies" \
  -f body="Fixed in <commit-sha>: <what was changed>."
```

> **Do not** post new top-level inline comments (`POST /pulls/{pr}/comments`) on your own PR — those create new threads that the author must manually resolve. Use a follow-up commit instead.

**Cannot do** `event: REQUEST_CHANGES` on your own PR. Use `event: COMMENT` if you must use the reviews endpoint, but prefer threaded replies to existing reviewer comments and a single follow-up commit for your own findings.

### Cleanup (when you've already made the mistake)

- Delete an inline comment: `gh api -X DELETE repos/<o>/<r>/pulls/comments/<id>`
- Delete a general PR comment: `gh api -X DELETE repos/<o>/<r>/issues/comments/<id>`
- A review wrapper itself can be dismissed but its comments persist — delete each comment individually if you need to remove them.

## Notes

- All PRs target `development`, never `master` — see [Commit Conventions § Feature Branches](commit-conventions.md#feature-branches)
- "Re-request review" is distinct from just pushing — always click it once, at the end
- Replying ONLY UNDER existing reviewer threads maintains a clean audit trail without creating new manual-resolve chores
- Self-review items go in commit messages, not as new inline comments
- The `/review-pr` skill automates the investigation and fix steps of this workflow
- The `/review-and-fix` skill goes further: it runs a fresh deep `code-review` on
  a PR, applies the fixes, verifies each one live (build → local redeploy →
  Playwright + DB), then calls `/review-pr` for the thread replies and drives CI
  to green. Use it on a PR that needs correcting rather than just triaging
  existing comments — e.g. one a `/merge-gate` run left `BLOCKED-REVIEW`. See
  [Review-and-Fix Skill Design](2026-09-02-review-and-fix-skill-design.md).
