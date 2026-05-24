---
name: review-pr
description: Investigate and resolve GitHub pull request review comments for HMIS. Use when the user wants Codex to read PR review threads, assess CodeRabbit or Codex feedback, discuss valid fixes, batch and apply changes, reply to each comment, and re-request review.
---

# Review PR

## Overview

Use this skill when working a PR review loop on HMIS: collect review comments, verify them against the codebase, discuss unclear items with the user, apply confirmed fixes, and close the loop on GitHub.

## Workflow

1. Fetch the branch and inspect the PR comments.
   - `git fetch origin`
   - `gh pr checkout <pr-number-or-url>`
   - `gh api repos/hmislk/hmis/pulls/<pr-number>/comments`

2. Group comments by file and behavior.
   - Read the touched code and nearby context with `rg`, `Get-Content`, or `git show`.
   - Separate valid findings from false positives and project conventions.

3. Discuss before changing anything ambiguous.
   - Ask the user when a reviewer suggestion could change behavior, contracts, or workflow.
   - Prefer confirmation over guessing when automated review output is uncertain.

4. Apply confirmed fixes in a tight batch.
   - Keep unrelated cleanup out of the review-fix commit.
   - Preserve HMIS rules: no constructor signature edits, no mock data, JPQL first, `findLongByJpql` for count queries.

5. Check the repository-specific hazards before pushing.
   - Verify `persistence.xml` placeholders remain `${JDBC_DATASOURCE}` and `${JDBC_AUDIT_DATASOURCE}`.
   - For JSF/XHTML fixes, keep standard `<!DOCTYPE html>` and use `h:outputText` for static ERP labels.

6. Push, reply, and re-request review — by the cardinal rules below.

## Cardinal Rules for Posting on a PR

1. **Do NOT create new top-level inline comments** on the PR. Each one becomes a thread the author must manually resolve. Self-review noise is the most common offender.
2. **Reply only after fixing** (or explicitly dismissing). Bots cannot fix anything — describe action taken, not requested.
   - Valid + fixed → "Fixed in `<commit-sha>`: `<what was changed>`"
   - Dismissed → "Dismissed because: `<specific reasoning>`"
   - **⚠️ "Please resolve" wording on a CodeRabbit thread triggers CodeRabbit Chat**, which opens a NEW PR attempting the fix against a stale snapshot. These auto-PRs have shipped unsafe diffs in the past (PR #20979). If accidentally triggered: close the PR, delete the `coderabbitai/chat/<sha>` branch.
3. **Replies go UNDER existing reviewer threads only** — use `POST /pulls/{pr}/comments/{id}/replies`. Never as new top-level comments.
4. **Self-review items go in the commit message** (or PR description if deferred). Never as new inline comments. Reference file:line in the commit message so reviewers can find the change in the diff.
5. **ONE re-review request at the end** — click "Re-request review" once after all fixes are pushed and all existing threads have replies. Not per item.

Order: **discuss → fix → push → reply to existing threads → one re-review request**.

## Endpoint Cheat Sheet

| Want | Endpoint | Use? |
|---|---|---|
| Reply under existing reviewer comment | `POST /pulls/{pr}/comments/{id}/replies` | YES |
| New standalone inline comment | `POST /pulls/{pr}/comments` | NO — creates a fresh thread |
| Bundled review wrapper | `POST /pulls/{pr}/reviews` | NO |
| General PR comment | `POST /issues/{n}/comments` | Sparingly, only for actual PR-wide notes |

```bash
# List existing comments to find a parent ID
gh api "repos/hmislk/hmis/pulls/<PR>/comments" \
  --jq '.[] | "\(.id) \(.user.login) \(.path):\(.line // .original_line)"'

# Post a threaded reply (the only legitimate reply form)
gh api -X POST "repos/hmislk/hmis/pulls/<PR>/comments/<COMMENT_ID>/replies" \
  -f body="Fixed in <commit-sha>: <what was changed>."
```

## Common Review Patterns

- CodeRabbit suggestions often need verification against the actual entity or controller method names.
- Review comments about null checks, constructor changes, or typos may be false positives in this codebase.
- When a reviewer points to session-scoped state, re-check the persisted entity at submit time before mutation.

## Reference

- `developer_docs/git/pr-review-workflow.md`
- `AGENTS.md`
