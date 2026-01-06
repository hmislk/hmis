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
