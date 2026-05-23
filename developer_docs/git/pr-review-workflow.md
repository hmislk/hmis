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

> **⚠️ Reply only after you have fixed (or explicitly dismissed) the comment.**
>
> Bots (CodeRabbit, Codex) **cannot fix anything** — they only identify issues. Replying "please resolve" / "please address" / "please add the null guard" treats the bot as if it were an assignee, which it is not. Every reply you post under a reviewer's comment must describe action **taken**:
>
> - **Valid + fixed** → "Fixed in `<commit-sha>`: `<what was changed>`"
> - **Dismissed** → "Dismissed because: `<specific reasoning>`"
>
> The sequence is always: **discuss → fix → push → reply → re-request review**. Never reply before pushing the fix. Same rule applies to your own standalone review items — once filed, *you* (or the PR author) must do the work, not the bot.

Four different ways exist to post comments on a PR. Picking the wrong one is the most common mistake. Each behaves differently in the GitHub UI:

| What you want | Endpoint | gh / API command | Visible as |
|---|---|---|---|
| Reply UNDER an existing inline comment (threaded) | `POST /pulls/{pr}/comments/{id}/replies` | `gh api -X POST repos/<o>/<r>/pulls/<pr>/comments/<id>/replies -f body="..."` | Nested reply in the thread |
| Standalone inline comment on a specific line | `POST /pulls/{pr}/comments` | `gh api -X POST repos/<o>/<r>/pulls/<pr>/comments -f commit_id=<sha> -f path=<file> -F line=<n> -f side=RIGHT -f body="..."` | Top-level inline comment (same shape as CodeRabbit/Codex) |
| Multiple inline comments grouped under a "review" | `POST /pulls/{pr}/reviews` with `comments[]` | `gh pr review` or `gh api -X POST repos/<o>/<r>/pulls/<pr>/reviews --input <json>` | All grouped under a single review wrapper — NOT what bots do |
| General PR-level comment (conversation tab) | `POST /issues/{n}/comments` | `gh pr comment <pr> --body "..."` | NOT tied to a line; lands in the Conversation tab |

### When to use which

- **Replying to a bot comment** (CodeRabbit / Codex / human reviewer) → always use the `/replies` endpoint. The reply stays in the parent's thread and the bot can detect the conversation correctly.
- **Filing a new review item on a specific line** → use `POST /pulls/{pr}/comments` directly. This produces an individual standalone inline comment, the same shape as CodeRabbit/Codex post. Each one is its own resolvable thread.
- **A PR-wide observation** (e.g. "this PR needs a Linear ticket") → `gh pr comment` is acceptable, but prefer inline if it can be tied to a line.
- **Avoid** `POST /pulls/{pr}/reviews` with bundled `comments[]` unless you're submitting a formal review pass — it groups all comments under one "review" wrapper, which makes them harder to resolve individually and does not match how bots post.

### Practical recipes

**Standalone inline comment on a specific line:**
```bash
SHA=$(git rev-parse HEAD)
gh api -X POST "repos/hmislk/hmis/pulls/<PR>/comments" \
  -f commit_id="$SHA" \
  -f path="src/main/java/.../Foo.java" \
  -F line=42 \
  -f side="RIGHT" \
  -f body="Body text here."
```

**Threaded reply under an existing comment (id from `/comments` list):**
```bash
gh api -X POST "repos/hmislk/hmis/pulls/<PR>/comments/<COMMENT_ID>/replies" \
  -f body="Please resolve — <action>."
```

**List existing inline comments with IDs (find a parent to reply to):**
```bash
gh api "repos/hmislk/hmis/pulls/<PR>/comments" \
  --jq '.[] | "\(.id) \(.user.login) \(.path):\(.line // .original_line)"'
```

**Cannot do** `event: REQUEST_CHANGES` on your own PR. Use `event: COMMENT` instead, or post standalone comments without a review wrapper.

### Cleanup

- Delete an inline comment: `gh api -X DELETE repos/<o>/<r>/pulls/comments/<id>`
- Delete a general PR comment: `gh api -X DELETE repos/<o>/<r>/issues/comments/<id>`
- A review wrapper itself can be dismissed but its comments persist — delete each comment individually if you need to remove them.

## Notes

- All PRs target `development`, never `master`
- "Re-request review" is distinct from just pushing — always click it
- Replying to each comment individually maintains a clean audit trail
- The `/review-pr` skill automates the investigation and fix steps of this workflow
