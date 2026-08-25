# Wiki Publishing Guide

## Overview

This project maintains user documentation in the GitHub Wiki at https://github.com/hmislk/hmis/wiki. The wiki is managed as a **separate git repository** in a sibling directory alongside the main project.

## Critical Rules

**🚨 NO WIKI FILES IN MAIN PROJECT**: Do NOT create wiki markdown files inside the main project repository (e.g., no `wiki-docs/` folder). This causes git submodule issues. All wiki content lives exclusively in the sibling `../hmis.wiki` directory.

**🚨 IMMEDIATE PUBLICATION**: When creating user documentation, ALWAYS publish to GitHub Wiki immediately - don't wait for PR merge.

**📝 GUIDELINES**: Follow the [Wiki Writing Guidelines](wiki-writing-guidelines.md)

## Directory Structure

```
D:\Dev\hmis\
├── hmis\              ← Main project repository (NO wiki files here)
│   ├── src\
│   ├── developer_docs\
│   ├── CLAUDE.md
│   └── ...
└── hmis.wiki\         ← Wiki repository (sibling directory)
    ├── Home.md
    ├── Pharmacy\
    │   ├── Pharmacy-Retail-Sale.md
    │   └── ...
    ├── Developer\
    │   ├── Claude-Code-Skills-Guide.md
    │   └── ...
    ├── Lab\
    ├── OPD\
    └── ...
```

## Setup (One-Time)

Clone the wiki repository as a sibling directory to the main project:

```bash
# From the parent directory (e.g., D:\Dev\hmis\)
cd ..
git clone https://github.com/hmislk/hmis.wiki.git
cd hmis
```

## Publishing Workflow

### Step 1: Ensure Wiki Repo is Up to Date

```bash
cd ../hmis.wiki
git pull origin master
```

### Step 2: Create or Edit Documentation

Create or edit markdown files **directly in the sibling `../hmis.wiki` directory**:

```bash
# Example: Create a new pharmacy feature page
# Edit directly in ../hmis.wiki/Pharmacy/Feature-Name.md
```

Organize files by module:
- `Pharmacy/` - Pharmacy module documentation
- `Lab/` - Laboratory module documentation
- `OPD/` - Outpatient department documentation
- `Developer/` - Developer-facing documentation
- `Administration/` - Admin and configuration guides
- Root level - General pages (Home.md, etc.)

### Step 3: Write Content

Follow the [Wiki Writing Guidelines](wiki-writing-guidelines.md) for audience, structure, and content rules.

### Step 4: Commit and Push

```bash
cd ../hmis.wiki
git add .
git commit -m "Add [Feature Name] user documentation

Co-Authored-By: Claude <noreply@anthropic.com>"
git push origin master
```

## Verification

After publishing, verify your documentation is live:

- Wiki URL: `https://github.com/hmislk/hmis/wiki/[Page-Name]`
- File name becomes page name (dashes become spaces)
- Example: `Stock-Ledger-Report.md` -> https://github.com/hmislk/hmis/wiki/Stock-Ledger-Report

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

## Troubleshooting

### Common Issues

#### Wiki Repo Not Found
- **Cause**: Sibling directory not cloned yet
- **Solution**: Clone from the parent directory:
  ```bash
  cd ..
  git clone https://github.com/hmislk/hmis.wiki.git
  cd hmis
  ```

#### Push Rejected
- **Cause**: Wiki repository out of sync
- **Solution**: Pull latest changes first:
  ```bash
  cd ../hmis.wiki
  git pull origin master
  # Then retry your changes
  ```

#### File Not Appearing on Wiki
- **Cause**: Incorrect directory or file format
- **Solution**:
  - Verify file is in the `../hmis.wiki` directory (not in the main project)
  - Ensure file has `.md` extension
  - Check markdown formatting is valid

#### Permission Denied
- **Cause**: No write access to wiki repository
- **Solution**: Request repository access from maintainers

## Integration with Development

### Workflow Timeline
1. **During feature development**: Create documentation directly in `../hmis.wiki/`
2. **Before committing code**: Review and finalize documentation
3. **After creating the wiki page**: Commit and push to wiki immediately
4. **During PR review**: Documentation already available for testing
5. **After PR merge**: Documentation already published (no additional steps)

### Why Publish Before PR Merge?
- QA testers need documentation during testing
- Stakeholders can review features immediately
- Documentation is available during PR review
- Reduces post-merge tasks

---

## Quick Reference

**Wiki Repo Location**: `../hmis.wiki` (sibling directory)
**Create Documentation**: Edit files directly in `../hmis.wiki/Module/Feature-Name.md`
**Publish**: `cd ../hmis.wiki && git add . && git commit -m "message" && git push origin master`
**View Published**: https://github.com/hmislk/hmis/wiki
**Content Guidelines**: [Wiki Writing Guidelines](wiki-writing-guidelines.md)
