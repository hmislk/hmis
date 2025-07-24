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

## GitHub Project Board Integration

### Project Information
- **Project**: "CareCode: HMIS Board" (Project #11)
- **Organization**: hmislk
- **URL**: https://github.com/orgs/hmislk/projects/11

### Required Authentication
Ensure GitHub CLI has project scope:
```bash
gh auth refresh --hostname github.com -s project
```

### Project Status Workflow
When working on issues, update their status in the project board:

#### 1. Starting Development
When beginning work on an issue:
```bash
# Add issue to project (if not already added)
gh project item-add 11 --owner hmislk --url https://github.com/hmislk/hmis/issues/ISSUE_NUMBER

# Move to "In Progress" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 47fc9ee4
```

#### 2. After Creating PR
When a pull request is created:
```bash
# Move to "Reviewing & Merging" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id a087e083
```

#### 3. After Merge/Completion
When the issue is fully resolved:
```bash
# Move to "Done" status
gh project item-edit --project-id PVT_kwDOAHw-zs4ApMln --id PROJECT_ITEM_ID --field-id PVTSSF_lADOAHw-zs4ApMlnzggpN3k --single-select-option-id 1bab9178
```

### Status Option IDs
- **Backlog**: `8a5edcf1`
- **Current Sprint**: `1cbc1930`
- **To Do**: `fbaa9d0f`
- **In Progress**: `47fc9ee4`
- **Reviewing & Merging**: `a087e083`
- **Dev Completed**: `85d4d4c9`
- **Ready For Testing**: `f9564514`
- **Testing In Progress**: `63aa4183`
- **Done**: `1bab9178`

### Project Constants
- **Project ID**: `PVT_kwDOAHw-zs4ApMln`
- **Status Field ID**: `PVTSSF_lADOAHw-zs4ApMlnzggpN3k`

### Finding Project Item ID
To get the project item ID for an issue:
```bash
gh api graphql -f query='
{
  search(query: "repo:hmislk/hmis is:issue ISSUE_NUMBER", type: ISSUE, first: 1) {
    nodes {
      ... on Issue {
        number
        projectItems(first: 10) {
          nodes {
            id
            project {
              number
            }
          }
        }
      }
    }
  }
}'
```

### Automatic Workflow
Claude should automatically:
1. Add issues to the project when starting development
2. Move issues to "In Progress" when development begins
3. Move issues to "Reviewing & Merging" when PRs are created
4. Include comprehensive test plans in PR descriptions for QA team

## Maven Testing Commands

### Environment-Specific Maven Locations
Different development machines have Maven installed in different locations.

**Machine Detection**: Use `hostname` and `whoami` commands to identify the current machine.

#### Known Configurations:
- **cclap** (Computer: `CARECODE-LAP`, User: `buddhika`):
  - Maven: `C:\Program Files\NetBeans-20\netbeans\java\maven\bin\mvn.cmd`
  - Payara Server: `C:\Users\buddhika\Payara_Server` (Domain 1)
- **hiulap** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_
- **hiud** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_ 
- **ccd** (Computer: `_[TBD]_`, User: `_[TBD]_`): _[To be documented]_

### Testing Commands to Try (in order of preference)
When running tests, try these commands in order until one works:

1. **Standard Maven** (if in PATH):
   ```bash
   mvn test
   ```

2. **NetBeans Bundled Maven** (cclap):
   ```bash
   "C:\Program Files\NetBeans-20\netbeans\java\maven\bin\mvn.cmd" test
   ```

3. **Maven Wrapper** (if available):
   ```bash
   ./mvnw test        # Linux/Mac
   ./mvnw.cmd test    # Windows
   ```

4. **Specific Test Classes**:
   ```shell
   mvn test -Dtest="*BigDecimal*Test"
   ```

### Note for Claude
**PREFERRED APPROACH**: Use the auto-detection script:
1. **First try**: `./detect-maven.sh test` (automatically detects machine and uses correct Maven)
2. **If script fails**: Fall back to manual detection:
   - Run `hostname` and `whoami` to identify machine
   - Try standard `mvn test`
   - If Maven not found, use machine-specific path based on hostname
   - For `CARECODE-LAP`/`carecode-lap` (cclap): Use `"C:\Program Files\NetBeans-20\netbeans\java\maven\bin\mvn.cmd" test`

**The detect-maven.sh script handles all this automatically and should be the first choice.**

---
This behavior should persist across all Claude Code sessions for this project.