# Claude Code Configuration for HMIS Project

## Persistence.xml Database Configuration Workflow

### IMPORTANT: Automatic Git Push Behavior

When asked to push changes to GitHub, Claude should AUTOMATICALLY:

1. **Before pushing:**
   - Change current JNDI names → `${JDBC_DATASOURCE}` in persistence.xml
   - Change current audit JNDI names → `${JDBC_AUDIT_DATASOURCE}` in persistence.xml

2. **Push changes** to GitHub

3. **After pushing:**
   - Revert `${JDBC_DATASOURCE}` → back to the original JNDI name that was there before
   - Revert `${JDBC_AUDIT_DATASOURCE}` → back to the original audit JNDI name that was there before

### File Location
`src/main/resources/META-INF/persistence.xml`

### Variable JNDI Names
The JNDI names change based on environment and will be manually updated:
- Examples: `jdbc/asiri`, `jdbc/ruhunu`, `jdbc/coop`, etc.
- Always preserve whatever the current local configuration is

### Security Note
**NEVER commit actual JNDI names to the repository - always use environment variables in pushed commits**

### Key Principle
Use the **last choice in history** - whatever JNDI names are currently in the file should be restored after pushing.

## Git Commit Message Conventions

### Issue Closing Keywords
When pushing commits that resolve GitHub issues, include one of these keywords in the commit message:
- `Closes #issueNumber` - for general issue resolution
- `Fixes #issueNumber` - for bug fixes
- `Resolves #issueNumber` - alternative to closes

### Example Commit Messages
```
Add flexible persistence.xml configuration workflow

Closes #14011

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>
```

### Auto-Close Behavior
When Claude pushes commits that complete an issue, automatically include the appropriate closing keyword in the commit message.

---
This behavior should persist across all Claude Code sessions for this project.