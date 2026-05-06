# Git Commit Message Conventions

## Issue Closing Keywords
When pushing commits that resolve GitHub issues, include one of these keywords in the commit message:
- `Closes #issueNumber` - for general issue resolution
- `Fixes #issueNumber` - for bug fixes
- `Resolves #issueNumber` - alternative to closes

## Example Commit Messages
```
Add flexible persistence.xml configuration workflow

Closes #14011

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

## Auto-Close Behavior
When Claude pushes commits that complete an issue, automatically include the appropriate closing keyword in the commit message.

## Branch Naming Conventions

### Hotfix Branches
When creating hotfix branches targeting production branches (e.g., `ruhunu-prod`), the branch name **must** end with `-hotfix`. This is required for the PR to be mergeable.

**Format:** `<issue-number>-<description>-hotfix`

**Examples:**
- `19635-configurable-bill-serial-digits-hotfix`
- `19500-fix-grn-return-hotfix`

### Feature Branches
Feature branches should be based on `origin/development` (never `master`) and follow the pattern:

**Format:** `<issue-number>-<description>`

**Examples:**
- `19635-configurable-bill-number-serial-digits`
- `19500-fix-grn-return-approval`
