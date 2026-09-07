# GitHub CLI (gh) Usage Guide for HMIS Project

## Overview
This guide covers using GitHub CLI (`gh`) for streamlined issue management, pull request creation, and project board integration in the HMIS project.

## Installation & Setup

### Install GitHub CLI
```bash
# Windows (using winget)
winget install --id GitHub.cli

# Windows (using Chocolatey)
choco install gh

# macOS
brew install gh

# Linux (Ubuntu/Debian)
sudo apt install gh
```

### Authentication
```bash
# Authenticate with GitHub
gh auth login

# Check authentication status
gh auth status
```

## Core Commands

### Issues Management

#### Create Issues
```bash
# Basic issue creation
gh issue create --title "Bug: Login validation error" --body "Description of the issue"

# Interactive issue creation
gh issue create

# Create issue with labels and assignees
gh issue create --title "Feature: Add patient search" --label "enhancement,frontend" --assignee username

# Create issue from template
gh issue create --template bug_report.md
```

#### List and View Issues
```bash
# List all open issues
gh issue list

# List issues with filters
gh issue list --state closed --limit 10
gh issue list --author username
gh issue list --assignee @me
gh issue list --label bug

# View specific issue
gh issue view 12345
gh issue view 12345 --web  # Open in browser
```

#### Update Issues
```bash
# Close issue
gh issue close 12345

# Reopen issue
gh issue reopen 12345

# Add comment to issue
gh issue comment 12345 --body "Updated status information"

# Edit issue
gh issue edit 12345 --title "New title" --add-label priority-high
```

### Pull Requests Management

#### Create Pull Requests
```bash
# Create PR from current branch
gh pr create --title "Implement patient search feature" --body "Closes #12345"

# Interactive PR creation
gh pr create

# Create draft PR
gh pr create --draft --title "WIP: Patient search implementation"

# Create PR with reviewers and assignees
gh pr create --reviewer username1,username2 --assignee @me
```

#### List and View PRs
```bash
# List open PRs
gh pr list

# List PRs with filters
gh pr list --author username
gh pr list --state merged --limit 5
gh pr list --assignee @me

# View PR details
gh pr view 123
gh pr view 123 --web

# Check PR status
gh pr status
```

#### Manage PRs
```bash
# Merge PR (after approval)
gh pr merge 123 --merge     # Create merge commit
gh pr merge 123 --squash    # Squash and merge
gh pr merge 123 --rebase    # Rebase and merge

# Close PR without merging
gh pr close 123

# Reopen PR
gh pr reopen 123

# Add review
gh pr review 123 --approve
gh pr review 123 --request-changes --body "Please fix the validation logic"

# Checkout PR locally
gh pr checkout 123
```

### Project Board Integration

#### Link Issues to Projects
```bash
# Add issue to project (requires project ID)
gh project item-add PROJECT_ID --url https://github.com/hmislk/hmis/issues/12345

# List project items
gh project list --owner hmislk
```

### Repository Management

#### Workflow Commands
```bash
# View workflow runs
gh run list

# View specific workflow run
gh run view 123456

# Rerun failed jobs
gh run rerun 123456

# Cancel workflow run
gh run cancel 123456
```

#### Release Management
```bash
# Create release
gh release create v1.2.0 --title "Version 1.2.0" --notes "Release notes here"

# List releases
gh release list

# View release
gh release view v1.2.0
```

## HMIS Project Specific Workflows

### Issue Creation with Auto-linking
```bash
# Create feature issue that auto-links to project board
gh issue create \
  --title "Feature: Implement fast sale for cashier" \
  --body "## Description
Implement fast sale functionality for cashier module

## Acceptance Criteria
- [ ] Fast sale UI component
- [ ] Backend API integration
- [ ] Validation logic
- [ ] Error handling

## Related Issues
Related to #14268" \
  --label "enhancement,cashier" \
  --project "HMIS Development Board"
```

### Branch-to-PR Workflow
```bash
# 1. Create issue-based branch
git checkout -b 14284-direct-purchase-cancel-bill

# 2. Make changes and commit
git add .
git commit -m "Implement direct purchase cancel functionality

Closes #14284"

# 3. Push branch
git push -u origin 14284-direct-purchase-cancel-bill

# 4. Create PR with auto-close
gh pr create \
  --title "Direct purchase cancel bill #14284" \
  --body "## Summary
Implements cancel functionality for direct purchase bills

## Changes
- Add cancel button to direct purchase interface
- Implement backend cancel logic
- Add validation for cancellation rules
- Update UI state management

## Testing
- [ ] Test cancel functionality
- [ ] Verify state updates
- [ ] Check validation rules

Closes #14284" \
  --reviewer team-lead \
  --assignee @me
```

### Quick Status Updates
```bash
# Check your assigned issues
gh issue list --assignee @me --json number,title,state | jq '.[] | "\(.number): \(.title) (\(.state))"'

# Check PRs awaiting review
gh pr list --search "review-requested:@me"

# View recent activity
gh pr list --limit 5 --json number,title,updatedAt | jq '.[] | "\(.number): \(.title) (updated: \(.updatedAt))"'
```

## Configuration & Aliases

