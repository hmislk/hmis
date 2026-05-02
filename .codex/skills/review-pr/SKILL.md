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

6. Push, reply, and re-request review.
   - Push only after the fix is ready.
   - Reply to each reviewer thread individually with what changed or why the comment was dismissed.
   - Ask for another review pass after the push.

## Common Review Patterns

- CodeRabbit suggestions often need verification against the actual entity or controller method names.
- Review comments about null checks, constructor changes, or typos may be false positives in this codebase.
- When a reviewer points to session-scoped state, re-check the persisted entity at submit time before mutation.

## Reference

- `developer_docs/git/pr-review-workflow.md`
- `AGENTS.md`
