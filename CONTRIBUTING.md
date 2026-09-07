# Contributing to HMIS

Thank you for your interest in contributing to the Hospital Management Information System (HMIS). This document provides comprehensive guidelines for safe contributions to this production system.

## Important Safety Guidelines

### 🚨 Production System Warning
This is a **live production system** serving 40+ healthcare institutions. All changes must be:
- Thoroughly tested
- Non-destructive
- Backward compatible
- Approved by maintainers

### Before Making Changes
1. **Always create a backup** of any files you plan to modify
2. **Test in development environment** first
3. **Follow the safe improvement plans** in the documentation
4. **Never modify database schema** without extensive planning

## Repository Setup for Contributors

### Prerequisites
- Git installed on your system
- GitHub account
- Java 8+ and Maven for building (see INSTALL_GUIDE.md)

### Step 1: Fork the Repository
1. Navigate to https://github.com/hmislk/hmis
2. Click the "Fork" button in the top-right corner
3. Select your GitHub account as the destination
4. Wait for the fork to be created at `https://github.com/YOUR_USERNAME/hmis`

### Step 2: Clone and Configure Your Fork
```bash
# Clone your fork (replace YOUR_USERNAME with your GitHub username)
git clone https://github.com/YOUR_USERNAME/hmis.git
cd hmis

# Add the main repository as upstream
git remote add upstream https://github.com/hmislk/hmis.git

# Verify your remote configuration
git remote -v
# Should show:
# origin    https://github.com/YOUR_USERNAME/hmis.git (fetch)
# origin    https://github.com/YOUR_USERNAME/hmis.git (push)
# upstream  https://github.com/hmislk/hmis.git (fetch)
# upstream  https://github.com/hmislk/hmis.git (push)
```

### Step 3: Set Up Development Branch Tracking
```bash
# Fetch all branches from upstream
git fetch upstream

# Switch to development branch (the main development branch)
git checkout development

# Set up tracking to upstream development
git branch --set-upstream-to=upstream/development development

# Sync with latest upstream changes
git merge upstream/development

# Push synced changes to your fork
git push origin development
```

## Development Workflow

### Creating a Feature Branch
```bash
# Always start from the latest upstream development
git fetch upstream
git checkout development
git merge upstream/development

# Create a feature branch (use descriptive names)
git checkout -b feature/your-improvement-name
# or for issue-based work:
git checkout -b 12345-brief-description-of-issue
```

### Making Changes
- Follow the guidelines in existing documentation
- Use the safe-dependency-update.bat script for dependency updates
- Add tests for any new functionality
- Document your changes thoroughly

### Committing Changes
```bash
# Stage your changes
git add .

# Commit with proper message format
git commit -m "Brief description of changes

Detailed explanation of:
- What was changed
- Why it was changed
- How it was tested
- Any risks or considerations

Closes #issue_number (if applicable)

Signed-off-by: Your Name <your.email@example.com>"
```

### Submitting Pull Requests
```bash
# Push your feature branch to your fork
git push origin feature/your-improvement-name

# Then create a pull request via GitHub web interface:
# 1. Go to your fork on GitHub
# 2. Click "Compare & pull request"
# 3. Set target: hmislk/hmis:development ← your-fork:feature-branch
# 4. Fill out the PR template with detailed description
```

## Safe Contribution Areas

### ✅ Safe (Low Risk)
- Documentation improvements
- Adding utility methods (without modifying existing ones)
- Adding comments and JavaDoc
- Creating test cases
- Security analysis and recommendations
- Performance analysis

### ⚠️ Moderate Risk (Requires Testing)
- Dependency updates (follow DEPENDENCY_UPDATE_PLAN.md)
- Adding validation to existing methods
- Improving error handling
- Adding logging

### ❌ High Risk (Avoid Without Approval)
- Modifying core business logic
- Changing database queries
- Removing or modifying existing methods
- Changing configuration files
- Refactoring large classes

## Troubleshooting Repository Setup

### Common Issues and Solutions

#### Issue: "Permission denied" when pushing to origin
**Cause**: Your local repository is pointing to the main repository instead of your fork.
**Solution**:
```bash
# Check your current remote configuration
git remote -v

# If origin points to hmislk/hmis, reconfigure it:
git remote set-url origin https://github.com/YOUR_USERNAME/hmis.git
git remote add upstream https://github.com/hmislk/hmis.git
```

#### Issue: "Your branch is behind 'origin/development' by X commits"
**Cause**: Your fork is not synchronized with the latest upstream changes.
**Solution**:
```bash
# Sync your fork with upstream
git fetch upstream
git checkout development
git merge upstream/development
git push origin development
```

#### Issue: "fatal: refusing to merge unrelated histories"
**Cause**: Your local repository and fork have diverged significantly.
**Solution**:
```bash
# Force sync (use with caution)
git fetch upstream
git checkout development
git reset --hard upstream/development
git push origin development --force
```

#### Issue: Cannot create pull request
**Cause**: Either no fork exists or branches are not properly configured.
**Solution**:
1. Ensure you have forked the repository on GitHub
2. Verify your remote configuration with `git remote -v`
3. Make sure you're pushing to your fork: `git push origin feature-branch-name`

### Verifying Your Setup
Run these commands to verify your repository is properly configured:
```bash
# Check remote configuration
git remote -v

# Check current branch and tracking
git branch -vv

# Check if you're up to date with upstream
git fetch upstream
git status
```

## Code Standards

### Java Code Style
- Follow existing code formatting
- Add JavaDoc for public methods
- Use meaningful variable names
- Handle exceptions appropriately
- Avoid hardcoded values

### SQL Guidelines
- **Always use parameterized queries**
- Never use string concatenation for SQL
- Test queries with various inputs
- Consider performance impact

### Security Guidelines
- Validate all inputs
- Use parameterized queries
- Don't log sensitive information
- Follow OWASP guidelines
- Review SECURITY_ANALYSIS.md (if available)

## Testing Requirements

### Unit Tests
- Test new methods thoroughly
- Mock external dependencies
- Test edge cases and error conditions
- Maintain or improve code coverage

### Integration Tests
- Test database interactions
- Test web service endpoints
- Verify business workflows
- Test with realistic data volumes

### Manual Testing
- Test in browser (all supported browsers)
- Test key user workflows
- Verify reports generate correctly
- Test with different user roles

## Pull Request Guidelines

### Before Submitting
- Ensure your branch is up to date with upstream/development
- Run all tests locally
- Test your changes thoroughly
- Update documentation if needed

### PR Description Should Include
- Clear description of changes made
- Reason for the changes
- Testing performed
- Any breaking changes
- Screenshots (for UI changes)
- Related issue numbers

### Review Process
- PRs are reviewed by core maintainers
- Address feedback promptly
- Be prepared to make revisions
- Maintain professional communication

## Getting Help

### Resources
- Read existing documentation
- Check issue tracker for similar problems
- Review code comments and JavaDoc
- Consult project documentation files

### Contact
- Create GitHub issues for questions
- Tag maintainers for urgent issues
- Use discussions for general questions

## Recognition

Contributors will be acknowledged in:
- Project documentation
- Release notes
- Commit history

Thank you for helping improve HMIS while maintaining system stability and security!
