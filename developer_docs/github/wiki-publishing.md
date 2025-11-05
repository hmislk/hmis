# Wiki Publishing Guide

## Overview

This project maintains user documentation in the GitHub Wiki at https://github.com/hmislk/hmis/wiki. User documentation must be published to the wiki **immediately** after creation, not waiting for PR merge.

## Critical Rules

**🚨 IMMEDIATE PUBLICATION**: When creating user documentation, ALWAYS publish to GitHub Wiki immediately after creating the markdown file - don't wait for PR merge.

**📁 DIRECTORY**: Create documentation in `wiki-docs/` directory (e.g., `wiki-docs/Pharmacy/Feature-Name.md`)

**📝 GUIDELINES**: Follow the [Wiki Writing Guidelines](wiki-writing-guidelines.md)

## Publishing Workflow

### Step 1: Create Wiki Documentation

1. Create markdown files in `wiki-docs/` directory
   ```
   wiki-docs/
   ├── Pharmacy/
   │   ├── Feature-Name.md
   │   └── Other-Features.md
   ├── Lab/
   │   └── Lab-Features.md
   └── General/
       └── User-Guide.md
   ```

2. Follow [Wiki Writing Guidelines](wiki-writing-guidelines.md)
3. Write for end users (pharmacy staff, nurses, doctors, administrators)

### Step 2: Commit to Feature Branch

1. Add to git:
   ```bash
   git add wiki-docs/
   ```

2. Commit to current feature branch with proper message:
   ```bash
   git commit -m "Add [Feature Name] user documentation"
   ```

3. Push feature branch to GitHub:
   ```bash
   git push
   ```

### Step 3: Publish to GitHub Wiki (IMMEDIATE)

**Do this IMMEDIATELY after Step 2 - don't wait for PR merge**

#### Full Process

```bash
# Navigate to project root
cd /home/buddhika/development/rh

# Clone wiki repository (if not exists)
git clone https://github.com/hmislk/hmis.wiki.git hmis.wiki

# Copy documentation to wiki
cp -r wiki-docs/Pharmacy/* hmis.wiki/Pharmacy/
# OR for specific file:
# cp wiki-docs/Pharmacy/Your-Feature.md hmis.wiki/Pharmacy/

# Navigate to wiki repository
cd hmis.wiki

# Commit and push to wiki
git add .
git commit -m "Add [Feature Name] user documentation

[Brief description]

🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>"

git push origin master

# Return to main repository
cd ..
```

#### Quick Command Template

When publishing specific file documentation:

```bash
cd hmis.wiki
cp ../wiki-docs/Pharmacy/[Your-File].md Pharmacy/
git add Pharmacy/[Your-File].md
git commit -m "Add [Feature] documentation"
git push origin master
cd ..
```

## Verification

After publishing, verify your documentation is live:

- Wiki URL: `https://github.com/hmislk/hmis/wiki/[Page-Name]`
- File name becomes page name (dashes become spaces)
- Example: `Stock-Ledger-Report.md` → https://github.com/hmislk/hmis/wiki/Stock-Ledger-Report

## Best Practices

### File Naming
- Use descriptive kebab-case names: `Stock-Ledger-Report.md`
- Group related features in subdirectories by module
- Avoid spaces and special characters
- File name becomes the wiki page title

### When to Publish
- **Immediately** after creating user documentation
- When adding new user-facing features
- When existing feature workflows change
- When user interface is modified
- When error messages or user workflows are updated

### Content Quality
See [Wiki Writing Guidelines](wiki-writing-guidelines.md) for detailed content guidance.

## Troubleshooting

### Common Issues

#### Wiki Clone Fails
- **Cause**: Authentication or network issue
- **Solution**: Verify GitHub credentials and internet connection

#### Push Rejected
- **Cause**: Wiki repository out of sync
- **Solution**: Pull latest changes first:
  ```bash
  cd hmis.wiki
  git pull origin master
  # Then retry your changes
  ```

#### File Not Appearing
- **Cause**: Incorrect directory or file format
- **Solution**:
  - Verify file is in correct wiki subdirectory
  - Ensure file has `.md` extension
  - Check markdown formatting is valid

#### Permission Denied
- **Cause**: No write access to wiki repository
- **Solution**: Request repository access from maintainers

## Integration with Development

### Workflow Timeline
1. **During feature development**: Create documentation in `wiki-docs/`
2. **Before committing code**: Review and finalize documentation
3. **After committing to feature branch**: Publish to wiki immediately
4. **During PR review**: Documentation already available for testing
5. **After PR merge**: Documentation already published (no additional steps)

### Why Publish Before PR Merge?
- QA testers need documentation during testing
- Stakeholders can review features immediately
- Documentation is available during PR review
- Reduces post-merge tasks

---

## Quick Reference

**Create Documentation**: `wiki-docs/Pharmacy/YourFeature.md`
**Publish to Wiki**: See [Step 3 above](#step-3-publish-to-github-wiki-immediate)
**View Published**: https://github.com/hmislk/hmis/wiki
**Content Guidelines**: [Wiki Writing Guidelines](wiki-writing-guidelines.md)