### Set Default Repository
```bash
# Set default repo for commands
gh repo set-default hmislk/hmis
```

### Useful Aliases
Add to your shell profile (.bashrc, .zshrc, etc.):

```bash
# Quick aliases for common operations
alias ghic='gh issue create'
alias ghil='gh issue list'
alias ghiv='gh issue view'
alias ghpc='gh pr create'
alias ghpl='gh pr list'
alias ghpv='gh pr view'
alias ghps='gh pr status'

# HMIS specific aliases
alias hmis-issues='gh issue list --repo hmislk/hmis'
alias hmis-prs='gh pr list --repo hmislk/hmis'
alias hmis-my-issues='gh issue list --repo hmislk/hmis --assignee @me'
```

## Best Practices for HMIS Project

### Issue Management
1. **Use descriptive titles**: Include module/feature context
2. **Add appropriate labels**: Use project-specific labels (frontend, backend, bug, enhancement)
3. **Link related issues**: Reference other issues in descriptions
4. **Update project board**: Ensure issues move through proper workflow states

### Pull Request Workflow
1. **Follow naming convention**: Use issue numbers in branch/PR titles
2. **Include closing keywords**: Always use "Closes #issueNumber" in PR description
3. **Request appropriate reviewers**: Tag relevant team members
4. **Update documentation**: Link to relevant documentation changes

### Commit Integration
```bash
# Example commit with proper formatting
git commit -m "Fix OPD income report DTO implementation

- Add proper null handling for date fields
- Implement pagination for large datasets
- Fix boolean field mapping issues

Closes #14297"
```

## Troubleshooting

### Common Issues
```bash
# Refresh authentication if commands fail
gh auth refresh

# Check repository permissions
gh repo view --json permissions

# Verify current repository context
gh repo view

# Debug API calls
gh api user --verbose
```

### Error Resolution
- **Permission denied**: Check repository access and authentication
- **Resource not found**: Verify issue/PR numbers and repository context
- **Rate limiting**: Wait or authenticate to increase limits

## Practical Examples

### Example 1: Creating Configuration Issue
```bash
# Create an issue for configuration file creation
gh issue create \
  --title "Create gemini.md for Gemini agent configuration" \
  --body "## Description
This commit creates the gemini.md file by combining the contents of CLAUDE.md and AGENTS.md. This file will provide the necessary configuration and guidelines for the Gemini agent.

## Tasks
- [ ] Combine CLAUDE.md and AGENTS.md contents
- [ ] Create gemini.md file with proper formatting
- [ ] Update cross-references in other configuration files
- [ ] Test configuration with Gemini agent

## Acceptance Criteria
- gemini.md file exists with complete configuration
- All guidelines from both source files are included
- File follows established documentation patterns
- Cross-references are updated appropriately

## Priority
Medium - Configuration standardization" \
  --label "documentation,configuration" \
  --assignee @me

# After creating, get the issue number for branch creation
gh issue list --limit 1 --json number,title
```

### Example 2: Full Development Workflow
```bash
# 1. Create issue (if not already created)
ISSUE_NUM=$(gh issue create \
  --title "Fix OPD income report boolean handling" \
  --body "Boolean fields in OPD income report are not displaying correctly" \
  --label "bug,opd" \
  --json number --jq '.number')

# 2. Create branch
git checkout -b ${ISSUE_NUM}-fix-opd-income-boolean-handling

# 3. Make changes, commit with proper message
git add .
git commit -m "Fix boolean field handling in OPD income report

- Add proper null checks for boolean fields
- Update DTO constructor to handle boolean conversion
- Fix display logic in report template

Closes #${ISSUE_NUM}"

# 4. Push branch
git push -u origin ${ISSUE_NUM}-fix-opd-income-boolean-handling

# 5. Create PR
gh pr create \
  --title "Fix OPD income report boolean handling #${ISSUE_NUM}" \
  --body "## Summary
Fixes boolean field display issues in OPD income reports

## Changes
- Updated DTO boolean field handling
- Added null safety checks  
- Fixed template display logic

## Testing
- [ ] Test with null boolean values
- [ ] Verify report generation
- [ ] Check display formatting

Closes #${ISSUE_NUM}" \
  --reviewer team-lead

# 6. Check PR status
gh pr status
```

### Example 3: Quick Issue Management
```bash
# List my open issues
gh issue list --assignee @me --state open

# Add comment to issue
gh issue comment 12345 --body "Working on implementation, will have PR ready by EOD"

# Close completed issue
gh issue close 12345 --comment "Completed in PR #456"

# Reopen if needed
gh issue reopen 12345 --comment "Reopening due to regression found in testing"
```

## Integration with Development Workflow

This GitHub CLI usage integrates with:
- **Persistence Configuration**: [Persistence Workflow](../persistence/persistence-workflow.md)
- **Commit Conventions**: [Git Conventions](../git/commit-conventions.md)
- **Project Board Management**: [Project Board Integration](project-board-integration.md)
- **Testing Workflow**: [Maven Commands](../testing/maven-commands.md)

---
For more advanced usage, see the [official GitHub CLI documentation](https://cli.github.com/manual/).